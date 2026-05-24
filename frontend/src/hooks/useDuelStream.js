import { useCallback, useEffect, useReducer, useRef } from 'react';
import { issueDuelTicket, getMatch } from '../services/duelService';

// ─── State shape ─────────────────────────────────────────────────────────────
//
// The hook centralizes everything `DuelArena.jsx` needs to render:
//   • Match identity: matchId, problemId, userA, userB
//   • Lifecycle:      status (WAITING / IN_PROGRESS / FINISHED), startedAt,
//                     remainingSeconds, outcome, winnerUserId, winnerUsername,
//                     endedAt
//   • Per-opponent presence: opponentTyping, opponentDisconnected
//   • Last-N events for a debug overlay / dev tools
//   • Connection: connected, error
//
// The reducer is intentionally tolerant: we never crash on an unknown event
// shape — at worst we drop the event and keep the existing state.

const initialState = {
  matchId: null,
  status: null,
  startedAt: null,
  remainingSeconds: null,
  problemId: null,
  userA: null,
  userB: null,
  outcome: null,
  winnerUserId: null,
  winnerUsername: null,
  endedAt: null,
  opponentTyping: false,
  opponentDisconnected: false,
  events: [], // array of { type, data, ts }
  connected: false,
  error: null,
};

const MAX_EVENTS = 50;

function reducer(state, action) {
  switch (action.type) {
    case 'connected':
      return { ...state, connected: true, error: null };

    case 'room_state': {
      const d = action.data ?? {};
      return {
        ...state,
        matchId: d.matchId ?? state.matchId,
        status: d.status ?? state.status,
        startedAt: d.startedAt ?? state.startedAt,
        remainingSeconds:
          d.remainingSeconds !== undefined ? d.remainingSeconds : state.remainingSeconds,
        problemId: d.problemId ?? state.problemId,
        userA: d.userA ?? state.userA,
        userB: d.userB ?? state.userB,
        outcome: d.outcome ?? state.outcome,
        winnerUserId: d.winnerUserId ?? state.winnerUserId,
        winnerUsername: d.winnerUsername ?? state.winnerUsername,
        endedAt: d.endedAt ?? state.endedAt,
      };
    }

    case 'progress': {
      const d = action.data ?? {};
      const evt = d.event;
      const next = {
        ...state,
        events: [...state.events, { type: 'progress', data: d, ts: Date.now() }].slice(-MAX_EVENTS),
      };
      // The server stamps `userId` on every progress event. The hook does not
      // know which userId is "self" (the page passes `currentUserId` to
      // `useDuelStream`'s consumer-level filter). So we surface raw flags here
      // and let the page collapse them onto the opponent panel.
      if (evt === 'typing') {
        next.opponentTyping = true;
      } else if (evt === 'disconnected') {
        next.opponentDisconnected = true;
      } else if (evt === 'reconnected') {
        next.opponentDisconnected = false;
      }
      return next;
    }

    case 'progress_typing_clear':
      return { ...state, opponentTyping: false };

    case 'match_finished': {
      const d = action.data ?? {};
      return {
        ...state,
        status: 'FINISHED',
        outcome: d.outcome ?? state.outcome,
        winnerUserId: d.winnerUserId ?? state.winnerUserId,
        winnerUsername: d.winnerUsername ?? state.winnerUsername,
        endedAt: d.endedAt ?? state.endedAt,
      };
    }

    case 'error':
      return { ...state, error: action.error };

    case 'reset':
      return initialState;

    default:
      return state;
  }
}

/**
 * Build the SSE URL for a given match + ticket. The URL MUST contain only the
 * 64-char hex ticket as a query param — never the JWT (Property 18).
 */
function buildStreamUrl(matchId, ticket) {
  const base = import.meta.env.VITE_API_URL ?? '/api';
  return `${base}/duels/${matchId}/stream?ticket=${encodeURIComponent(ticket)}`;
}

/**
 * Attach all duel-scoped listeners to a fresh EventSource. Extracted so the
 * 401-retry path can re-bind without duplicating the body.
 */
