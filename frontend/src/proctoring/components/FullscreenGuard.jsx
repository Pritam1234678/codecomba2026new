import { useEffect, useRef } from 'react';
import { useFullscreen } from '../hooks/useFullscreen';

// ── Design tokens (Practice / Contest palette) ────────────────────────────────
//
// Inlined here until `proctoring/constants.js` lands in task 3.4. Once that
// module exists, swap these literals for the named exports so the palette has
// a single source of truth across the proctoring surface.
const C = {
  bg:        '#131313',
  surface:   '#1c1b1b',
  border:    '#50453b',
  primary:   '#f1bc8b',
  secondary: '#e9c176',
  muted:     '#d4c4b7',
};

const styles = {
  // Per Req 4.5 the modal must block ALL contest UI interactions while
  // visible. A fixed full-viewport overlay with a high z-index sits on
  // top of the (now non-fullscreen) arena and intercepts pointer events.
  overlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(13, 13, 13, 0.96)',
    backdropFilter: 'blur(6px)',
    zIndex: 99999,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '24px',
  },
  modal: {
    width: '100%',
    maxWidth: '480px',
    backgroundColor: C.surface,
    border: `1px solid ${C.border}`,
    padding: '32px 28px',
    display: 'flex',
    flexDirection: 'column',
    gap: '18px',
    alignItems: 'center',
    textAlign: 'center',
  },
  heading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '28px',
    fontWeight: 600,
    color: C.primary,
    letterSpacing: '0.01em',
  },
  body: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    lineHeight: 1.55,
    color: C.muted,
  },
  hint: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    color: C.border,
  },
  button: {
    marginTop: '6px',
    padding: '12px 28px',
    border: `1px solid ${C.secondary}`,
    backgroundColor: C.secondary,
    color: C.bg,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
};

/**
 * Blocking guard that enforces fullscreen for the contest root.
 *
 * Wraps `useFullscreen` so the guard owns both detection and recovery:
 *   - listens for `fullscreenchange` via the hook (Req 4.2)
 *   - on a real exit transition (true → false), invokes `onFullscreenExit`
 *     so the parent can stream a `FULLSCREEN_EXIT` Suspicious_Event over
 *     the proctoring WebSocket (Req 4.3)
 *   - while not fullscreen, renders a blocking modal that pauses the
 *     arena (Req 4.4, 4.5)
 *   - re-enters fullscreen on a user gesture — the resume button is the
 *     only interactive element on the overlay (Req 4.6)
 *
 * The guard does NOT fire `onFullscreenExit` on initial mount when the
 * candidate has never entered fullscreen — only true → false transitions
 * trigger the callback. This avoids a spurious `FULLSCREEN_EXIT` before
 * the candidate ever gets into fullscreen via the preflight gesture
 * (Req 3.5).
 *
 * The `useProctoringSocket` hook lands later (task 4.5). Until then the
 * parent (`ProctoredContestArena`) is responsible for wiring the
 * `onFullscreenExit` callback to `socket.sendEvent('FULLSCREEN_EXIT')`.
 *
 * @param {Object} props
 * @param {React.RefObject<HTMLElement>} props.targetRef - ref to the
 *   contest root element. Per design Q2 this is the contest root, not
 *   `document.documentElement`.
 * @param {() => void} [props.onFullscreenExit] - called once per
 *   fullscreen exit transition. The parent wraps this in
 *   `sendEvent('FULLSCREEN_EXIT')` once the WebSocket is connected.
 * @param {() => Promise<void>} [props.onEnter] - called when user clicks
 *   "Re-enter Fullscreen". The parent can use this user gesture to
 *   also request screen share (getDisplayMedia).
 * @param {boolean} [props.active=true] - master switch. When false the
 *   guard is fully disabled (no transition tracking, no overlay). Used
 *   by the arena to hold the guard until the WebSocket is open so the
 *   first emitted event is not lost.
 */
export default function FullscreenGuard({ targetRef, onFullscreenExit, onEnter, active = true }) {
  const { isFullscreen, enter } = useFullscreen(targetRef);
  const wasFullscreenRef = useRef(false);
  const onFullscreenExitRef = useRef(onFullscreenExit);

  useEffect(() => {
    onFullscreenExitRef.current = onFullscreenExit;
  }, [onFullscreenExit]);

  useEffect(() => {
    if (!active) return;
    if (isFullscreen) {
      wasFullscreenRef.current = true;
      return;
    }
    // Only fire on a real exit transition (true → false). Mount-time
    // false-state and `active` toggles must not emit a phantom event.
    if (wasFullscreenRef.current) {
      wasFullscreenRef.current = false;
      const cb = onFullscreenExitRef.current;
      if (typeof cb === 'function') {
        try {
          cb();
        } catch {
          // Never let a downstream callback error break the guard wiring.
        }
      }
    }
  }, [isFullscreen, active]);

  if (!active || isFullscreen) return null;

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="fs-guard-title"
      aria-describedby="fs-guard-body"
      style={styles.overlay}
    >
      <div style={styles.modal}>
        <h2 id="fs-guard-title" style={styles.heading}>Fullscreen Required</h2>
        <p id="fs-guard-body" style={styles.body}>
          Your contest is paused. Re-enter fullscreen to continue. Leaving
          fullscreen during a proctored contest is recorded as a suspicious
          event.
        </p>
        <p style={styles.hint}>Press the button below to resume.</p>
        <button type="button" onClick={enter} style={styles.button}>
          Re-enter Fullscreen
        </button>
      </div>
    </div>
  );
}
