import { useEffect, useRef, useState } from 'react';

// ── Design tokens (Practice / Contest palette) ──────────────────────────────
//
// Mirrors the literals in `ProctoredContestTerminated.jsx` and
// `FullscreenGuard.jsx` so the proctoring surface stays cohesive. We
// intentionally inline these instead of importing `proctoring/constants.js`
// so the guard has zero coupling to the wider proctoring module surface
// — it is a pure presentational component the arena drives.
const C = {
  bg:          '#131313',
  surface:     '#1c1b1b',
  border:      '#50453b',
  primary:     '#f1bc8b',
  secondary:   '#e9c176',
  muted:       '#d4c4b7',
  outline:     '#9d8e83',
  destructive: '#ffb4ab',
  success:     '#4ade80',
};

// ── Styles ──────────────────────────────────────────────────────────────────
//
// Same overlay pattern as `FullscreenGuard.jsx`: a fixed full-viewport
// dim layer with a high z-index that intercepts every pointer event.
// Per Req 11.5 the editor must be paused and submissions blocked while
// the modal is up — `pointerEvents: 'auto'` on the overlay plus the
// arena treating `connected === false` as a paused-state guard does the
// rest. The card visually echoes `ProctoredContestTerminated.jsx` so a
// candidate sees a coherent "session paused" → "session ended" arc.
const styles = {
  overlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(13, 13, 13, 0.94)',
    backdropFilter: 'blur(6px)',
    zIndex: 99997,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '24px',
  },
  card: {
    width: '100%',
    maxWidth: '520px',
    backgroundColor: C.surface,
    border: `1px solid ${C.border}`,
    padding: '40px 36px',
    display: 'flex',
    flexDirection: 'column',
    gap: '18px',
    alignItems: 'center',
    textAlign: 'center',
  },
  cardOk: {
    borderColor: C.success,
  },
  eyebrow: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.18em',
    textTransform: 'uppercase',
    color: C.outline,
  },
  heading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '28px',
    fontWeight: 600,
    letterSpacing: '0.01em',
    color: C.primary,
    lineHeight: 1.2,
  },
  headingOk: {
    color: C.success,
  },
  body: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    lineHeight: 1.55,
    color: C.muted,
    maxWidth: '420px',
  },
  divider: {
    width: '40px',
    height: '1px',
    backgroundColor: C.border,
    margin: '4px 0',
    border: 0,
  },
  // Spinner — pure CSS, sized to feel like an inline accent rather than
  // a spotlight. Rendered via a keyframe injected once per component
  // mount (see effect below) so we avoid pulling in a global stylesheet.
  spinner: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    border: `3px solid ${C.border}`,
    borderTopColor: C.secondary,
    animation: 'pcg-spin 900ms linear infinite',
  },
  countdownRow: {
    display: 'flex',
    alignItems: 'baseline',
    gap: '10px',
    marginTop: '4px',
  },
  countdownLabel: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.14em',
    textTransform: 'uppercase',
    color: C.outline,
  },
  countdown: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '34px',
    fontWeight: 700,
    letterSpacing: '0.02em',
    color: C.primary,
    fontVariantNumeric: 'tabular-nums',
  },
  countdownDanger: {
    color: C.destructive,
  },
  hint: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.08em',
    color: C.outline,
  },
};

// Keyframes are injected exactly once per page lifetime via a singleton
// `<style>` element. Avoids a full CSS import for a single 12-line rule.
const SPIN_KEYFRAMES_ID = '__proctoring_disconnection_guard_keyframes__';
function ensureSpinKeyframes() {
  if (typeof document === 'undefined') return;
  if (document.getElementById(SPIN_KEYFRAMES_ID)) return;
  const style = document.createElement('style');
  style.id = SPIN_KEYFRAMES_ID;
  style.textContent = '@keyframes pcg-spin { to { transform: rotate(360deg); } }';
  document.head.appendChild(style);
}

// How long the brief "Reconnected" confirmation stays visible after the
// connection is restored. Kept short so the candidate can return to the
// arena quickly — Req 11.5 doesn't require it, this is purely a UX nicety.
const RECONNECTED_FLASH_MS = 1200;

/**
 * Blocking modal that pauses the contest while the proctoring WebSocket
 * is down (Req 11.5, Req 24 connection guard).
 *
 * Lifecycle:
 *   - Mount with `connected === true` → renders nothing.
 *   - When `connected` flips to `false` we capture wall-clock time and
 *     start a 1 s tick. The modal renders "Connection lost. Trying to
 *     reconnect…" and a countdown of `maxOfflineSeconds - elapsed`.
 *   - When the countdown hits 0 we fire `onTimeoutExceeded()` exactly
 *     once. The parent decides what to do (typically navigate to the
 *     terminated screen with `reason=HEARTBEAT_TIMEOUT`).
 *   - When `connected` flips back to `true` mid-countdown we briefly
 *     show a "Reconnected" success card for ~1.2 s, then hide entirely.
 *
 * Notes on robustness:
 *   - We use `Date.now()` rather than counting ticks so a backgrounded
 *     tab (where `setInterval` is throttled) still fires the timeout
 *     close to the configured budget on focus return.
 *   - `onTimeoutExceeded` is read through a ref so the parent does not
 *     have to memoise it — same pattern as `FullscreenGuard`.
 *   - `maxOfflineSeconds` is clamped to a sane minimum (1 s). A value
 *     of 0 or negative would otherwise immediately expire and create a
 *     terminate-loop.
 *
 * @param {Object} props
 * @param {boolean} props.connected - canonical "WS open + last frame
 *   within stale window" signal from `useProctoringSocket`.
 * @param {() => void} [props.onTimeoutExceeded] - called once when the
 *   offline window has been exceeded. The parent typically navigates
 *   to `/contests/:id/proctored/terminated?reason=HEARTBEAT_TIMEOUT`.
 * @param {number} [props.maxOfflineSeconds=60] - the offline budget.
 */
