import { useEffect, useRef } from 'react';

// ─── useTabFocusMonitor ─────────────────────────────────────────────────────
//
// Watches `visibilitychange`, `blur`, and `focus` and emits the three
// proctoring events the candidate WebSocket cares about (Req 5.1–5.4):
//
//   • `TAB_SWITCH`     — fired when `document.hidden` flips to `true` (the
//                        candidate switched tabs or minimised the window).
//   • `WINDOW_BLUR`    — fired when the window `blur` event arrives *while
//                        the document is still visible* (e.g. focus moved
//                        to a popup / devtools / another app that did not
//                        hide the tab). Skipped on plain tab switches so
//                        the same transition does not double-emit.
//   • `FOCUS_RESTORED` — fired exactly once when the candidate returns
//                        from either of the above. Carries `duration_ms`,
//                        the wall-clock interval the window was away.
//
// Per Req 5.5 the editor must be read-only and submission blocked while
// `document.hidden` is true. The hook surfaces that signal to the parent
// via the `onHiddenChange(isHidden)` callback so `ProctoredContestArena`
// can flip Monaco's `readOnly` flag and gate the submit button without
// re-rendering this hook.
//
// API:
//   useTabFocusMonitor({ onEvent, onHiddenChange, enabled = true })
//
//   onEvent({ type, payload })  — emit a Suspicious_Event. `type` is one
//                                 of `TAB_SWITCH`, `WINDOW_BLUR`,
//                                 `FOCUS_RESTORED`. `payload` is omitted
//                                 for the two transition events and is
//                                 `{ duration_ms }` for `FOCUS_RESTORED`.
//                                 `client_timestamp` is attached by the
//                                 socket layer, not here.
//   onHiddenChange(isHidden)    — invoked whenever `document.hidden`
//                                 transitions. Always called with a
//                                 boolean.
//
// Both callbacks are read through refs so callers do not have to memoise
// them to avoid listener thrashing.

export function useTabFocusMonitor({ onEvent, onHiddenChange, enabled = true } = {}) {
  const onEventRef = useRef(onEvent);
  const onHiddenChangeRef = useRef(onHiddenChange);

  useEffect(() => {
    onEventRef.current = onEvent;
  }, [onEvent]);

  useEffect(() => {
    onHiddenChangeRef.current = onHiddenChange;
  }, [onHiddenChange]);

  useEffect(() => {
    if (!enabled || typeof document === 'undefined' || typeof window === 'undefined') {
      return undefined;
    }

    // Focus-aware blur tracking: blur events are ONLY emitted after the
    // window has first received focus. This prevents phantom WINDOW_BLUR
    // during initial mount / fullscreen entry / screen share popup where
    // the browser fires blur as a normal part of the rendering lifecycle.
    // Once focus is established and lost, subsequent blurs are real.
    let hasFocused = false;
    let awaySince = null;
    let lastHidden = document.hidden;

    const emitEvent = (type, payload) => {
      const cb = onEventRef.current;
      if (typeof cb !== 'function') return;
      const frame = payload === undefined ? { type } : { type, payload };
      try {
        cb(frame);
      } catch {
        // Never let a downstream callback error break the listener wiring.
      }
    };

    const emitHidden = (hidden) => {
      const cb = onHiddenChangeRef.current;
      if (typeof cb !== 'function') return;
      try {
        cb(hidden);
      } catch {
        // Never let a downstream callback error break the listener wiring.
      }
    };

    const markAway = () => {
      if (awaySince == null) {
        awaySince = Date.now();
      }
    };

    // Restoration is only complete when the document is both visible
    // AND has focus. Calling this from either `focus` or the
    // visibilitychange→visible path collapses both signals onto a
    // single `FOCUS_RESTORED` emission per real away interval.
    const tryRestore = () => {
      const present = !document.hidden
        && (typeof document.hasFocus !== 'function' || document.hasFocus());
      if (present && awaySince != null) {
        const delta = Date.now() - awaySince;
        const duration_ms = delta < 0 ? 0 : delta;
        awaySince = null;
        emitEvent('FOCUS_RESTORED', { duration_ms });
      }
    };

    // Sync the parent at mount in case the document is already hidden
    // when the hook attaches (e.g. arena loaded in a background tab).
    emitHidden(lastHidden);
    if (lastHidden) markAway();

    const onVisibility = () => {
      const hidden = document.hidden;
      if (hidden !== lastHidden) {
        lastHidden = hidden;
        emitHidden(hidden);
      }
      if (hidden) {
        markAway();
        emitEvent('TAB_SWITCH');
      } else {
        tryRestore();
      }
    };

    const onBlur = () => {
      if (!hasFocused) return;
      // Req 5.3: emit WINDOW_BLUR only while the document is still visible.
      if (!document.hidden) {
        markAway();
        emitEvent('WINDOW_BLUR');
      }
    };

    const onFocus = () => {
      hasFocused = true;
      tryRestore();
    };

    document.addEventListener('visibilitychange', onVisibility);
    window.addEventListener('blur', onBlur);
    window.addEventListener('focus', onFocus);

    return () => {
      document.removeEventListener('visibilitychange', onVisibility);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onFocus);
    };
  }, [enabled]);
}

export default useTabFocusMonitor;
