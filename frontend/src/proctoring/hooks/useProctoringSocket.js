import { useCallback, useEffect, useRef, useState } from 'react';
import proctoringApi from '../services/proctoringApi';
import useEventBuffer from './useEventBuffer';

// ─── useProctoringSocket (initial) ──────────────────────────────────────────
//
// Opens a single proctoring WebSocket to `/api/proctoring/ws?ticket=…`,
// auto-sends `HEARTBEAT` frames, surfaces inbound server frames via the
// caller's callbacks, and exposes typed `sendEvent` / `sendHeartbeat` /
// `sendFinish` / `sendQuit` helpers. Reconnects with exponential backoff
// (1s, 2s, 4s — capped at 10s) on non-terminal closes, minting a fresh
// single-use ticket per attempt via `proctoringApi.mintWsTicket(sessionId)`.
//
// This is the **initial** version called out by tasks.md task 4.5. The
// IndexedDB offline-buffer + replay-with-BUFFER_ACK path is layered on
// in task 9 (`useEventBuffer`); for now `sendEvent` simply rejects when
// the socket is not open. The reconnect/backoff scaffolding here is
// designed to be extended without breaking the public surface.
//
// Public contract
// ───────────────
// Inputs (all optional except `sessionId` + `wsTicket`):
//   sessionId               — proctoring_sessions.id (Long).
//   wsTicket                — initial single-use ticket from
//                             POST /api/proctoring/contests/{cid}/sessions.
//                             Reconnects mint a fresh ticket via
//                             proctoringApi.mintWsTicket(sessionId).
//   onTerminated(endReason) — called once when the session reaches a
//                             terminal state (server pushed
//                             `SESSION_TERMINATED`, server closed with
//                             4401/4403/4408/4409, or all reconnect
//                             attempts failed). `endReason` is the
//                             server-supplied `end_reason` when known,
//                             otherwise a synthetic string like
//                             `'AUTH_FAILED'` / `'HEARTBEAT_TIMEOUT'` /
//                             `'DUPLICATE_CONNECTION'` /
//                             `'RECONNECT_FAILED'`.
//   onRiskUpdate({ score, band })
//                           — every server-pushed RISK_UPDATE.
//   onWarning({ message, … })
//                           — every server-pushed WARNING.
//   onConnectivityChange(connected)
//                           — boolean that flips whenever the derived
//                             "connected" signal changes. The signal is
//                             `wsOpen && (Date.now() - lastEventAt) < 90s`
//                             (Req 24 connection guard).
//   heartbeatIntervalSeconds — default 20, mirrors design.md §"Reconnection".
//
// Returns
//   status                  — 'connecting' | 'open' | 'reconnecting' |
//                             'closed' | 'terminated'.
//   sendEvent(eventType, payload, opts?) → Promise<event_id>
//                             Wraps the frame as
//                             `{ type:'EVENT', event_type, client_timestamp,
//                                client_correlation_id, payload, replayed }`
//                             and resolves with the `event_id` returned by
//                             the matching `EVENT_ACK`. Rejects after a
//                             10 s timeout (`EVENT_ACK_TIMEOUT`) or if the
//                             socket is not open (`SOCKET_NOT_OPEN`).
//   sendHeartbeat()         — manual heartbeat (auto-fire is wired below).
//   sendFinish()            — sends `{ type:'FINISH' }`. The terminal
//                             transition arrives via `SESSION_TERMINATED`.
//   sendQuit()              — sends `{ type:'QUIT' }`. Same shape.
//   lastEventAt             — wall-clock ms of the last server frame.
//   score                   — last `risk_score` pushed via RISK_UPDATE.
//   band                    — last `risk_band` pushed via RISK_UPDATE.

// Server-side close codes that signal a terminal state. The candidate
// must NOT reconnect on these — see design.md "Close Codes".
// NOTE: 4409 (duplicate connection) is intentionally NOT terminal — a
// React StrictMode dev double-mount, or a fast reconnect that races the
// server's unregister of the prior socket, can produce a transient
// duplicate. We retry those with backoff (capped) so the connection
// self-heals once the old binding is released, rather than going dead.
const TERMINAL_CLOSE_CODES = new Set([4401, 4403, 4408]);

