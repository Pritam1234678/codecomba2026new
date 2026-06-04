// ─── Proctoring offline event buffer (IndexedDB low-level wrapper) ────────────
//
// Persists Suspicious_Event frames while the proctoring WebSocket is closed so
// they can be replayed when the link comes back. Backed by IndexedDB so the
// buffer survives page reloads inside the same browser profile (Req 11.1, 11.2).
//
// Schema (per design.md "Offline buffer flow")
//   DB name      : "proctoring_buffer"
//   DB version   : 1
//   Object store : "events"
//     keyPath  : "client_correlation_id" (string)
//   Record shape :
//     {
//       client_correlation_id : string,   // primary key, stable across replay
//       session_id            : number,   // proctoring_sessions.id
//       event_type            : string,   // e.g. "TAB_SWITCH"
//       client_timestamp      : string,   // ISO-8601 from when the event fired
//       payload               : object,   // detector-specific extra data
//       queued_at_ms          : number,   // Date.now() at insert; used for prune
//     }
//
// All public functions are async and return Promises. The IDBDatabase handle
// is opened lazily and cached at module scope so repeated calls reuse the same
// connection (`useEventBuffer` calls these on every render path).
//
// This wrapper is intentionally low-level — it does not coordinate with the
// WebSocket. The React-facing `useEventBuffer` hook composes these primitives
// into the enqueue / drain / clear contract consumed by `useProctoringSocket`.

const DB_NAME = 'proctoring_buffer';
const DB_VERSION = 1;
const STORE = 'events';

let dbPromise = null;

/**
 * Open (or reuse) the IndexedDB connection. Lazy + cached.
 * Rejects if the runtime has no `indexedDB` (SSR, very old browser).
 *
 * @returns {Promise<IDBDatabase>}
 */
function openDb() {
  if (dbPromise) return dbPromise;
  if (typeof indexedDB === 'undefined') {
    return Promise.reject(new Error('IndexedDB is not available in this environment'));
  }

  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);

    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'client_correlation_id' });
      }
    };

    req.onsuccess = () => {
      const db = req.result;
      // Drop the cached promise if the connection is closed/version-changed
      // from another tab so the next call reopens a fresh handle.
      db.onclose = () => {
        if (dbPromise && dbPromise._db === db) dbPromise = null;
      };
      db.onversionchange = () => {
        try { db.close(); } catch (_) { /* ignored */ }
        if (dbPromise && dbPromise._db === db) dbPromise = null;
      };
      resolve(db);
    };

    req.onerror = () => reject(req.error || new Error('IndexedDB open failed'));
    req.onblocked = () => reject(new Error('IndexedDB open blocked'));
  });

  dbPromise.then((db) => { dbPromise._db = db; }).catch(() => { dbPromise = null; });
  return dbPromise;
}

/** Wrap a single IDBRequest in a Promise. */
function reqAsPromise(req) {
  return new Promise((resolve, reject) => {
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

/** Wrap an IDBTransaction completion in a Promise. */
function txDone(tx) {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error || new Error('IndexedDB transaction aborted'));
  });
}

/**
 * Insert (or overwrite) one buffered event. Idempotent on
 * `client_correlation_id`: an enqueue with the same id replaces the existing
 * record so a burst-with-retry path cannot duplicate rows.
 *
 * @param {{
 *   client_correlation_id: string,
 *   session_id: number|null,
 *   event_type: string,
 *   client_timestamp: string|number,
 *   payload?: object,
 *   queued_at_ms: number,
 * }} record
 * @returns {Promise<void>}
 */
export async function put(record) {
  if (!record || typeof record !== 'object') {
    throw new TypeError('put(record): record must be an object');
  }
  if (typeof record.client_correlation_id !== 'string' || record.client_correlation_id.length === 0) {
    throw new TypeError('put(record): client_correlation_id is required');
  }
  const db = await openDb();
  const tx = db.transaction(STORE, 'readwrite');
  tx.objectStore(STORE).put(record);
  await txDone(tx);
}

/**
 * Read every buffered record, sorted by `queued_at_ms` ascending so replay
 * preserves the original arrival order even after IDB is reopened (the IDB
 * spec guarantees insertion order on cursor walks but not on `getAll`).
 *
 * @returns {Promise<Array<object>>}
 */
export async function getAll() {
  const db = await openDb();
  const tx = db.transaction(STORE, 'readonly');
  const rows = await reqAsPromise(tx.objectStore(STORE).getAll());
  await txDone(tx);
  return (rows || []).slice().sort((a, b) => (a?.queued_at_ms ?? 0) - (b?.queued_at_ms ?? 0));
}

/** Truncate the store. Used on `BUFFER_ACK` once the server has accepted a replayed batch. */
export async function clear() {
  const db = await openDb();
  const tx = db.transaction(STORE, 'readwrite');
  tx.objectStore(STORE).clear();
  await txDone(tx);
}

/** Current row count. */
export async function count() {
  const db = await openDb();
  const tx = db.transaction(STORE, 'readonly');
  const c = await reqAsPromise(tx.objectStore(STORE).count());
  await txDone(tx);
  return c;
}

/** Delete one record by `client_correlation_id`. Missing keys are silently ignored. */
export async function deleteOne(cid) {
  if (typeof cid !== 'string' || cid.length === 0) return;
  const db = await openDb();
  const tx = db.transaction(STORE, 'readwrite');
  tx.objectStore(STORE).delete(cid);
  await txDone(tx);
}

/**
 * Drop every record older than `maxAgeMs` (by `queued_at_ms`). The buffer cap
 * is time-based per Req 11.5 (`maxOfflineSeconds`, default 60 s) — anything
 * older is no longer worth replaying and would just inflate the dedup window.
 *
 * Uses a cursor walk because we don't have an index on `queued_at_ms`; the
 * buffer is small (≤ 60 s × event rate, capped well under 1 k) so the scan is
 * cheap.
 *
 * @param {number} maxAgeMs
 * @returns {Promise<number>} number of rows deleted
 */
export async function pruneOlderThan(maxAgeMs) {
  const safeMaxAge = Math.max(0, Number.isFinite(maxAgeMs) ? maxAgeMs : 0);
  const cutoff = Date.now() - safeMaxAge;
  const db = await openDb();
  const tx = db.transaction(STORE, 'readwrite');
  const store = tx.objectStore(STORE);

  let deleted = 0;
  await new Promise((resolve, reject) => {
    const req = store.openCursor();
    req.onsuccess = () => {
      const cursor = req.result;
      if (!cursor) {
        resolve();
        return;
      }
      const v = cursor.value;
      if (typeof v?.queued_at_ms === 'number' && v.queued_at_ms < cutoff) {
        cursor.delete();
        deleted += 1;
      }
      cursor.continue();
    };
    req.onerror = () => reject(req.error);
  });

  await txDone(tx);
  return deleted;
}

/**
 * Test/diagnostic helper: drop the cached connection so the next public call
 * reopens it. Not part of the documented API surface.
 */
export function _resetForTesting() {
  if (dbPromise && dbPromise._db) {
    try { dbPromise._db.close(); } catch (_) { /* ignored */ }
  }
  dbPromise = null;
}
