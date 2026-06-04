import { useMemo } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';

// ── Design tokens (Practice / Contest palette) ────────────────────────────────
//
// Mirrors the proctoring surface used by FullscreenGuard.jsx and
// FinishQuitButtons.jsx so the terminal screen feels cohesive with the
// arena it replaces. No "scary red" — the warm-cool palette communicates
// a finished state without panic.
const C = {
  bg:          '#131313',
  surface:     '#1c1b1b',
  border:      '#50453b',
  primary:     '#f1bc8b',
  secondary:   '#e9c176',
  muted:       '#d4c4b7',
  outline:     '#9d8e83',
  on:          '#e5e2e1',
  destructive: '#ffb4ab',
  ok:          '#4ade80',
};

// ── Reason → message map ──────────────────────────────────────────────────────
//
// The server's `end_reason` enum values (Req 13.x — `SELF_FINISHED`,
// `SELF_QUIT`, `ADMIN_FORCED`, `HEARTBEAT_TIMEOUT`, `CONTEST_ENDED`) are
// the canonical source of truth. The route also accepts the friendlier
// shorthand listed in the spec prompt (`FINISHED`, `QUIT`,
// `ADMIN_TERMINATED`, `LOCKOUT`, `RISK_BAND_HIGH`) so the page can be
// linked directly from the eligibility endpoint or the LOCKED_OUT (423)
// response without a translation step.
//
// `reEntry` is `false` for any reason that locks the candidate out for
// the remainder of the contest (Req 13.9, Req 24.6) — the page does NOT
// render a "back to contest" affordance for those cases.
const REASON_MESSAGES = {
  // Server enum values
  SELF_FINISHED: {
    title: 'Submission Recorded',
    body: 'Your submission has been recorded. Best of luck.',
    accent: C.ok,
    reEntry: false,
  },
  SELF_QUIT: {
    title: 'Session Ended',
    body: 'You exited the proctored contest. You cannot re-enter for this contest session.',
    accent: C.primary,
    reEntry: false,
  },
  ADMIN_FORCED: {
    title: 'Session Ended by Admin',
    body: 'An administrator ended your session. Contact support if you believe this was in error.',
    accent: C.destructive,
    reEntry: false,
  },
  HEARTBEAT_TIMEOUT: {
    title: 'Connection Lost',
    body: 'Your session ended after the connection went silent for too long. Your progress up to that point has been preserved.',
    accent: C.secondary,
    reEntry: false,
  },
  CONTEST_ENDED: {
    title: 'Contest Closed',
    body: 'The contest window has closed. Your final submission has been recorded.',
    accent: C.secondary,
    reEntry: false,
  },

  // Friendly shorthand (query-param style)
  FINISHED: {
    title: 'Submission Recorded',
    body: 'Your submission has been recorded. Best of luck.',
    accent: C.ok,
    reEntry: false,
  },
  QUIT: {
    title: 'Session Ended',
    body: 'You exited the proctored contest. You cannot re-enter for this contest session.',
    accent: C.primary,
    reEntry: false,
  },
  ADMIN_TERMINATED: {
    title: 'Session Ended by Admin',
    body: 'An administrator ended your session. Contact support if you believe this was in error.',
    accent: C.destructive,
    reEntry: false,
  },
  LOCKOUT: {
    title: 'Locked Out',
    body: 'You are locked out of this contest. Once a proctored session ends with quit, admin termination, or a heartbeat timeout, re-entry for the same contest is not permitted.',
    accent: C.destructive,
    reEntry: false,
  },
  RISK_BAND_HIGH: {
    title: 'Session Ended',
    body: 'Session ended due to risk threshold. Your work has been preserved.',
    accent: C.secondary,
    reEntry: false,
  },
};