// Duplicate-connection close — retryable, not terminal. Bounded by the
// normal backoff cap so a genuinely stuck duplicate still surfaces as
// RECONNECT_FAILED instead of looping forever.
const DUPLICATE_CLOSE_CODE = 4409;

// Map a terminal close code to the `endReason` we surface through
// `onTerminated` when the server did not push a `SESSION_TERMINATED`
// frame first (e.g. 4401 at handshake).
const TERMINAL_CLOSE_REASONS = Object.freeze({
  4401: 'AUTH_FAILED',
  4403: 'FORBIDDEN',
  4408: 'HEARTBEAT_TIMEOUT',
  4409: 'DUPLICATE_CONNECTION',
});

// Exponential backoff schedule for non-terminal closes. Values are in
// milliseconds. Capped at the last value once `attempt >= length-1`.
// The full design schedule (`[1s,2s,4s,8s,16s,30s,…]`) lands in task 9;
// the "initial" version here uses 1s, 2s, 4s with a 10 s ceiling per
// the task 4.5 prompt.
const BACKOFF_DELAYS_MS = [1000, 2000, 4000, 10000];

// Window after which a still-open socket is considered "stale" — the
// server should be sending at least a HEARTBEAT_ACK every
// heartbeatIntervalSeconds, so 90 s with no inbound frame means the
// link is wedged even if the TCP/WS layer is still alive (Req 24).
const CONNECTIVITY_STALE_MS = 90_000;

// Resolved correlation timeout per task 4.5 prompt.
const EVENT_ACK_TIMEOUT_MS = 10_000;

// Default heartbeat interval, in seconds. Falls back to 20 s when the
// caller does not pass one — matches the design's heartbeat cadence
// for proctoring (Req 9.7).
const DEFAULT_HEARTBEAT_INTERVAL_S = 20;

/**
 * Build the wss:// URL for the proctoring channel. Mirrors
 * `useDuelStream.buildStreamUrl` and `CoderCompiler` so the same env
 * resolution covers dev (Vite proxy) and prod.
 *
 * Rules:
 *   • Use `import.meta.env.VITE_API_URL` if present (e.g.
 *     `https://api.codecoder.in/api`) and swap http→ws / https→wss.
 *   • On localhost without VITE_API_URL set, fall back to
 *     `ws://localhost:8080/api`.
 *   • Otherwise relative `/api` becomes `wss://{host}/api`.
 *
 * Both `ticket` AND `sessionId` are sent as query params. The handshake
 * interceptor (ProctoringHandshakeInterceptor) consumes the single-use
 * ticket to resolve the userId, then REQUIRES the sessionId param to
 * load + ownership-check the session row — a missing sessionId is a 401
 * at the upgrade.
 */
function buildSocketUrl(ticket, sessionId) {
  const apiBase = import.meta.env.VITE_API_URL ?? '/api';
  const proto = (typeof window !== 'undefined' && window.location?.protocol === 'https:') ? 'wss:' : 'ws:';

  let wsBase;
  if (apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    wsBase = apiBase.replace(/^https?:/, proto);
  } else if (typeof window !== 'undefined' && window.location?.hostname === 'localhost') {
    wsBase = `${proto}//localhost:8080${apiBase.startsWith('/') ? apiBase : `/${apiBase}`}`;
  } else if (typeof window !== 'undefined') {
    wsBase = `${proto}//${window.location.host}${apiBase.startsWith('/') ? apiBase : `/${apiBase}`}`;
  } else {
    wsBase = `${proto}//localhost:8080/api`;
  }

  return `${wsBase}/proctoring/ws?ticket=${encodeURIComponent(ticket)}&sessionId=${encodeURIComponent(sessionId ?? '')}`;
}

/**
 * Mint a UUIDv4 for `client_correlation_id`. Falls back to a
 * Math.random-based string if `crypto.randomUUID` is unavailable
 * (older browsers, JSDOM in tests). The id only needs to be unique
 * within the lifetime of one WS connection, so collision risk is
 * acceptable.
 */
function newCorrelationId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // RFC4122-ish fallback. Sufficient for correlation, not for security.
  return 'cid-' + Math.random().toString(36).slice(2) + '-' + Date.now().toString(36);
}

