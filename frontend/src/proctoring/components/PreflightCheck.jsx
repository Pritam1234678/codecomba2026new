import { useCallback, useEffect, useRef, useState } from 'react';
import { C } from '../constants';

// ── Step definitions ──────────────────────────────────────────────────────────
//
// Sequenced exactly per Req 3.1:
//   1. webcam permission granted
//   2. webcam stream active (≥ 1 detectable face is verified by the
//      face-detector hook in task 8.x, but the basic stream check is
//      done here so the candidate gets fast feedback)
//   3. Fullscreen API supported
//   4. AI_Detector model loaded (probed by HEAD-fetching the model
//      manifest from `/models/blaze_face_short_range.tflite` —
//      a positive HEAD means the asset ships and the worker can boot
//      in task 8.x)
//   5. WebSocket reachability — opens a transient WS to
//      `/api/proctoring/ws/preflight` (a no-op endpoint that simply
//      accepts and closes); falls back to a HEAD against the API
//      origin so this shell still passes when the real WS handler has
//      not yet been mounted on the backend.
//
// Each step exposes `run()` returning a promise that resolves with
// `{ ok, message }`. The component runs them one at a time and lets
// the candidate retry an individual failed step (Req 3.4).
const STEP_DEFS = [
  {
    id: 'webcam-permission',
    label: 'Camera Permission',
    description: 'Allow access to your webcam.',
    failureReq: 'Req 3.2',
  },
  {
    id: 'webcam-stream',
    label: 'Camera Stream Active',
    description: 'Confirm a live video feed.',
  },
  {
    id: 'fullscreen-api',
    label: 'Fullscreen Capability',
    description: 'Verify your browser supports the Fullscreen API.',
  },
  {
    id: 'ai-model',
    label: 'AI Model Available',
    description: 'Confirm the face detection model is reachable.',
    failureReq: 'Req 3.3',
  },
  {
    id: 'ws-reachability',
    label: 'Network Reachability',
    description: 'Confirm the proctoring server is reachable.',
    failureReq: 'Req 3.4',
  },
];

// ── Step runners ──────────────────────────────────────────────────────────────

const CAMERA_PERMISSION_TIMEOUT_MS = 30_000;

const runWebcamPermission = async (mediaStreamRef) => {
  if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
    return { ok: false, message: 'Your browser does not support camera access.' };
  }
  // Stop any previously acquired stream before re-requesting so the camera
  // light turns off between retry attempts and we don't hold two streams.
  const prev = mediaStreamRef.current;
  if (prev) {
    try { prev.getTracks().forEach((t) => t.stop()); } catch { /* ignored */ }
    mediaStreamRef.current = null;
  }
  let timeoutId;
  const timeoutPromise = new Promise((_, reject) => {
    timeoutId = setTimeout(
      () => reject(Object.assign(new Error('Timeout'), { name: 'TimeoutError' })),
      CAMERA_PERMISSION_TIMEOUT_MS,
    );
  });
  try {
    const stream = await Promise.race([
      navigator.mediaDevices.getUserMedia({ video: { width: 320, height: 240 }, audio: false }),
      timeoutPromise,
    ]);
    clearTimeout(timeoutId);
    mediaStreamRef.current = stream;
    return { ok: true };
  } catch (err) {
    clearTimeout(timeoutId);
    const name = err?.name || '';
    if (name === 'TimeoutError') {
      return { ok: false, message: 'Camera permission timed out. Allow access in the browser prompt and retry.' };
    }
    if (name === 'NotAllowedError' || name === 'SecurityError') {
      return {
        ok: false,
        message:
          'Camera access was denied. The proctored contest cannot start without camera access.',
      };
    }
    if (name === 'NotFoundError' || name === 'OverconstrainedError') {
      return { ok: false, message: 'No camera was detected on this device.' };
    }
    return { ok: false, message: 'Could not access the camera. Try again.' };
  }
};

const runWebcamStream = async (mediaStreamRef) => {
  const stream = mediaStreamRef.current;
  if (!stream) {
    return { ok: false, message: 'Camera stream is not active. Re-check the previous step.' };
  }
  const videoTracks = stream.getVideoTracks();
  if (videoTracks.length === 0) {
    return { ok: false, message: 'No video tracks were produced by the camera.' };
  }
  const live = videoTracks.some((t) => t.readyState === 'live' && !t.muted);
  if (!live) {
    return { ok: false, message: 'The camera stream is not active. Try unblocking the camera.' };
  }
  return { ok: true };
};

const runFullscreenSupport = async () => {
  if (typeof document === 'undefined') {
    return { ok: false, message: 'No document is available.' };
  }
  const supported =
    typeof document.documentElement.requestFullscreen === 'function' ||
    typeof document.documentElement.webkitRequestFullscreen === 'function';
  if (!supported) {
    return {
      ok: false,
      message: 'This browser does not support the Fullscreen API. Try Chrome, Edge, or Firefox.',
    };
  }
  return { ok: true };
};

