import { useState } from 'react';
import { C } from '../constants';

// ── Styles ────────────────────────────────────────────────────────────────────
//
// The consent screen is rendered inline by `ProctoredContestEntry` rather
// than as a modal overlay — Req 2.2 forbids access to the contest UI
// while consent is pending, so a dedicated screen is clearer than a
// dismissible dialog. The component is named `ConsentDialog` to match
// `design.md`'s component map.
const styles = {
  root: {
    display: 'flex',
    justifyContent: 'center',
    padding: '48px 24px',
  },
  card: {
    width: '100%',
    maxWidth: '720px',
    backgroundColor: C.surfaceLow,
    border: `1px solid ${C.border}`,
    padding: '40px 36px',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
  },
  topAccent: {
    height: '2px',
    backgroundColor: C.secondary,
    margin: '-40px -36px 0',
  },
  eyebrow: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.2em',
    color: C.outline,
    textTransform: 'uppercase',
  },
  heading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '32px',
    fontWeight: 700,
    color: C.primary,
    letterSpacing: '-0.01em',
    lineHeight: 1.15,
  },
  intro: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '15px',
    lineHeight: 1.6,
    color: C.muted,
  },
  list: {
    listStyle: 'none',
    margin: 0,
    padding: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  listItem: {
    display: 'grid',
    gridTemplateColumns: '28px 1fr',
    gap: '14px',
    alignItems: 'start',
    padding: '14px 16px',
    border: `1px solid ${C.border}`,
    backgroundColor: C.bg,
  },
  bullet: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color: C.secondary,
    letterSpacing: '0.1em',
    paddingTop: '2px',
  },
  itemTitle: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    color: C.primary,
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
  },
  itemBody: {
    margin: '4px 0 0',
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    lineHeight: 1.55,
    color: C.outline,
  },
  versionLine: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color: C.outline,
    letterSpacing: '0.08em',
  },
  ackRow: {
    display: 'flex',
    gap: '10px',
    alignItems: 'flex-start',
    padding: '14px 16px',
    border: `1px solid ${C.border}`,
    backgroundColor: C.bg,
  },
  checkbox: {
    marginTop: '3px',
    width: '16px',
    height: '16px',
    accentColor: C.secondary,
    cursor: 'pointer',
  },
  ackLabel: {
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    color: C.muted,
    lineHeight: 1.5,
    cursor: 'pointer',
  },
  buttonRow: {
    display: 'flex',
    gap: '12px',
    flexWrap: 'wrap',
    marginTop: '4px',
  },
  acceptButton: (disabled) => ({
    flex: '1 1 auto',
    minWidth: '180px',
    padding: '14px 28px',
    border: `1px solid ${C.secondary}`,
    backgroundColor: disabled ? 'transparent' : C.secondary,
    color: disabled ? C.outline : C.bg,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: disabled ? 'not-allowed' : 'pointer',
    transition: 'all 0.2s',
  }),
  declineButton: {
    flex: '0 1 auto',
    padding: '14px 24px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.outline,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
};

// ── Monitored signals ─────────────────────────────────────────────────────────
//
// Per Req 2.1 the consent screen MUST list every monitored signal
// before consent is recorded. Strings here are the source of truth for
// the candidate-facing wording.
const MONITORED_SIGNALS = [
  {
    id: 'fullscreen',
    title: 'Fullscreen Enforcement',
    body:
      'Your browser must remain in fullscreen for the entire contest. ' +
      'Exiting fullscreen pauses the editor and is recorded as a ' +
      'suspicious event.',
  },
  {
    id: 'focus',
    title: 'Tab and Window Focus',
    body:
      'Switching tabs, minimising the window, or losing focus is ' +
      'logged. The editor becomes read-only while the tab is hidden.',
  },
  {
    id: 'copy-paste',
    title: 'Copy, Paste, and Right-Click',
    body:
      'Copy, cut, paste, and right-click are blocked outside the code ' +
      'editor. Each blocked attempt is recorded.',
  },
  {
    id: 'webcam',
    title: 'Webcam-Based Face Detection',
    body:
      'A face-detection model runs entirely in your browser. Raw ' +
      'video never leaves your device. Only face counts and a still ' +
      'frame on suspicious events are sent to the server.',
  },
  {
    id: 'screenshots',
    title: 'On-Event Screenshot Capture',
    body:
      'When a suspicious event fires, one still frame is captured ' +
      'from your webcam and uploaded for review. Screenshots are ' +
      'never captured on a periodic timer.',
  },
];