export default function useProctoringSocket({
  sessionId,
  wsTicket,
  onTerminated,
  onRiskUpdate,
  onWarning,
  onConnectivityChange,
  heartbeatIntervalSeconds = DEFAULT_HEARTBEAT_INTERVAL_S,
} = {}) {
  // ── Public state ─────────────────────────────────────────────────
  const [status, setStatus] = useState('connecting');
  const [lastEventAt, setLastEventAt] = useState(0);
  const [score, setScore] = useState(0);
  const [band, setBand] = useState('LOW');

  // ── Offline buffer (IndexedDB-backed) ────────────────────────────
  // While the WS is closed, `sendEvent` writes to this buffer instead
  // of rejecting. On the next successful open we drain it as replayed
  // frames; on `BUFFER_ACK` from the server we clear it. The hook is
  // safe to call unconditionally — its effects run inside the same
  // React tree as ours.
  const buffer = useEventBuffer();
  const bufferRef = useRef(buffer);
  useEffect(() => { bufferRef.current = buffer; }, [buffer]);

  // Re-entrancy guard for `drain` — a flapping connection can fire
  // `onopen` again while a previous drain is still in flight.
  const drainingRef = useRef(false);

  // ── Refs (callbacks + connection bookkeeping) ────────────────────
  // Callbacks are read through refs so the caller does not have to
  // memoise them — same pattern as `useTabFocusMonitor`.
  const onTerminatedRef = useRef(onTerminated);
  const onRiskUpdateRef = useRef(onRiskUpdate);
  const onWarningRef = useRef(onWarning);
  const onConnectivityChangeRef = useRef(onConnectivityChange);

  useEffect(() => { onTerminatedRef.current = onTerminated; }, [onTerminated]);
  useEffect(() => { onRiskUpdateRef.current = onRiskUpdate; }, [onRiskUpdate]);
  useEffect(() => { onWarningRef.current = onWarning; }, [onWarning]);
  useEffect(() => { onConnectivityChangeRef.current = onConnectivityChange; }, [onConnectivityChange]);

  // Live connection bookkeeping. None of these belong in React state —
  // they change on every frame and would thrash the render tree.
  const wsRef = useRef(null);
  const attemptRef = useRef(0);                 // reconnect attempt counter
  const reconnectTimerRef = useRef(null);
  const heartbeatTimerRef = useRef(null);
  const connectivityTimerRef = useRef(null);    // periodic connected check
  const lastConnectivityRef = useRef(false);
  const lastEventAtRef = useRef(0);
  const wsOpenRef = useRef(false);
  const terminatedRef = useRef(false);          // latched once terminal
  const unmountedRef = useRef(false);
  const initialTicketRef = useRef(wsTicket);
  const sessionIdRef = useRef(sessionId);
  const heartbeatIntervalRef = useRef(heartbeatIntervalSeconds);

  // Pending EVENT_ACK promises keyed by client_correlation_id.
  // Each entry is `{ resolve, reject, timer }`.
  const pendingAcksRef = useRef(new Map());

  useEffect(() => { initialTicketRef.current = wsTicket; }, [wsTicket]);
  useEffect(() => { sessionIdRef.current = sessionId; }, [sessionId]);
  useEffect(() => { heartbeatIntervalRef.current = heartbeatIntervalSeconds; }, [heartbeatIntervalSeconds]);

  // ── Helpers ──────────────────────────────────────────────────────

  /**
   * Compute the current "connected" signal and notify the caller iff
   * it changed. The signal is `wsOpen && (now - lastEventAt) < 90s`.
   * Polled on a 5 s tick so the candidate sees the warning modal
   * within ~5 s of the link going stale even if no further frames
   * arrive (Req 11.5, Req 24 connection guard).
   */
  const evaluateConnectivity = useCallback(() => {
    const stale = lastEventAtRef.current === 0
      ? !wsOpenRef.current
      : (Date.now() - lastEventAtRef.current) >= CONNECTIVITY_STALE_MS;
    const connected = wsOpenRef.current && !stale;
    if (connected !== lastConnectivityRef.current) {
      lastConnectivityRef.current = connected;
      const cb = onConnectivityChangeRef.current;
      if (typeof cb === 'function') {
        try { cb(connected); } catch { /* never crash on consumer error */ }
      }
    }
  }, []);

  /** Clear every interval/timeout owned by this hook instance. */
  const clearTimers = useCallback(() => {
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current);
      heartbeatTimerRef.current = null;
    }
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (connectivityTimerRef.current) {
      clearInterval(connectivityTimerRef.current);
      connectivityTimerRef.current = null;
    }
  }, []);

  /** Reject + drain every still-pending EVENT_ACK promise. */
  const drainPendingAcks = useCallback((reason) => {
    const map = pendingAcksRef.current;
    pendingAcksRef.current = new Map();
    map.forEach(({ reject, timer }) => {
      if (timer) clearTimeout(timer);
      try { reject(new Error(reason)); } catch { /* ignored */ }
    });
  }, []);

  /**
   * Latch a terminal state and fire `onTerminated` exactly once.
   * After this the hook stops reconnecting and `sendEvent` will
   * reject — `status` becomes `'terminated'`.
   */
  const finalizeTerminated = useCallback((endReason) => {
    if (terminatedRef.current) return;
    terminatedRef.current = true;
    clearTimers();
    drainPendingAcks('SESSION_TERMINATED');
    setStatus('terminated');
    wsOpenRef.current = false;
    evaluateConnectivity();
    const cb = onTerminatedRef.current;
    if (typeof cb === 'function') {
      try { cb(endReason); } catch { /* ignored */ }
    }
  }, [clearTimers, drainPendingAcks, evaluateConnectivity]);

  /** Send a JSON object on the live socket. No-op when not open. */
  const rawSend = useCallback((obj) => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) return false;
    try {
      ws.send(JSON.stringify(obj));
      return true;
    } catch (err) {
      // The browser throws when send() is called on a closing socket.
      // Treat as a transient failure — the close handler will pick up.
      // eslint-disable-next-line no-console
      console.warn('useProctoringSocket: send failed', err);
      return false;
    }
  }, []);

  /**
   * Dispatch one inbound server frame. Pure with respect to refs;
   * does not start/stop the socket.
   */
  const handleServerFrame = useCallback((frame) => {
    if (!frame || typeof frame !== 'object') return;
    lastEventAtRef.current = Date.now();
    setLastEventAt(lastEventAtRef.current);
    evaluateConnectivity();

    switch (frame.type) {
      case 'EVENT_ACK': {
        const cid = frame.client_correlation_id;
        const eventId = frame.event_id;
        const entry = pendingAcksRef.current.get(cid);
        if (entry) {
          pendingAcksRef.current.delete(cid);
          if (entry.timer) clearTimeout(entry.timer);
          try { entry.resolve(eventId); } catch { /* ignored */ }
        }
        return;
      }
      case 'HEARTBEAT_ACK':
        // Server liveness — already accounted for via lastEventAt.
        return;
      case 'RISK_UPDATE': {
        const nextScore = typeof frame.risk_score === 'number' ? frame.risk_score : 0;
        const nextBand = typeof frame.risk_band === 'string' ? frame.risk_band : 'LOW';
        setScore(nextScore);
        setBand(nextBand);
        const cb = onRiskUpdateRef.current;
        if (typeof cb === 'function') {
          try { cb({ score: nextScore, band: nextBand }); } catch { /* ignored */ }
        }
        return;
      }
      case 'WARNING': {
        const cb = onWarningRef.current;
        if (typeof cb === 'function') {
          try { cb({ message: frame.message, admin_id: frame.admin_id, acted_at: frame.acted_at }); } catch { /* ignored */ }
        }
        return;
      }
      case 'SESSION_TERMINATED': {
        finalizeTerminated(frame.end_reason ?? frame.reason ?? 'TERMINATED');
        // The server closes the WS itself right after this frame; we
        // just stop reconnect attempts via the latch above.
        return;
      }
      case 'BUFFER_ACK':
        // Server has accepted the entire replayed batch. Purge the
        // local IndexedDB queue so the same events don't replay on the
        // next reconnect (Req 11.2).
        try {
          bufferRef.current?.clear?.();
        } catch { /* ignored */ }
        return;
      case 'RATE_LIMIT_EXCEEDED':
        // Informational only at this layer. The arena UI may surface a
        // toast based on this in a later task.
        return;
      default:
        // Forward-compat: unknown frame types are silently ignored so
        // a future detector plugin can ride the same channel without
        // breaking older candidates (Req 22.x).
        return;
    }
  }, [evaluateConnectivity, finalizeTerminated]);

  // Forward declaration — `connect` calls itself indirectly through
  // `scheduleReconnect`, so we keep the function in a ref.
  const connectRef = useRef(null);

  /**
   * Schedule the next reconnect attempt using exponential backoff.
   * Capped at the last value of BACKOFF_DELAYS_MS (10 s by default).
   * Mints a fresh single-use ws ticket per attempt because the
   * server consumes the ticket atomically on handshake.
   */
  const scheduleReconnect = useCallback(() => {
    if (terminatedRef.current || unmountedRef.current) return;
    const idx = Math.min(attemptRef.current, BACKOFF_DELAYS_MS.length - 1);
    const delay = BACKOFF_DELAYS_MS[idx];
    attemptRef.current += 1;
    setStatus('reconnecting');

    if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
    reconnectTimerRef.current = setTimeout(async () => {
      reconnectTimerRef.current = null;
      if (terminatedRef.current || unmountedRef.current) return;

      // Mint a fresh ticket. If `mintWsTicket` is unavailable, fail
      // closed — without a ticket the handshake would just bounce
      // with 4401 and we'd loop forever.
      let ticket;
      try {
        if (typeof proctoringApi.mintWsTicket !== 'function') {
          throw new Error('NO_MINT_TICKET');
        }
        const res = await proctoringApi.mintWsTicket(sessionIdRef.current);
        ticket = res?.data?.wsTicket ?? res?.data?.ticket ?? null;
        if (!ticket) throw new Error('TICKET_EMPTY');
      } catch (err) {
        // Mint failed. Treat repeated failures as terminal once we hit
        // the cap, otherwise back off again.
        // eslint-disable-next-line no-console
        console.warn('useProctoringSocket: ticket mint failed', err);
        if (attemptRef.current >= BACKOFF_DELAYS_MS.length) {
          finalizeTerminated('RECONNECT_FAILED');
          return;
        }
        scheduleReconnect();
        return;
      }

      // Open the next socket with the freshly minted ticket.
      if (typeof connectRef.current === 'function') {
        connectRef.current(ticket);
      }
    }, delay);
  }, [finalizeTerminated]);

  /**
   * Start the per-session heartbeat ticker. Cleared on close/teardown.
   */
  const startHeartbeat = useCallback(() => {
    if (heartbeatTimerRef.current) {
      clearInterval(heartbeatTimerRef.current);
      heartbeatTimerRef.current = null;
    }
    const intervalMs = Math.max(1, heartbeatIntervalRef.current) * 1000;
    heartbeatTimerRef.current = setInterval(() => {
      rawSend({ type: 'HEARTBEAT' });
    }, intervalMs);
  }, [rawSend]);

  /**
   * Open one WebSocket with `ticket`. Wires up open/message/close/
   * error handlers and bumps `status`.
   */
  const connect = useCallback((ticket) => {
    if (terminatedRef.current || unmountedRef.current) return;
    if (!ticket) {
      finalizeTerminated('NO_TICKET');
      return;
    }
    setStatus(attemptRef.current === 0 ? 'connecting' : 'reconnecting');

    let ws;
    try {
      ws = new WebSocket(buildSocketUrl(ticket, sessionIdRef.current));
    } catch (err) {
      // eslint-disable-next-line no-console
      console.warn('useProctoringSocket: WebSocket constructor threw', err);
      scheduleReconnect();
      return;
    }
    wsRef.current = ws;

    ws.onopen = () => {
      if (unmountedRef.current || terminatedRef.current) {
        try { ws.close(1000, 'unmounted'); } catch { /* ignored */ }
        return;
      }
      wsOpenRef.current = true;
      attemptRef.current = 0;          // reset backoff after a clean open
      setStatus('open');
      lastEventAtRef.current = Date.now();
      setLastEventAt(lastEventAtRef.current);
      evaluateConnectivity();
      startHeartbeat();

      // Drain any buffered offline events as `replayed: true` frames.
      // The server emits `BUFFER_ACK` after the contiguous batch is
      // ingested, and our message handler clears the IDB store. We
      // intentionally do not await this — it runs alongside live
      // emissions, which is fine because the dedup key on the server
      // (`proctoring:dedup:{sid}:{ts_ms}:{type}`) keeps scoring correct.
      if (!drainingRef.current) {
        drainingRef.current = true;
        const drainSender = async (record) => {
          // The active socket may have already closed by the time we
          // reach a later record — bail out so the remainder stays
          // queued for the next reconnect.
          const live = wsRef.current;
          if (!live || live.readyState !== WebSocket.OPEN) {
            throw new Error('SOCKET_NOT_OPEN_DURING_DRAIN');
          }
          live.send(JSON.stringify({
            type: 'EVENT',
            event_type: record.event_type,
            client_timestamp: record.client_timestamp,
            client_correlation_id: record.client_correlation_id,
            payload: record.payload ?? {},
            replayed: true,
          }));
        };
        Promise.resolve()
          .then(() => bufferRef.current?.drain?.(drainSender))
          .catch((err) => {
            // eslint-disable-next-line no-console
            console.warn('useProctoringSocket: buffer drain failed', err);
          })
          .finally(() => {
            drainingRef.current = false;
          });
      }
    };

    ws.onmessage = (ev) => {
      let frame;
      try {
        frame = JSON.parse(ev.data);
      } catch (err) {
        // eslint-disable-next-line no-console
        console.warn('useProctoringSocket: malformed server frame', err);
        return;
      }
      handleServerFrame(frame);
    };

    ws.onerror = () => {
      // The browser does not surface details for WS errors. The
      // companion `onclose` handler will fire next and own the
      // reconnect/terminate decision.
    };

    ws.onclose = (ev) => {
      wsOpenRef.current = false;
      if (heartbeatTimerRef.current) {
        clearInterval(heartbeatTimerRef.current);
        heartbeatTimerRef.current = null;
      }
      // Drop the live ref so any stray send() goes nowhere.
      if (wsRef.current === ws) wsRef.current = null;
      evaluateConnectivity();

      if (terminatedRef.current || unmountedRef.current) {
        setStatus('closed');
        return;
      }
      // Terminal close codes from the server — never reconnect.
      if (TERMINAL_CLOSE_CODES.has(ev.code)) {
        finalizeTerminated(TERMINAL_CLOSE_REASONS[ev.code] ?? 'TERMINATED');
        return;
      }
      // Duplicate connection (4409) — a transient StrictMode remount or
      // fast-reconnect race. Retry with backoff; the prior socket's
      // unregister releases the binding so the next attempt binds clean.
      if (ev.code === DUPLICATE_CLOSE_CODE) {
        scheduleReconnect();
        return;
      }
      // Normal closure (1000) without a prior SESSION_TERMINATED frame
      // happens when the server completes a FINISH/QUIT — finalize as
      // closed but not terminated, so the caller can navigate away.
      if (ev.code === 1000) {
        setStatus('closed');
        return;
      }
      // Anything else is a transient drop. Reconnect with backoff.
      scheduleReconnect();
    };
  }, [evaluateConnectivity, finalizeTerminated, handleServerFrame, scheduleReconnect, startHeartbeat]);

  // Keep the latest `connect` callable for `scheduleReconnect`.
  useEffect(() => {
    connectRef.current = connect;
  }, [connect]);

  // ── Public API ───────────────────────────────────────────────────

  const sendHeartbeat = useCallback(() => {
    rawSend({ type: 'HEARTBEAT' });
  }, [rawSend]);

  const sendFinish = useCallback(() => {
    rawSend({ type: 'FINISH' });
  }, [rawSend]);

  const sendQuit = useCallback(() => {
    rawSend({ type: 'QUIT' });
  }, [rawSend]);

  const sendEvent = useCallback((eventType, payload = {}, opts = {}) => {
    if (terminatedRef.current) {
      return Promise.reject(new Error('SESSION_TERMINATED'));
    }
    const cid = newCorrelationId();
    const clientTimestamp = new Date().toISOString();
    const ws = wsRef.current;
    const wsOpen = ws && ws.readyState === WebSocket.OPEN;

    if (!wsOpen) {
      // Offline path: persist to IndexedDB so the next reconnect can
      // replay it as a `replayed: true` frame. Resolve with the
      // synthetic correlation id so the caller's await flow does not
      // diverge between online and offline modes — the real `event_id`
      // arrives later via `EVENT_ACK` once the server accepts the
      // replayed frame and is then forwarded to a no-op pending entry
      // (drain creates one). For the offline-emit caller the cid is
      // sufficient as a handle.
      const record = {
        client_correlation_id: cid,
        session_id: sessionIdRef.current ?? null,
        event_type: eventType,
        client_timestamp: clientTimestamp,
        payload,
        queued_at_ms: Date.now(),
      };
      return Promise.resolve()
        .then(() => bufferRef.current?.enqueue?.(record))
        .then(() => cid)
        .catch((err) => {
          // If IDB write fails we still return the cid so the caller
          // can continue — the alternative (rejecting) would mean
          // dropping the event entirely on a transient storage error.
          // eslint-disable-next-line no-console
          console.warn('useProctoringSocket: offline enqueue failed', err);
          return cid;
        });
    }

    return new Promise((resolve, reject) => {
      const frame = {
        type: 'EVENT',
        event_type: eventType,
        client_timestamp: clientTimestamp,
        client_correlation_id: cid,
        payload,
        replayed: opts.replayed === true,
      };

      const timer = setTimeout(() => {
        if (pendingAcksRef.current.has(cid)) {
          pendingAcksRef.current.delete(cid);
          reject(new Error('EVENT_ACK_TIMEOUT'));
        }
      }, EVENT_ACK_TIMEOUT_MS);

      pendingAcksRef.current.set(cid, { resolve, reject, timer });

      const ok = rawSend(frame);
      if (!ok) {
        pendingAcksRef.current.delete(cid);
        clearTimeout(timer);
        reject(new Error('SOCKET_NOT_OPEN'));
      }
    });
  }, [rawSend]);

  // ── Mount / unmount ──────────────────────────────────────────────
  useEffect(() => {
    unmountedRef.current = false;
    terminatedRef.current = false;
    attemptRef.current = 0;
    lastEventAtRef.current = 0;
    lastConnectivityRef.current = false;

    if (!sessionId) {
      // Nothing to connect to — leave status at 'connecting' so the
      // caller can render a spinner while it waits for the session.
      return undefined;
    }

    // Periodic connectivity tick — re-evaluates the (wsOpen &&
    // now - lastEventAt < 90s) signal so the consumer sees the link
    // going stale even when no inbound frames arrive.
    connectivityTimerRef.current = setInterval(evaluateConnectivity, 5000);

    // Initial connect ALWAYS mints a fresh single-use ticket rather than
    // reusing the one-shot ticket from route state. This is required for
    // React StrictMode (dev) correctness: StrictMode double-invokes the
    // mount effect (mount → cleanup → mount), and the route-state ticket
    // is consumed by the first connect's handshake — the second mount
    // would then bounce with 4401 and kill the socket. Minting per
    // connect makes each mount independent. The route-state ticket is
    // used only as a fast-path for the very first attempt when it is
    // still present and unconsumed; if the mint endpoint is unavailable
    // we fall back to it.
    let cancelled = false;
    (async () => {
      let ticket = null;
      try {
        if (typeof proctoringApi.mintWsTicket === 'function') {
          const res = await proctoringApi.mintWsTicket(sessionId);
          ticket = res?.data?.wsTicket ?? res?.data?.ticket ?? null;
        }
      } catch {
        // Mint failed — fall back to the route-state ticket below.
      }
      if (!ticket) ticket = initialTicketRef.current;
      if (cancelled || unmountedRef.current || terminatedRef.current) return;
      if (!ticket) {
        finalizeTerminated('NO_TICKET');
        return;
      }
      connect(ticket);
    })();

    return () => {
      cancelled = true;
      unmountedRef.current = true;
      clearTimers();
      drainPendingAcks('UNMOUNTED');
      const ws = wsRef.current;
      wsRef.current = null;
      wsOpenRef.current = false;
      if (ws) {
        try { ws.close(1000, 'unmount'); } catch { /* ignored */ }
      }
    };
    // We intentionally key the lifecycle on `sessionId` only — the
    // initial ticket is captured into a ref above. Every connect mints
    // a fresh ticket via `proctoringApi`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  return {
    status,
    sendEvent,
    sendHeartbeat,
    sendFinish,
    sendQuit,
    lastEventAt,
    score,
    band,
  };
}
