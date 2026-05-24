import { useCallback, useEffect, useRef, useState } from 'react';
import { enqueue, cancelQueue } from '../services/duelService';
import api from '../services/api';

// ─── State machine ──────────────────────────────────────────────────────────
// IDLE      → user is not queued (initial / cancelled / error reset / matched)
// AWAITING  → POST /api/duels/queue succeeded; waiting for `matched` SSE event
// COOLDOWN  → server returned 429 with Retry-After; ticking down to 0 → IDLE
// ERROR     → unexpected failure; surfaced via `error` state
const STATES = {
  IDLE: 'IDLE',
  AWAITING: 'AWAITING',
  COOLDOWN: 'COOLDOWN',
  ERROR: 'ERROR',
};

/**
 * Listens for matchmaking-related events on the user's existing
 * `/api/submissions/stream` per-user SSE channel (no new SSE transport). The
 * backend re-uses that channel to deliver:
 *   - `matched`         { matchId } — pair has been assembled, navigate now
 *   - `queue_timeout`   {}          — 120s sweep dropped this user; retry
 *   - `pairing_failed`  { reason }  — pair-loop hit a conflict; retry
 *
 * Returns `{ state, queueToken, matchId, cooldownSeconds, error, findMatch,
 * cancel, STATES }` so the lobby can render Find Match / Cancel / cooldown.
 */