function attachListeners(es, dispatch) {
  es.addEventListener('connected', () => dispatch({ type: 'connected' }));

  es.addEventListener('room_state', (ev) => {
    try {
      dispatch({ type: 'room_state', data: JSON.parse(ev.data) });
    } catch (e) {
      console.error('useDuelStream: room_state parse failed', e);
    }
  });

  es.addEventListener('progress', (ev) => {
    try {
      dispatch({ type: 'progress', data: JSON.parse(ev.data) });
    } catch (e) {
      console.error('useDuelStream: progress parse failed', e);
    }
  });

  es.addEventListener('match_finished', (ev) => {
    try {
      dispatch({ type: 'match_finished', data: JSON.parse(ev.data) });
    } catch (e) {
      console.error('useDuelStream: match_finished parse failed', e);
    }
  });

  // Heartbeat keeps the connection alive across proxies — payload is irrelevant.
  es.addEventListener('ping', () => {});
}

export default function useDuelStream(matchId) {
  const [state, dispatch] = useReducer(reducer, initialState);
  const esRef = useRef(null);
  const fallbackTimerRef = useRef(null);
  const retriedRef = useRef(false);

  const startPollingFallback = useCallback(() => {
    if (!matchId || fallbackTimerRef.current) return;
    fallbackTimerRef.current = setInterval(async () => {
      try {
        const view = await getMatch(matchId);
        dispatch({ type: 'room_state', data: view });
        if (view?.status === 'FINISHED') {
          dispatch({ type: 'match_finished', data: view });
          if (fallbackTimerRef.current) {
            clearInterval(fallbackTimerRef.current);
            fallbackTimerRef.current = null;
          }
        }
      } catch {
        // swallow — keep polling. A persistent 403 will already have routed
        // the user to the "not a participant" page before this hook mounted.
      }
    }, 3000);
  }, [matchId]);

  const openStream = useCallback(async () => {
    if (!matchId) return;
    try {
      const ticket = await issueDuelTicket(matchId);
      const es = new EventSource(buildStreamUrl(matchId, ticket));
      attachListeners(es, dispatch);

      es.onerror = (e) => {
        // EventSource will auto-reconnect on transient drops. We only act on
        // a hard close (readyState === CLOSED) to avoid thrashing.
        // eslint-disable-next-line no-console
        console.warn('useDuelStream SSE error', e);
        if (es.readyState === EventSource.CLOSED && !retriedRef.current) {
          retriedRef.current = true;
          // Re-issue ticket once and reopen.
          (async () => {
            try {
              const fresh = await issueDuelTicket(matchId);
              const replacement = new EventSource(buildStreamUrl(matchId, fresh));
              attachListeners(replacement, dispatch);
              replacement.onerror = () => {
                if (replacement.readyState === EventSource.CLOSED) {
                  dispatch({ type: 'error', error: 'Stream closed — falling back to polling' });
                  startPollingFallback();
                }
              };
              esRef.current = replacement;
            } catch {
              dispatch({ type: 'error', error: 'Stream auth failed — falling back to polling' });
              startPollingFallback();
            }
          })();
        }
      };

      esRef.current = es;
    } catch (err) {
      const status = err?.response?.status;
      if (status === 401 && !retriedRef.current) {
        retriedRef.current = true;
        // Re-issue and retry once.
        try {
          const ticket = await issueDuelTicket(matchId);
          const es = new EventSource(buildStreamUrl(matchId, ticket));
          attachListeners(es, dispatch);
          es.onerror = () => {
            if (es.readyState === EventSource.CLOSED) {
              dispatch({ type: 'error', error: 'Stream auth failed — falling back to polling' });
              startPollingFallback();
            }
          };
          esRef.current = es;
        } catch {
          dispatch({ type: 'error', error: 'Auth failed — falling back to polling' });
          startPollingFallback();
        }
      } else if (status === 403) {
        dispatch({ type: 'error', error: 'NOT_A_PARTICIPANT' });
      } else {
        dispatch({
          type: 'error',
          error: err?.response?.data?.message ?? 'Stream failed',
        });
      }
    }
  }, [matchId, startPollingFallback]);

  // Auto-clear opponentTyping after 1500ms of no further `typing` events. We
  // re-arm the timer on every state transition that flips `opponentTyping`
  // back to true.
  useEffect(() => {
    if (!state.opponentTyping) return undefined;
    const t = setTimeout(() => dispatch({ type: 'progress_typing_clear' }), 1500);
    return () => clearTimeout(t);
  }, [state.opponentTyping, state.events.length]);

  useEffect(() => {
    retriedRef.current = false;
    openStream();
    return () => {
      if (esRef.current) {
        esRef.current.close();
        esRef.current = null;
      }
      if (fallbackTimerRef.current) {
        clearInterval(fallbackTimerRef.current);
        fallbackTimerRef.current = null;
      }
      dispatch({ type: 'reset' });
    };
  }, [openStream]);

  return state;
}