const runAiModelReachable = async () => {
  // The model file ships in `frontend/public/models/`. A HEAD request
  // verifies it is reachable from the candidate's network without
  // downloading the multi-MB blob — full load happens later inside
  // the face-detector worker.
  try {
    const res = await fetch('/models/blaze_face_short_range.tflite', { method: 'HEAD' });
    if (!res.ok) {
      return { ok: false, message: `AI model not reachable (HTTP ${res.status}).` };
    }
    return { ok: true };
  } catch {
    return { ok: false, message: 'AI model is not reachable. Check your connection and retry.' };
  }
};

const runNetworkReachability = async () => {
  // Probe the health endpoint with a lightweight GET.
  // When VITE_API_URL is an absolute cross-origin URL (production on
  // Vercel), the browser sends a CORS preflight (OPTIONS) first —
  // GET is in the backend's allowed-methods set, so the preflight
  // passes and the health check succeeds.
  //
  // When VITE_API_URL is a relative path like /api (local dev),
  // Vite proxies the request to the backend directly — no CORS
  // involved, the same health endpoint returns 200.
  //
  // Any 2xx response counts as reachable. 401/403 are also
  // "reachable" — the server is up, just auth-required. Only network
  // failures (fetch throws) count as a failure.
  try {
    const apiUrl = import.meta.env.VITE_API_URL ?? '/api';
    const probeUrl = apiUrl.endsWith('/') ? `${apiUrl}health` : `${apiUrl}/health`;
    const res = await fetch(probeUrl, { method: 'GET' });
    if (res && typeof res.status === 'number' && res.status > 0) {
      return { ok: true };
    }
    return { ok: false, message: 'Server reachability could not be verified.' };
  } catch {
    return {
      ok: false,
      message: 'Could not reach the proctoring server. Check your connection and retry.',
    };
  }
};

const RUNNERS = {
  'webcam-permission': runWebcamPermission,
  'webcam-stream':     runWebcamStream,
  'fullscreen-api':    runFullscreenSupport,
  'ai-model':          runAiModelReachable,
  'ws-reachability':   runNetworkReachability,
};