const FALLBACK_MESSAGE = {
  title: 'Session Ended',
  body: 'Your proctored contest session has ended.',
  accent: C.muted,
  reEntry: false,
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
  page: {
    minHeight: '100vh',
    width: '100%',
    backgroundColor: C.bg,
    color: C.on,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 24px',
  },
  card: {
    width: '100%',
    maxWidth: '560px',
    backgroundColor: C.surface,
    border: `1px solid ${C.border}`,
    padding: '40px 36px',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
    textAlign: 'center',
    alignItems: 'center',
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
    fontSize: '32px',
    fontWeight: 600,
    letterSpacing: '0.01em',
    lineHeight: 1.2,
  },
  body: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '15px',
    lineHeight: 1.6,
    color: C.muted,
    maxWidth: '440px',
  },
  divider: {
    width: '40px',
    height: '1px',
    backgroundColor: C.border,
    margin: '4px 0',
    border: 0,
  },
  meta: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.08em',
    color: C.outline,
  },
  actions: {
    marginTop: '12px',
    display: 'flex',
    gap: '12px',
    justifyContent: 'center',
    flexWrap: 'wrap',
  },
  primaryButton: {
    padding: '12px 26px',
    border: `1px solid ${C.primary}`,
    backgroundColor: C.primary,
    color: C.bg,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    transition: 'opacity 120ms ease',
  },
  ghostButton: {
    padding: '12px 22px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.muted,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    transition: 'opacity 120ms ease',
  },
};

/**
 * Terminal screen for a proctored contest session (Req 10.4, Req 13.9,
 * Req 24.6).
 *
 * Shown after the candidate's session has ended for any of the five
 * end-reason cases. The server's `end_reason` enum and the friendlier
 * query-param shorthand both resolve through the same map so the page
 * can be linked from:
 *
 *   - the WebSocket `SESSION_TERMINATED` frame (route state, task 12.3)
 *   - the eligibility endpoint when `locked` is true (entry-page redirect)
 *   - a direct 423 LOCKED_OUT response from the session-create endpoint
 *
 * Resolution order for the reason:
 *   1. `location.state.endReason`  (set by useProctoringSocket on
 *      `SESSION_TERMINATED`)
 *   2. `?reason=` query param      (LOCKED_OUT 423 redirect, eligibility
 *      page redirect)
 *   3. `location.state.reason`     (fallback name)
 *
 * Per Req 24.6 this page deliberately offers NO re-entry control for
 * `SELF_QUIT`, `ADMIN_FORCED`, or `HEARTBEAT_TIMEOUT` — only a back
 * link to the contest list. The same is true for the `LOCKOUT` and
 * `RISK_BAND_HIGH` shorthand. There is no proctoring activity on this
 * page (no fullscreen guard, no webcam, no WebSocket, no detectors).
 */
export default function ProctoredContestTerminated() {
  const { contestId } = useParams();
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();

  const reasonKey = useMemo(() => {
    const fromState =
      (location.state && (location.state.endReason || location.state.reason)) || null;
    const fromQuery = searchParams.get('reason');
    const raw = (fromState || fromQuery || '').toString().trim().toUpperCase();
    return raw;
  }, [location.state, searchParams]);

  const message = REASON_MESSAGES[reasonKey] || FALLBACK_MESSAGE;
  const detail =
    (location.state && location.state.message) ||
    (location.state && location.state.detail) ||
    null;

  const handleBack = () => {
    navigate('/contests', { replace: true });
  };

  const headingStyle = {
    ...styles.heading,
    color: message.accent || C.on,
  };

  return (
    <div style={styles.page}>
      <div role="status" aria-live="polite" style={styles.card}>
        <p style={styles.eyebrow}>Proctored Contest</p>
        <h1 style={headingStyle}>{message.title}</h1>
        <hr style={styles.divider} />
        <p style={styles.body}>{message.body}</p>
        {detail && <p style={{ ...styles.body, color: C.outline }}>{detail}</p>}
        {(reasonKey || contestId) && (
          <p style={styles.meta}>
            {contestId ? `Contest #${contestId}` : null}
            {contestId && reasonKey ? ' · ' : null}
            {reasonKey || null}
          </p>
        )}
        <div style={styles.actions}>
          <button type="button" onClick={handleBack} style={styles.primaryButton}>
            Back to Contests
          </button>
          <button
            type="button"
            onClick={() => navigate('/support')}
            style={styles.ghostButton}
          >
            Contact Support
          </button>
        </div>
      </div>
    </div>
  );
}
