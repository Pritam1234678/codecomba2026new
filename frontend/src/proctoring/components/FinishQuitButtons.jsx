import { useCallback, useEffect, useRef, useState } from 'react';

// ── Design tokens (Practice / Contest palette) ────────────────────────────────
//
// Inlined until `proctoring/constants.js` lands. Mirrors the palette used in
// `FullscreenGuard.jsx` so the proctoring surface stays visually cohesive.
// `destructive` is the only token unique to this component — it is used for
// the Quit button and its confirmation dialog accents (Req 24.3).
const C = {
  bg:          '#131313',
  surface:     '#1c1b1b',
  border:      '#50453b',
  primary:     '#f1bc8b',
  secondary:   '#e9c176',
  destructive: '#ffb4ab',
  muted:       '#d4c4b7',
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
  // Inline button row — placed by the parent (typically the arena bottom bar).
  row: {
    display: 'flex',
    gap: '12px',
    alignItems: 'center',
  },
  baseButton: {
    padding: '10px 22px',
    border: '1px solid transparent',
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    transition: 'opacity 120ms ease',
  },
  finishButton: {
    backgroundColor: C.primary,
    borderColor: C.primary,
    color: C.bg,
  },
  quitButton: {
    // Destructive button — outlined rather than filled to keep it visually
    // distinct from the primary Finish action while still using a warm
    // palette-consistent hue (Req 24.3).
    backgroundColor: 'transparent',
    borderColor: C.destructive,
    color: C.destructive,
  },
  buttonDisabled: {
    opacity: 0.5,
    cursor: 'not-allowed',
  },
  // Modal — same overlay pattern as FullscreenGuard.
  overlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(13, 13, 13, 0.96)',
    backdropFilter: 'blur(6px)',
    zIndex: 99998,
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
  },
  modalQuitAccent: {
    borderColor: C.destructive,
  },
  heading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '26px',
    fontWeight: 600,
    letterSpacing: '0.01em',
  },
  headingFinish: {
    color: C.primary,
  },
  headingQuit: {
    color: C.destructive,
  },
  body: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '14px',
    lineHeight: 1.55,
    color: C.muted,
  },
  warning: {
    margin: 0,
    padding: '12px 14px',
    border: `1px solid ${C.destructive}`,
    backgroundColor: 'rgba(255, 180, 171, 0.08)',
    color: C.destructive,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    lineHeight: 1.5,
    letterSpacing: '0.04em',
  },
  errorBanner: {
    margin: 0,
    padding: '10px 12px',
    border: `1px solid ${C.destructive}`,
    color: C.destructive,
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    lineHeight: 1.5,
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '10px',
    marginTop: '6px',
  },
  cancelButton: {
    padding: '10px 18px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.muted,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
  confirmFinishButton: {
    padding: '10px 22px',
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
  confirmQuitButton: {
    padding: '10px 22px',
    border: `1px solid ${C.destructive}`,
    backgroundColor: C.destructive,
    color: C.bg,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    fontWeight: 600,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
};

const COPY = {
  finish: {
    title: 'Finish Contest',
    body: 'Submitting will end your proctoring session and lock in your final state. You will not be able to make further changes after confirming.',
    confirmLabel: 'Finish & Submit',
    pendingLabel: 'Finishing…',
  },
  quit: {
    title: 'Quit Contest',
    body: 'Quitting abandons your attempt and ends the proctoring session.',
    warning:
      'This action is permanent. You will not be able to re-enter this proctored contest, even if it is still running.',
    confirmLabel: 'Quit Permanently',
    pendingLabel: 'Quitting…',
  },
};

/**
 * Candidate-facing exit controls for a proctored contest.
 *
 * Renders two visually distinct inline buttons (Req 24.1, 24.3):
 *   - **Finish** — primary palette (`#f1bc8b`), confirmation dialog
 *     summarises that submission ends the session.
 *   - **Quit** — destructive palette (`#ffb4ab`), confirmation dialog
 *     warns that quitting is permanent and re-entry is blocked
 *     (Req 24.4).
 *
 * Both confirmation dialogs are inline modals styled to match
 * `FullscreenGuard.jsx`. No third-party modal library is used.
 *
 * Both `onFinish` and `onQuit` are async-friendly: the component awaits
 * the returned promise, keeps the dialog open while pending, and surfaces
 * any thrown error inline so the candidate can retry or cancel. The
 * server is the source of truth for end-reason mapping (Req 24.2 / 24.5);
 * this component only routes the user intent.
 *
 * @param {Object} props
 * @param {() => void | Promise<void>} [props.onFinish] - invoked when the
 *   candidate confirms the finish dialog. May return a promise.
 * @param {() => void | Promise<void>} [props.onQuit] - invoked when the
 *   candidate confirms the quit dialog. May return a promise.
 * @param {boolean} [props.disabled=false] - master switch. When true,
 *   both buttons are disabled and dialogs cannot be opened. Used by the
 *   arena while the WebSocket is closing or after the session has
 *   already ended.
 */
export default function FinishQuitButtons({
  onFinish = () => {},
  onQuit = () => {},
  disabled = false,
}) {
  // 'finish' | 'quit' | null
  const [openDialog, setOpenDialog] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Track unmount so a slow async callback can't setState on a gone tree.
  const mountedRef = useRef(true);
  useEffect(() => () => { mountedRef.current = false; }, []);

  // If the parent disables the controls mid-flight (e.g. session ended
  // remotely), close any open dialog so the candidate isn't stuck staring
  // at a confirm modal with no working buttons.
  useEffect(() => {
    if (disabled && !submitting) {
      setOpenDialog(null);
      setError(null);
    }
  }, [disabled, submitting]);

  const openFinish = useCallback(() => {
    if (disabled) return;
    setError(null);
    setOpenDialog('finish');
  }, [disabled]);

  const openQuit = useCallback(() => {
    if (disabled) return;
    setError(null);
    setOpenDialog('quit');
  }, [disabled]);

  const closeDialog = useCallback(() => {
    if (submitting) return; // can't bail out of an in-flight request
    setOpenDialog(null);
    setError(null);
  }, [submitting]);

  const handleConfirm = useCallback(async () => {
    if (!openDialog || submitting) return;
    const action = openDialog === 'finish' ? onFinish : onQuit;
    setSubmitting(true);
    setError(null);
    try {
      await action();
      if (!mountedRef.current) return;
      // On success leave it to the parent to navigate / re-render. We
      // close the dialog so a stale modal doesn't sit on top of the
      // terminal screen if the parent re-renders us briefly.
      setOpenDialog(null);
    } catch (err) {
      if (!mountedRef.current) return;
      const message =
        (err && (err.message || err.error)) ||
        'Could not complete the request. Please try again.';
      setError(String(message));
    } finally {
      if (mountedRef.current) setSubmitting(false);
    }
  }, [openDialog, submitting, onFinish, onQuit]);

  const onOverlayKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') closeDialog();
    },
    [closeDialog]
  );

  const isQuit = openDialog === 'quit';
  const copy = openDialog ? COPY[openDialog] : null;

  return (
    <>
      <div style={styles.row} role="group" aria-label="Contest exit controls">
        <button
          type="button"
          onClick={openFinish}
          disabled={disabled}
          aria-haspopup="dialog"
          aria-expanded={openDialog === 'finish'}
          style={{
            ...styles.baseButton,
            ...styles.finishButton,
            ...(disabled ? styles.buttonDisabled : null),
          }}
        >
          Finish Contest
        </button>
        <button
          type="button"
          onClick={openQuit}
          disabled={disabled}
          aria-haspopup="dialog"
          aria-expanded={openDialog === 'quit'}
          style={{
            ...styles.baseButton,
            ...styles.quitButton,
            ...(disabled ? styles.buttonDisabled : null),
          }}
        >
          Quit Contest
        </button>
      </div>

      {openDialog && copy && (
        <div
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="fq-dialog-title"
          aria-describedby="fq-dialog-body"
          style={styles.overlay}
          onKeyDown={onOverlayKeyDown}
        >
          <div
            style={{
              ...styles.modal,
              ...(isQuit ? styles.modalQuitAccent : null),
            }}
          >
            <h2
              id="fq-dialog-title"
              style={{
                ...styles.heading,
                ...(isQuit ? styles.headingQuit : styles.headingFinish),
              }}
            >
              {copy.title}
            </h2>
            <p id="fq-dialog-body" style={styles.body}>
              {copy.body}
            </p>
            {isQuit && (
              <p style={styles.warning} role="note">
                {copy.warning}
              </p>
            )}
            {error && (
              <p style={styles.errorBanner} role="alert">
                {error}
              </p>
            )}
            <div style={styles.actions}>
              <button
                type="button"
                onClick={closeDialog}
                disabled={submitting}
                style={{
                  ...styles.cancelButton,
                  ...(submitting ? styles.buttonDisabled : null),
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                disabled={submitting}
                autoFocus
                style={{
                  ...(isQuit
                    ? styles.confirmQuitButton
                    : styles.confirmFinishButton),
                  ...(submitting ? styles.buttonDisabled : null),
                }}
              >
                {submitting ? copy.pendingLabel : copy.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