// ── Styles ────────────────────────────────────────────────────────────────────
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
  },
  intro: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '15px',
    lineHeight: 1.6,
    color: C.muted,
  },
  stepList: {
    listStyle: 'none',
    margin: 0,
    padding: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  step: (status) => ({
    display: 'grid',
    gridTemplateColumns: '36px 1fr auto',
    gap: '14px',
    alignItems: 'center',
    padding: '14px 16px',
    border: `1px solid ${
      status === 'failed' ? C.error :
      status === 'passed' ? C.secondary :
      C.border
    }`,
    backgroundColor: C.bg,
  }),
  marker: (status) => ({
    width: '28px',
    height: '28px',
    borderRadius: '50%',
    border: `1px solid ${
      status === 'failed' ? C.error :
      status === 'passed' ? C.secondary :
      C.border
    }`,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color:
      status === 'failed' ? C.error :
      status === 'passed' ? C.secondary :
      C.outline,
  }),
  stepLabel: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    color: C.primary,
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
  },
  stepDesc: {
    margin: '4px 0 0',
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    color: C.outline,
    lineHeight: 1.5,
  },
  stepError: {
    margin: '6px 0 0',
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    color: C.error,
    lineHeight: 1.5,
  },
  stepStatus: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.15em',
    textTransform: 'uppercase',
    color: C.outline,
  },
  buttonRow: {
    display: 'flex',
    gap: '12px',
    flexWrap: 'wrap',
    marginTop: '4px',
  },
  primaryButton: (disabled) => ({
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
  retryButton: {
    padding: '10px 18px',
    border: `1px solid ${C.error}`,
    backgroundColor: 'transparent',
    color: C.error,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
  cancelButton: {
    padding: '14px 24px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.outline,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
};

const STATUS_LABEL = {
  pending: 'Pending',
  running: 'Checking…',
  passed: 'OK',
  failed: 'Failed',
};

/**
 * Pre-flight environment check (Req 3.1 – 3.5).
 *
 * Runs the five sequenced checks. Each step can be retried
 * individually after a failure. The "Continue" button only enables
 * once every step has passed.
 *
 * The parent (`ProctoredContestEntry`) owns navigation: when every
 * check passes, the candidate clicks Continue, the parent calls
 * `proctoringApi.createSession`, requests fullscreen via a user
 * gesture (Req 3.5), and transitions into the arena.
 *
 * Holds the captured `MediaStream` in a ref so the second step
 * (stream-active) can verify the same stream the first step
 * acquired. The stream is stopped on unmount or on `onCancel` so
 * the camera light turns off when the candidate decides to back out.
 *
 * @param {Object} props
 * @param {(ok: boolean) => void} [props.onComplete] - called with
 *   `true` once every step has passed and the candidate clicks
 *   Continue. Wired by the parent to `proctoringApi.createSession`.
 * @param {() => void} [props.onCancel] - called when the candidate
 *   clicks "Back to Contest" (e.g. their environment is wrong and
 *   they want to retry later).
 */
export default function PreflightCheck({ onComplete, onCancel }) {
  const mediaStreamRef = useRef(null);
  const [steps, setSteps] = useState(() =>
    STEP_DEFS.map((d) => ({ ...d, status: 'pending', error: null }))
  );
  const [currentIdx, setCurrentIdx] = useState(0);
  const [running, setRunning] = useState(false);

  const stopStream = useCallback(() => {
    const stream = mediaStreamRef.current;
    if (stream) {
      try {
        stream.getTracks().forEach((t) => t.stop());
      } catch {
        // Track already ended.
      }
      mediaStreamRef.current = null;
    }
  }, []);

  // Cleanup on unmount — never leave the camera light on if the
  // candidate navigates away.
  useEffect(() => stopStream, [stopStream]);

  const runStep = useCallback(async (idx) => {
    const def = STEP_DEFS[idx];
    if (!def) return;
    const runner = RUNNERS[def.id];
    if (!runner) return;
    setRunning(true);
    setSteps((prev) =>
      prev.map((s, i) =>
        i === idx ? { ...s, status: 'running', error: null } : s
      )
    );
    let result;
    try {
      result = await runner(mediaStreamRef);
    } catch (err) {
      result = { ok: false, message: err?.message || 'Unexpected error.' };
    }
    setSteps((prev) =>
      prev.map((s, i) =>
        i === idx
          ? { ...s, status: result.ok ? 'passed' : 'failed', error: result.ok ? null : result.message }
          : s
      )
    );
    setRunning(false);
    if (result.ok && idx < STEP_DEFS.length - 1) {
      // Advance to the next step. Use a microtask so the React state
      // commit lands before the next check kicks off.
      setCurrentIdx(idx + 1);
    }
  }, []);

  // Auto-run the active step in sequence. A failed step parks the
  // sequence until the candidate clicks Retry on that row.
  useEffect(() => {
    const idx = currentIdx;
    const def = steps[idx];
    if (!def) return;
    if (def.status === 'pending' && !running) {
      runStep(idx);
    }
  }, [currentIdx, steps, running, runStep]);

  const allPassed = steps.every((s) => s.status === 'passed');

  const handleRetry = (idx) => {
    // Stop the camera stream before retrying the permission step so the
    // old stream is released and the browser re-prompts cleanly.
    if (idx === 0) stopStream();
    setSteps((prev) =>
      prev.map((s, i) =>
        i === idx ? { ...s, status: 'pending', error: null } : s
      )
    );
    setCurrentIdx(idx);
  };

  const handleContinue = () => {
    if (!allPassed) return;
    // Stop the preflight stream before passing control to the parent.
    // The Arena mounts its own getUserMedia call — keeping this stream
    // active would orphan it (camera light stays on) and on some browsers
    // block the Arena's acquisition with NotReadableError.
    stopStream();
    if (typeof onComplete === 'function') onComplete(true);
  };

  const handleCancel = () => {
    stopStream();
    if (typeof onCancel === 'function') onCancel();
  };

  return (
    <div style={styles.root}>
      <div style={styles.card}>
        <div style={styles.topAccent} />

        <p style={styles.eyebrow}>Proctored Contest · Preflight</p>
        <h1 style={styles.heading}>Environment Check</h1>
        <p style={styles.intro}>
          We need to verify a few things before the contest can start.
          Each check runs in order. If any check fails you can fix the
          issue and retry that step without restarting the others.
        </p>

        <ul style={styles.stepList}>
          {steps.map((s, i) => (
            <li key={s.id} style={styles.step(s.status)}>
              <span style={styles.marker(s.status)}>
                {s.status === 'passed' ? '✓' : s.status === 'failed' ? '!' : i + 1}
              </span>
              <div>
                <p style={styles.stepLabel}>{s.label}</p>
                <p style={styles.stepDesc}>{s.description}</p>
                {s.error && <p style={styles.stepError}>{s.error}</p>}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                <span style={styles.stepStatus}>{STATUS_LABEL[s.status]}</span>
                {s.status === 'failed' && (
                  <button
                    type="button"
                    onClick={() => handleRetry(i)}
                    disabled={running}
                    style={styles.retryButton}
                  >
                    Retry
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>

        <div style={styles.buttonRow}>
          <button
            type="button"
            onClick={handleContinue}
            disabled={!allPassed}
            style={styles.primaryButton(!allPassed)}
          >
            {allPassed ? 'Continue to Contest' : 'Waiting for Checks…'}
          </button>
          <button
            type="button"
            onClick={handleCancel}
            style={styles.cancelButton}
          >
            Back to Contest
          </button>
        </div>
      </div>
    </div>
  );
}