export default function useDuelMatchmaking() {
  const [state, setState] = useState(STATES.IDLE);
  const [queueToken, setQueueToken] = useState(null);
  const [matchId, setMatchId] = useState(null);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const [error, setError] = useState(null);

  const eventSourceRef = useRef(null);
  const cooldownTimerRef = useRef(null);
  const matchPollRef = useRef(null);

  // ── Per-user SSE channel ─────────────────────────────────────────────────
  // Reuses the existing /api/submissions/stream endpoint — same ticket flow
  // as ProblemSolve.jsx. We never open a new transport for matchmaking; we
  // just listen for the duel-specific event names on the user's channel.
  const openPerUserSse = useCallback(async () => {
    try {
      // Issue a single-use SSE ticket (64-char hex, EX 60s, GETDEL on consume).
      const ticketResp = await api.post('/submissions/sse-ticket');
      const ticket = ticketResp.data?.ticket;
      if (!ticket) {
        console.warn('Duel matchmaking: no ticket returned');
        return;
      }

      // Build the EventSource URL using the same base resolution as api.js.
      const base = import.meta.env.VITE_API_URL ?? '/api';
      const url = `${base}/submissions/stream?ticket=${encodeURIComponent(ticket)}`;
      const es = new EventSource(url);

      es.addEventListener('matched', (ev) => {
        try {
          const data = JSON.parse(ev.data);
          // Stop the polling fallback — we got the SSE event.
          if (matchPollRef.current) {
            clearInterval(matchPollRef.current);
            matchPollRef.current = null;
          }
          setMatchId(data.matchId);
          // Reset to IDLE — caller observes `matchId` and navigates to the arena.
          setState(STATES.IDLE);
          setError(null);
        } catch (e) {
          console.error('Duel matchmaking: matched parse error', e);
        }
      });

      es.addEventListener('queue_timeout', () => {
        setState(STATES.IDLE);
        setQueueToken(null);
        setError('Queue timed out — try again');
      });

      es.addEventListener('pairing_failed', (ev) => {
        try {
          const data = JSON.parse(ev.data);
          setState(STATES.IDLE);
          setQueueToken(null);
          setError(`Pairing failed: ${data.reason}`);
        } catch (e) {
          console.error('Duel matchmaking: pairing_failed parse error', e);
        }
      });

      es.onerror = (e) => {
        // EventSource auto-reconnects on transport drops; tickets are single-
        // use though, so a forced reconnect needs a fresh ticket. Just log.
        console.warn('Duel matchmaking SSE error', e);
      };

      eventSourceRef.current = es;
    } catch (err) {
      console.error('Duel matchmaking: failed to open per-user SSE', err);
    }
  }, []);

  // ── findMatch ─────────────────────────────────────────────────────────────
  const findMatch = useCallback(async () => {
    setError(null);
    try {
      // Open the per-user SSE listener BEFORE we hit the enqueue endpoint.
      // Otherwise the pair-loop (250 ms tick) can pair us, fire `matched`
      // on the per-user channel, and have the event silently dropped
      // because our EventSource isn't open yet.
      if (!eventSourceRef.current) {
        await openPerUserSse();
      }

      const result = await enqueue();
      setQueueToken(result.queueToken ?? null);
      setState(STATES.AWAITING);

      // Belt + suspenders: even if `matched` fires before SSE finishes
      // attaching, we re-poll the enqueue endpoint every 1.5 s. The
      // backend is idempotent and returns 409 ALREADY_IN_MATCH with the
      // existing matchId once we're paired — the catch block below
      // already handles that and surfaces matchId to the caller, which
      // navigates to the arena. We stop the poll on cleanup or after
      // a hard 30 s budget.
      let polls = 0;
      const pollMaxAttempts = 20; // 30 s
      const poll = setInterval(async () => {
        polls += 1;
        if (polls > pollMaxAttempts) {
          clearInterval(poll);
          if (matchPollRef.current === poll) matchPollRef.current = null;
          return;
        }
        try {
          await enqueue();
          // Still queued — the pair-loop hasn't matched us yet. Keep waiting.
        } catch (pollErr) {
          const status = pollErr?.response?.status;
          const data = pollErr?.response?.data;
          if (status === 409 && data?.error === 'ALREADY_IN_MATCH' && data?.matchId) {
            // Surface the matchId to the consumer; the lobby's useEffect
            // will navigate to /duel/{matchId} on the next render.
            setMatchId(data.matchId);
            setState(STATES.IDLE);
            clearInterval(poll);
            if (matchPollRef.current === poll) matchPollRef.current = null;
          }
          // Other errors (e.g. cooldown) are unlikely mid-await; ignore.
        }
      }, 1500);
      matchPollRef.current = poll;
    } catch (err) {
      const status = err?.response?.status;
      const data = err?.response?.data;

      if (status === 409 && data?.error === 'ALREADY_IN_MATCH') {
        // User is already a participant of an active match. The consumer
        // should observe `matchId` and navigate to /duel/{matchId}.
        setMatchId(data.matchId);
        setState(STATES.IDLE);
        return;
      }

      if (status === 429) {
        const retryAfter = parseInt(
          err.response.headers?.['retry-after'] ?? data?.retryAfterSec ?? 5,
          10
        );
        setCooldownSeconds(retryAfter);
        setState(STATES.COOLDOWN);

        // Tick down once per second; clear interval on reaching 0.
        if (cooldownTimerRef.current) clearInterval(cooldownTimerRef.current);
        cooldownTimerRef.current = setInterval(() => {
          setCooldownSeconds((s) => {
            if (s <= 1) {
              clearInterval(cooldownTimerRef.current);
              cooldownTimerRef.current = null;
              setState(STATES.IDLE);
              return 0;
            }
            return s - 1;
          });
        }, 1000);
        return;
      }

      setState(STATES.ERROR);
      setError(data?.message ?? data?.error ?? 'Failed to enter queue');
    }
  }, [openPerUserSse]);

  // ── cancel ────────────────────────────────────────────────────────────────
  const cancel = useCallback(async () => {
    if (matchPollRef.current) {
      clearInterval(matchPollRef.current);
      matchPollRef.current = null;
    }
    try {
      await cancelQueue();
    } catch (err) {
      // Cancel is idempotent on the backend (LREM 0 + DEL); a transient
      // failure is acceptable. Log and proceed to local reset.
      console.warn('Duel matchmaking: cancelQueue failed', err);
    }
    setState(STATES.IDLE);
    setQueueToken(null);
  }, []);

  // ── Cleanup on unmount ───────────────────────────────────────────────────
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        try { eventSourceRef.current.close(); } catch { /* noop */ }
        eventSourceRef.current = null;
      }
      if (cooldownTimerRef.current) {
        clearInterval(cooldownTimerRef.current);
        cooldownTimerRef.current = null;
      }
      if (matchPollRef.current) {
        clearInterval(matchPollRef.current);
        matchPollRef.current = null;
      }
    };
  }, []);

  return {
    state,
    queueToken,
    matchId,
    cooldownSeconds,
    error,
    findMatch,
    cancel,
    STATES,
  };
}