/**
 * Inline consent screen rendered before the preflight check.
 *
 * Implements Requirement 2:
 *   - 2.1: lists every monitored signal
 *   - 2.2: parent gates contest UI access while this screen is visible
 *   - 2.3: `onAccept` is wired by the parent to
 *          `proctoringApi.consent(contestId, consentVersion)`
 *   - 2.5: `onDecline` is wired by the parent to navigate back to
 *          the contest list
 *
 * Stateless w.r.t. server I/O — the parent owns the API calls.
 *
 * @param {Object} props
 * @param {string} props.contestName - human-readable contest name for
 *   the heading.
 * @param {number} props.consentVersion - version stamp included in the
 *   ack and shown to the candidate for transparency.
 * @param {() => void} props.onAccept - called when the candidate clicks
 *   "Accept and Continue" (only enabled after the explicit checkbox).
 * @param {() => void} props.onDecline - called when the candidate
 *   clicks "Decline".
 * @param {boolean} [props.submitting=false] - parent-controlled flag
 *   that disables both buttons while the ack request is in flight.
 */
export default function ConsentDialog({
  contestName,
  consentVersion,
  onAccept,
  onDecline,
  submitting = false,
}) {
  const [acked, setAcked] = useState(false);
  const acceptDisabled = !acked || submitting;

  return (
    <div style={styles.root}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="consent-title"
        style={styles.card}
      >
        <div style={styles.topAccent} />

        <p style={styles.eyebrow}>Proctored Contest · Consent</p>
        <h1 id="consent-title" style={styles.heading}>
          {contestName ? `Consent to Proctor "${contestName}"` : 'Proctoring Consent'}
        </h1>
        <p style={styles.intro}>
          Before you start, please review what is monitored during this
          proctored contest. You can decline at any time and return to
          the contest list. Nothing is recorded until you accept.
        </p>

        <ul style={styles.list}>
          {MONITORED_SIGNALS.map((sig, i) => (
            <li key={sig.id} style={styles.listItem}>
              <span style={styles.bullet}>{String(i + 1).padStart(2, '0')}</span>
              <div>
                <p style={styles.itemTitle}>{sig.title}</p>
                <p style={styles.itemBody}>{sig.body}</p>
              </div>
            </li>
          ))}
        </ul>

        <p style={styles.versionLine}>
          Consent version: v{consentVersion ?? '1'}
        </p>

        <label style={styles.ackRow}>
          <input
            type="checkbox"
            checked={acked}
            onChange={(e) => setAcked(e.target.checked)}
            disabled={submitting}
            style={styles.checkbox}
          />
          <span style={styles.ackLabel}>
            I have read and accept the proctoring terms above. I
            understand my webcam will be active for face detection and
            that screenshots may be captured on suspicious events.
          </span>
        </label>

        <div style={styles.buttonRow}>
          <button
            type="button"
            onClick={onAccept}
            disabled={acceptDisabled}
            style={styles.acceptButton(acceptDisabled)}
          >
            {submitting ? 'Submitting…' : 'Accept and Continue'}
          </button>
          <button
            type="button"
            onClick={onDecline}
            disabled={submitting}
            style={styles.declineButton}
          >
            Decline
          </button>
        </div>
      </div>
    </div>
  );
}