export default function DisconnectionGuard({
  connected,
  onTimeoutExceeded,
  maxOfflineSeconds = 60,
}) {
  // Inject the spinner keyframes lazily, exactly once per page.
  useEffect(() => { ensureSpinKeyframes(); }, []);

  const cap = Math.max(1, Number.isFinite(maxOfflineSeconds) ? maxOfflineSeconds : 60);

  // `null` while connected (or never disconnected); a wall-clock ms
  // timestamp captured the moment connection was lost. Re-rendered into
  // the countdown via the per-second tick below.
  const [disconnectedAt, setDisconnectedAt] = useState(null);
  const [now, setNow] = useState(() => Date.now());

  // Brief "Reconnected" confirmation. Set to true when we go from
  // disconnected → connected; cleared after RECONNECTED_FLASH_MS so the
  // overlay tears down.
  const [showReconnected, setShowReconnected] = useState(false);

  // Refs that don't need to thrash the render tree.
  const onTimeoutRef = useRef(onTimeoutExceeded);
  const timeoutFiredRef = useRef(false);
  const wasDisconnectedRef = useRef(false);

  useEffect(() => { onTimeoutRef.current = onTimeoutExceeded; }, [onTimeoutExceeded]);

  // Flip the disconnect latch on every connectivity transition. The
  // tick + countdown side-effect runs in a separate effect below so
  // each concern stays self-contained.
  useEffect(() => {
    if (connected) {
      // True → false → true: surface the "Reconnected" flash before
      // tearing down. Pure mount with connected=true skips the flash.
      if (wasDisconnectedRef.current) {
        setShowReconnected(true);
        const t = setTimeout(() => setShowReconnected(false), RECONNECTED_FLASH_MS);
        wasDisconnectedRef.current = false;
        setDisconnectedAt(null);
        timeoutFiredRef.current = false;
        return () => clearTimeout(t);
      }
      // Already connected and was never disconnected — nothing to do.
      setDisconnectedAt(null);
      return undefined;
    }
    // connected === false. Start (or resume) the countdown. We only
    // capture the timestamp on the first transition into disconnected
    // so a flapping link does not reset the timer.
    wasDisconnectedRef.current = true;
    setShowReconnected(false);
    setDisconnectedAt((prev) => (prev == null ? Date.now() : prev));
    return undefined;
  }, [connected]);

  // Per-second tick while disconnected. Drives both the countdown
  // re-render and the eventual `onTimeoutExceeded` call. Cleared the
  // moment we reconnect or unmount so React's strict-mode double-mount
  // does not leak intervals.
  useEffect(() => {
    if (disconnectedAt == null) return undefined;
    setNow(Date.now());
    const id = setInterval(() => {
      const t = Date.now();
      setNow(t);
      const elapsedSec = Math.floor((t - disconnectedAt) / 1000);
      if (elapsedSec >= cap && !timeoutFiredRef.current) {
        timeoutFiredRef.current = true;
        clearInterval(id);
        const cb = onTimeoutRef.current;
        if (typeof cb === 'function') {
          try { cb(); } catch { /* never let consumer crash the guard */ }
        }
      }
    }, 1000);
    return () => clearInterval(id);
  }, [disconnectedAt, cap]);

  // Nothing to render in the steady-state connected case.
  if (connected && !showReconnected) return null;

  // Compute remaining seconds. When the timer has already fired we
  // pin the display at 0 so the modal stays consistent until the
  // parent navigates away.
  const elapsedSec = disconnectedAt == null ? 0 : Math.floor((now - disconnectedAt) / 1000);
  const remaining = Math.max(0, cap - elapsedSec);
  const isDanger = remaining <= 10 && !showReconnected;

  if (showReconnected) {
    return (
      <div
        role="status"
        aria-live="polite"
        style={styles.overlay}
      >
        <div style={{ ...styles.card, ...styles.cardOk }}>
          <p style={styles.eyebrow}>Proctoring</p>
          <h2 style={{ ...styles.heading, ...styles.headingOk }}>Reconnected</h2>
          <hr style={styles.divider} />
          <p style={styles.body}>
            Connection restored. Returning you to the contest.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="dg-title"
      aria-describedby="dg-body"
      style={styles.overlay}
    >
      <div style={styles.card}>
        <div style={styles.spinner} aria-hidden="true" />
        <p style={styles.eyebrow}>Proctoring</p>
        <h2 id="dg-title" style={styles.heading}>Connection lost</h2>
        <hr style={styles.divider} />
        <p id="dg-body" style={styles.body}>
          Trying to reconnect&hellip; your contest is paused. Submissions
          are disabled until the connection is restored.
        </p>
        <div style={styles.countdownRow} aria-live="polite">
          <span style={styles.countdownLabel}>Time remaining</span>
          <span
            style={{
              ...styles.countdown,
              ...(isDanger ? styles.countdownDanger : null),
            }}
          >
            {remaining}s
          </span>
        </div>
        <p style={styles.hint}>
          Session ends automatically if disconnected for more than {cap}s.
        </p>
      </div>
    </div>
  );
}
