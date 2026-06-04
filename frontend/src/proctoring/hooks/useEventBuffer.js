import { useCallback, useEffect, useRef, useState } from 'react';
import * as eventBuffer from '../storage/eventBuffer';

// ─── useEventBuffer ──────────────────────────────────────────────────────────
//
// React hook over `proctoring/storage/eventBuffer.js`. Exposes the offline
// queue used by `useProctoringSocket` so that events emitted while the WS is
// closed are persisted in IndexedDB and replayed on reconnect.
//
// Public contract
//   enqueue(eventFrame) → Promise<void>
//     Persist a single frame. Old records (>60 s by `queued_at_ms`) are
//     pruned on every insert per Req 11.5 (`maxOfflineSeconds=60`), which the
//     design treats as the cap-by-age policy: drop the oldest first.
//   drain(sendFn) → Promise<number>
//     Read every queued record (sorted by `queued_at_ms` ascending) and
//     `await sendFn(record)` for each. Stops on the first thrown error so a
//     transient send failure does not lose the rest of the queue. Returns
//     the count of records successfully forwarded. Re-entrancy is suppressed
//     via an internal `drainingRef` — calling `drain` while a drain is
//     in-flight resolves to `0` (the in-flight call owns the queue).
//   clear() → Promise<void>
//     Truncate the store. Called by `useProctoringSocket` on `BUFFER_ACK`
//     once the server has acknowledged the replayed batch.
//   size: number
//     Reactive row count. Updated after every enqueue/drain/clear so the UI
//     can render an "events queued" indicator.
//
// Validates: Requirements 11.1, 11.2, 11.5.

const MAX_AGE_MS = 60_000;

export default function useEventBuffer() {
  const [size, setSize] = useState(0);
  const drainingRef = useRef(false);
  const mountedRef = useRef(true);

  /** Re-read the count from IDB and push it into reactive state. */
  const refreshSize = useCallback(async () => {
    try {
      const c = await eventBuffer.count();
      if (mountedRef.current) setSize(c);
    } catch {
      // IDB errors are non-fatal here — the count just stays stale until the
      // next successful op.
    }
  }, []);

  // Initial size + cleanup latch.
  useEffect(() => {
    mountedRef.current = true;
    refreshSize();
    return () => {
      mountedRef.current = false;
    };
  }, [refreshSize]);

  const enqueue = useCallback(async (frame) => {
    // Prune by age first so an `enqueue` after a long offline gap rolls the
    // window forward instead of letting >60 s old records pile up.
    try {
      await eventBuffer.pruneOlderThan(MAX_AGE_MS);
    } catch {
      // Pruning is best-effort; keep going so the new event still lands.
    }
    await eventBuffer.put(frame);
    refreshSize();
  }, [refreshSize]);

  const drain = useCallback(async (sendFn) => {
    if (typeof sendFn !== 'function') return 0;
    if (drainingRef.current) return 0;
    drainingRef.current = true;
    let sent = 0;
    try {
      const records = await eventBuffer.getAll();
      for (const record of records) {
        try {
          await sendFn(record);
          sent += 1;
        } catch {
          // First send failure aborts the drain — leave the remainder for
          // the next reconnect so we don't silently drop events. The
          // failed-record itself stays in IDB because we never delete on a
          // per-record basis here; the server's BUFFER_ACK is the cue to
          // clear the whole batch via `clear()`.
          break;
        }
      }
    } finally {
      drainingRef.current = false;
      refreshSize();
    }
    return sent;
  }, [refreshSize]);

  const clear = useCallback(async () => {
    try {
      await eventBuffer.clear();
    } finally {
      refreshSize();
    }
  }, [refreshSize]);

  return { enqueue, drain, clear, size };
}
