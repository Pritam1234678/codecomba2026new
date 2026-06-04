import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import FullscreenGuard from '../components/FullscreenGuard';
import DisconnectionGuard from '../components/DisconnectionGuard';
import FinishQuitButtons from '../components/FinishQuitButtons';
import ProblemSolveEmbed from '../components/ProblemSolveEmbed';
import { useTabFocusMonitor } from '../hooks/useTabFocusMonitor';
import { useCopyPasteBlocker } from '../hooks/useCopyPasteBlocker';
import useProctoringSocket from '../hooks/useProctoringSocket';
import useFaceDetector from '../hooks/useFaceDetector';
import proctoringApi from '../services/proctoringApi';
import api from '../../services/api';
import { C, EVENT_TYPES } from '../constants';

// Camera capture constants. The webcam stream is requested at 320×240
// (Req 7.1 — enough resolution for face count + centroid; design
// section "Inference Loop") with audio explicitly off (Req 23.4 — no
// audio capture). The on-screen preview is downscaled to 160×120 so it
// stays out of the way in the corner of the arena (design Components:
// "WebcamPreview.jsx — small corner preview during contest").
const CAMERA_CONSTRAINTS = Object.freeze({
  video: { width: 320, height: 240 },
  audio: false,
});

// Inference cadence handed to `useFaceDetector`. 1 s mirrors task 8.5's
// brief and stays well below the 5 s `noFaceThresholdSeconds` window so
// the state machine can fire on the right wall-clock boundary.
const FACE_INFERENCE_INTERVAL_MS = 1000;

// Screenshot capture dimensions. Match the design's `screenshotMaxWidth
// × screenshotMaxHeight` defaults (320 × 240) so the JPEG stays under
// the 256 KiB byte cap enforced by the backend (Req 8.4).
const SCREENSHOT_WIDTH = 320;
const SCREENSHOT_HEIGHT = 240;
const SCREENSHOT_JPEG_QUALITY = 0.7;

// Default offline budget per Req 11.5 (`maxOfflineSeconds`, default 60s).
// Once the proctoring WebSocket has been "disconnected" (combined
// `wsOpen && lastFrameAt < 90s` signal from `useProctoringSocket`) for
// longer than this window, the arena terminates the session as a
// `HEARTBEAT_TIMEOUT` and routes the candidate to the terminal screen.
const MAX_OFFLINE_SECONDS = 60;

// ─── ProctoredContestArena ──────────────────────────────────────────────────
//
// Task 6.4 wires the three browser detectors into the arena and routes
// every emitted Suspicious_Event over the proctoring WebSocket via
// `useProctoringSocket.sendEvent`. The arena body itself is still a
// placeholder ("Arena coming soon") — the real problem-solve UI lands
// in a follow-up task. What this file owns now:
//
//   • a stable `arenaRef` for the contest root, used by both the
//     fullscreen guard (Req 4.x) and the copy/paste blocker (Req 6.x)
//   • `useProctoringSocket` opened against the `{ sessionId, wsTicket }`
//     pair handed in via `location.state.session`
//   • `FullscreenGuard.onFullscreenExit` → `sendEvent('FULLSCREEN_EXIT')`
//     so a true → false transition streams a Suspicious_Event over WS
//   • `useTabFocusMonitor.onEvent` → `sendEvent('TAB_SWITCH' |
//     'WINDOW_BLUR' | 'FOCUS_RESTORED')`. Read-only / submit-block
//     enforcement (Req 5.5) is mirrored in local state via
//     `onHiddenChange` and surfaced through a "Paused" overlay until
//     the real arena UI lands.
//   • `useCopyPasteBlocker.onEvent` → `sendEvent('COPY_ATTEMPT' |
//     'CUT_ATTEMPT' | 'PASTE_ATTEMPT' | 'CONTEXT_MENU_ATTEMPT')` with
//     a `{ source: 'arena' }` payload so admins can tell where the
//     attempt fired (Req 6.2, 6.3).
//   • a tiny connection chip in the top-right corner showing the
//     socket `status`, current `band`, and `score`. Mirrors the
//     warm Practice palette tokens used elsewhere on the proctoring
//     surface (Req 23.8).
//
// Every `sendEvent` call is awaited with a `.catch` so a rejection
// (offline, EVENT_ACK_TIMEOUT, SESSION_TERMINATED) cannot crash the
// arena. The IndexedDB offline buffer + replay path lands in task 9.

// ── Styles ────────────────────────────────────────────────────────────────
const styles = {
  root: {
    minHeight: '100vh',
    backgroundColor: C.bg,
    color: C.onBg,
    fontFamily: "'Geist', sans-serif",
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
  },
  topBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '16px',
    padding: '14px 24px',
    borderBottom: `1px solid ${C.border}`,
    backgroundColor: C.surfaceLow,
  },
  topBarRight: {
    display: 'flex',
    alignItems: 'center',
    gap: '20px',
  },
  brand: {
    margin: 0,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.18em',
    textTransform: 'uppercase',
    color: C.secondary,
  },
  contestId: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.1em',
    color: C.outline,
    textTransform: 'uppercase',
  },
  body: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minHeight: 0,
  },

  // ── Sticky proctoring banner ───────────────────────────────────────
  // Rendered just below the top bar and above the 2-column workspace.
  // Stays sticky so the candidate is always reminded that activity is
  // being recorded — the message is intentionally calm (no neon, no
  // alerts) and only the score color escalates with the risk band.
  banner: {
    position: 'sticky',
    top: 0,
    zIndex: 60,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '16px',
    padding: '10px 24px',
    backgroundColor: C.surfaceCon,
    borderBottom: `1px solid ${C.border}`,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    letterSpacing: '0.08em',
    color: C.muted,
  },
  bannerLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    flexWrap: 'wrap',
  },
  bannerDot: {
    width: '6px',
    height: '6px',
    borderRadius: '50%',
    backgroundColor: C.error,
    boxShadow: `0 0 0 4px ${C.error}25`,
    display: 'inline-block',
  },
  bannerLabel: {
    color: C.secondary,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
  },
  bannerCopy: {
    color: C.outline,
  },
  bannerScore: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color: C.muted,
    letterSpacing: '0.08em',
  },

  // ── Two-column workspace ───────────────────────────────────────────
  workspace: {
    flex: 1,
    display: 'flex',
    minHeight: 0,
    overflow: 'hidden',
  },
  rail: {
    width: '280px',
    flexShrink: 0,
    backgroundColor: C.surfaceLow,
    borderRight: `1px solid ${C.border}`,
    display: 'flex',
    flexDirection: 'column',
    overflowY: 'auto',
  },
  railHeader: {
    padding: '14px 18px',
    borderBottom: `1px solid ${C.border}`,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    letterSpacing: '0.18em',
    color: C.outline,
    textTransform: 'uppercase',
  },
  railList: {
    display: 'flex',
    flexDirection: 'column',
    padding: '8px',
    gap: '6px',
  },
  railEmpty: {
    padding: '24px 18px',
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color: C.outline,
    fontStyle: 'italic',
  },
  problemCard: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.muted,
    textAlign: 'left',
    cursor: 'pointer',
    transition: 'background-color 0.15s, border-color 0.15s',
    fontFamily: "'Geist', sans-serif",
  },
  problemCardActive: {
    borderColor: C.secondary,
    backgroundColor: C.surfaceHi,
  },
  letterCircle: {
    width: '30px',
    height: '30px',
    borderRadius: '50%',
    flexShrink: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: `1px solid ${C.border}`,
    backgroundColor: C.surfaceCon,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    color: C.primary,
    letterSpacing: '0.04em',
  },
  letterCircleActive: {
    borderColor: C.secondary,
    color: C.secondary,
    backgroundColor: 'transparent',
  },
  problemMeta: {
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,
    flex: 1,
    gap: '4px',
  },
  problemTitle: {
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    color: C.onBg,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  problemSub: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    flexWrap: 'wrap',
  },
  diffBadge: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '9px',
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    padding: '1px 6px',
    border: `1px solid ${C.border}`,
  },
  problemLimits: {
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    color: C.outline,
  },
  rightPane: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,
    backgroundColor: C.bg,
  },
  emptyRight: {
    flex: 1,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 24px',
    textAlign: 'center',
  },
  emptyCard: {
    width: '100%',
    maxWidth: '480px',
    backgroundColor: C.surfaceLow,
    border: `1px solid ${C.border}`,
    padding: '32px 28px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    alignItems: 'center',
  },
  emptyHeading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '22px',
    fontWeight: 600,
    color: C.primary,
  },
  emptyCopy: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    lineHeight: 1.55,
    color: C.muted,
  },

  // ── Connection indicator ───────────────────────────────────────────
  // Small chip pinned to the top-right of the arena. Three rows:
  //   • status dot + label (e.g. "● live", "○ reconnecting")
  //   • risk band ("LOW" / "MEDIUM" / "HIGH"), color-coded
  //   • numeric risk score
  // Stays neutral and out-of-the-way; promotes to amber/red only when
  // the band escalates so the candidate is gently informed without a
  // distracting toast.
  chip: {
    position: 'fixed',
    top: '76px',
    right: '16px',
    zIndex: 100,
    minWidth: '160px',
    padding: '10px 14px',
    backgroundColor: C.surfaceLow,
    border: `1px solid ${C.border}`,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '11px',
    color: C.muted,
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
    pointerEvents: 'none',
  },
  chipRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '12px',
  },
  chipLabel: {
    color: C.outline,
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
  },
  chipValue: {
    color: C.muted,
    letterSpacing: '0.04em',
  },
  chipDot: {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    display: 'inline-block',
    marginRight: '8px',
  },

  // ── Hidden / paused overlay ────────────────────────────────────────
  // Mirrors Req 5.5 — while `document.hidden` is true the editor must
  // be read-only and submission blocked. Until the real arena UI lands,
  // we render a translucent overlay that visually pauses the placeholder.
  hiddenOverlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(13,13,13,0.86)',
    backdropFilter: 'blur(4px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 90,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    letterSpacing: '0.18em',
    textTransform: 'uppercase',
    color: C.outline,
  },

  // ── Webcam preview (corner-pip) ────────────────────────────────────
  // 160×120 picture-in-picture rendered in the bottom-left corner of
  // the arena. Stays at a low z-index so the disconnection guard
  // (z=200) and hidden-tab overlay (z=90) layer above it. The video
  // element is muted + autoPlay + playsInline so it begins drawing
  // frames as soon as the MediaStream is bound, and `pointer-events:
  // none` so it can never steal focus from the contest UI.
  webcamWrap: {
    position: 'fixed',
    left: '16px',
    bottom: '16px',
    zIndex: 50,
    width: '160px',
    border: `1px solid ${C.border}`,
    backgroundColor: C.surfaceLow,
    pointerEvents: 'none',
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '10px',
    color: C.outline,
    display: 'flex',
    flexDirection: 'column',
  },
  webcamVideo: {
    width: '160px',
    height: '120px',
    display: 'block',
    objectFit: 'cover',
    backgroundColor: '#000',
    // Mirror the preview so the candidate sees a "selfie" view.
    transform: 'scaleX(-1)',
  },
  webcamPlaceholder: {
    width: '160px',
    height: '120px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#000',
    color: C.outline,
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    textAlign: 'center',
    padding: '8px',
  },
  // Face-status pill rendered just below the preview frame. Mirrors
  // the connection chip style so the corner stays visually consistent.
  facePillRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '6px 8px',
    borderTop: `1px solid ${C.border}`,
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
  },
  facePillLabel: {
    color: C.outline,
  },
  facePillValue: {
    letterSpacing: '0.04em',
  },

  // ── Webcam-blocking modal (Req 7.6, Req 3.2) ───────────────────────
  // Rendered while the camera permission has been denied, the stream
  // was lost mid-session, or the AI model failed to initialise. The
  // arena cannot proctor without a stream, so the candidate is asked
  // to recover (re-grant permission, replug the camera) before the
  // contest UI is unblocked. Sits above every other overlay.
  cameraBlockOverlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(13,13,13,0.92)',
    backdropFilter: 'blur(6px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 999999,  // Above FullscreenGuard (99999) so camera prompt is visible
    padding: '24px',
  },
  cameraBlockCard: {
    width: '100%',
    maxWidth: '480px',
    backgroundColor: C.surfaceLow,
    border: `1px solid ${C.border}`,
    padding: '32px 28px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    textAlign: 'center',
  },
  cameraBlockHeading: {
    margin: 0,
    fontFamily: "'Playfair Display', serif",
    fontSize: '24px',
    fontWeight: 700,
    color: C.primary,
  },
  cameraBlockBody: {
    margin: 0,
    fontFamily: "'Geist', sans-serif",
    fontSize: '13px',
    lineHeight: 1.6,
    color: C.muted,
  },
  cameraBlockButton: {
    marginTop: '8px',
    alignSelf: 'center',
    padding: '12px 28px',
    border: `1px solid ${C.border}`,
    backgroundColor: 'transparent',
    color: C.muted,
    fontFamily: "'JetBrains Mono', monospace",
    fontSize: '12px',
    letterSpacing: '0.12em',
    textTransform: 'uppercase',
    cursor: 'pointer',
  },
};

// Map socket.status → (color, label) for the corner chip. Keeps the
// status copy short so the chip never overflows on narrow viewports.
const STATUS_LOOKUP = Object.freeze({
  connecting:   { color: C.outline,   label: 'connecting' },
  open:         { color: C.success,   label: 'live' },
  reconnecting: { color: C.warning,   label: 'reconnecting' },
  closed:       { color: C.outline,   label: 'closed' },
  terminated:   { color: C.error,     label: 'ended' },
});

// Risk band → text color for the chip's band row. Stays inside the
// Practice palette per Req 23.8.
const BAND_COLOR = Object.freeze({
  LOW:    C.muted,
  MEDIUM: C.warning,
  HIGH:   C.error,
});

// Difficulty token → (color, label) for the left-rail problem cards.
// Mirrors `DIFF_CFG` in `ContestDetail.jsx` but stays strictly inside
// the proctoring palette (no neon greens / orange accents).
const DIFF_CFG = Object.freeze({
  EASY:   { color: C.success,   label: 'Easy' },
  MEDIUM: { color: C.secondary, label: 'Medium' },
  HARD:   { color: C.error,     label: 'Hard' },
});

// Face-count → (label, color) for the corner pill. `1` is the only
// healthy state; `0` warns (no face), `≥2` errors (multiple faces).
// Stays inside the Practice palette per Req 23.8.
function faceMeta(count) {
  if (count === 1) return { label: 'face: 1',  color: C.success };
  if (count === 0) return { label: 'face: 0',  color: C.warning };
  return             { label: 'face: 2+', color: C.error };
}

export default function ProctoredContestArena() {
  const arenaRef = useRef(null);
  const videoRef = useRef(null);         // webcam stream
  const screenVideoRef = useRef(null);   // screen-capture stream
  const screenshotCanvasRef = useRef(null);
  const { contestId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const id = contestId;

  // Guard against landing on the arena URL directly without going
  // through the entry shell — arena requires a live session created
  // by `proctoringApi.createSession`. If we have no session info in
  // route state, bounce back to entry so the candidate goes through
  // eligibility → consent → preflight again.
  const session = location.state?.session ?? null;

  // ── WebSocket lifecycle ─────────────────────────────────────────
  // `useProctoringSocket` is unconditionally called below (hooks
  // can't be conditional), but with `sessionId === null` it stays in
  // 'connecting' and never opens — see the guard below the hook.
  const sessionId = session?.sessionId ?? null;
  const wsTicket = session?.wsTicket ?? null;

  // `document.hidden` mirror so we can render the paused overlay
  // (Req 5.5). Default false; the focus monitor pushes the truth.
  const [isHidden, setIsHidden] = useState(false);

  // Live "WS open + recent server frame" signal pushed by
  // `useProctoringSocket.onConnectivityChange`. The disconnection guard
  // (Req 11.5, Req 24) renders a blocking modal whenever this is false
  // and starts the `maxOfflineSeconds` countdown — when it expires the
  // arena terminates the session as `HEARTBEAT_TIMEOUT`.
  //
  // Default `true` so the guard does not flash a "Connection lost"
  // modal during the initial 'connecting' window before the first
  // server frame arrives. The socket flips this to `false` only on a
  // real connectivity drop.
  const [connected, setConnected] = useState(true);

  // ── Terminal redirect ───────────────────────────────────────────
  // The socket latches `terminated` on SESSION_TERMINATED or a
  // server-side terminal close (4401/4403/4408/4409). When that
  // happens we navigate to the terminated route so the candidate
  // sees the lockout / heartbeat-timeout / etc. messaging.
  const onTerminated = useCallback((endReason) => {
    navigate(`/contests/${id}/proctored/terminated`, {
      replace: true,
      state: { endReason },
    });
  }, [navigate, id]);

  // ── Disconnection-guard timeout ─────────────────────────────────
  // Fired by `DisconnectionGuard` once the offline window
  // (`MAX_OFFLINE_SECONDS`) has been exceeded without recovery. Per
  // Req 11.5 the candidate is routed to the terminated screen with
  // a synthetic `HEARTBEAT_TIMEOUT` reason — even though the server
  // may not have closed the WS yet, the browser-side budget has
  // been exhausted and submissions are no longer permitted.
  // We use `replace: true` so the candidate cannot use Back to
  // re-enter the now-unsupervised arena.
  const onOfflineTimeoutExceeded = useCallback(() => {
    navigate(
      `/contests/${id}/proctored/terminated?reason=HEARTBEAT_TIMEOUT`,
      { replace: true, state: { endReason: 'HEARTBEAT_TIMEOUT' } },
    );
  }, [navigate, id]);

  const {
    status: wsStatus,
    band,
    score,
    sendEvent,
    sendFinish,
    sendQuit,
  } = useProctoringSocket({
    sessionId,
    wsTicket,
    onTerminated,
    onConnectivityChange: setConnected,
  });

  // ── Finish / Quit lifecycle ─────────────────────────────────────
  // Tracks whether the candidate has already initiated a Finish or
  // Quit. Once true, the buttons stay disabled (and the confirmation
  // dialog stays in its pending state) until either:
  //   • the server pushes `SESSION_TERMINATED` → `onTerminated`
  //     navigates to the terminal screen and the arena unmounts, or
  //   • the safety-net timeout below fires and we reject the handler
  //     promise so the dialog re-enables and surfaces the error.
  //
  // The handlers below are intentionally async: `FinishQuitButtons`
  // awaits them, keeps its modal open while pending, and shows the
  // rejection message inline. The server is the source of truth for
  // the end-reason mapping (Req 24.2 / 24.5) — we only route intent.
  const [terminating, setTerminating] = useState(false);

  // Resolve / reject hooks for the in-flight Finish-or-Quit promise.
  // The promise resolves implicitly when `onTerminated` navigates and
  // unmounts the arena, so we hold a `reject` reference here for the
  // safety-net timeout path. `mountedRef` lets the safety-net no-op
  // if the component has already unmounted between fire-time and
  // resolve-time.
  const mountedRef = useRef(true);
  const exitRejectRef = useRef(null);

  // Single helper covering both flows. The WS frame path is preferred
  // when the socket is open; otherwise we fall back to the REST mirror
  // (`proctoringApi.finish` / `proctoringApi.quit`) so
  // the candidate can always end a session — even from inside the
  // disconnection guard's offline modal (Req 24.1, 24.2, 24.3, 24.5).
  // The REST handler performs the same conditional UPDATE as the WS
  // path, so duplicate clicks race cleanly: whichever caller wins
  // closes the row, and any later attempt sees `closed === false`
  // and is mapped to a friendly error.
  const initiateExit = useCallback((kind) => {
    if (terminating) {
      return Promise.reject(new Error('Already finishing the session.'));
    }

    setTerminating(true);

    // ── REST fallback path (WS not open) ─────────────────────────
    // No safety-net timer here: the REST call resolves or rejects
    // synchronously from the candidate's perspective. On 200 we
    // navigate to the terminated screen with the right `reason=`
    // shorthand (`FINISHED` / `QUIT`) so `ProctoredContestTerminated`
    // renders the matching copy. On any error we re-enable the
    // dialog so the candidate can retry — including the documented
    // 409 ALREADY_ENDED, which we surface as "session already
    // ended" so the candidate understands the click was a no-op.
    if (wsStatus !== 'open') {
      const restCall = kind === 'FINISH'
        ? proctoringApi.finish(sessionId)
        : proctoringApi.quit(sessionId);
      const reasonParam = kind === 'FINISH' ? 'FINISHED' : 'QUIT';

      return restCall
        .then(() => {
          if (!mountedRef.current) return;
          // Navigate replaces the arena entry so Back cannot return
          // the candidate to a now-unsupervised session.
          navigate(
            `/contests/${id}/proctored/terminated?reason=${reasonParam}`,
            { replace: true, state: { endReason: reasonParam } },
          );
        })
        .catch((err) => {
          if (!mountedRef.current) return Promise.reject(err);
          setTerminating(false);
          // Surface a friendly message to the dialog. The 409
          // ALREADY_ENDED case (server-side
          // `ProctoringStateConflictException`) means a race
          // already finalised the session — point the candidate at
          // the terminal screen instead of leaving them confused.
          const code = err?.response?.data?.error;
          const status = err?.response?.status;
          if (status === 409 && code === 'ALREADY_ENDED') {
            navigate(
              `/contests/${id}/proctored/terminated?reason=${reasonParam}`,
              { replace: true, state: { endReason: reasonParam } },
            );
            return;
          }
          const message = err?.response?.data?.message
            || err?.message
            || 'Could not end the session. Please try again.';
          return Promise.reject(new Error(message));
        });
    }

    // ── WS frame path (preferred when the socket is open) ────────
    return new Promise((resolve, reject) => {
      exitRejectRef.current = reject;

      // Safety-net: if the server never pushes SESSION_TERMINATED
      // (e.g. WS dies right after we send), reject after 10 s so the
      // dialog re-enables. The server-side close is the canonical
      // success path — `onTerminated` unmounts us before this fires.
      const timeoutId = setTimeout(() => {
        if (!mountedRef.current) return;
        exitRejectRef.current = null;
        setTerminating(false);
        reject(new Error(
          'The server did not confirm the request in time. Please try again.'
        ));
      }, 10_000);

      // Wrap reject so the timeout is cleared on early failure.
      const safeReject = (err) => {
        clearTimeout(timeoutId);
        if (!mountedRef.current) return;
        exitRejectRef.current = null;
        setTerminating(false);
        reject(err);
      };
      exitRejectRef.current = safeReject;

      try {
        if (kind === 'FINISH') sendFinish();
        else sendQuit();
      } catch (err) {
        safeReject(err instanceof Error ? err : new Error(String(err)));
        return;
      }

      // We deliberately do not call `resolve()` here. The success
      // signal is the server-pushed SESSION_TERMINATED frame, which
      // unmounts the arena via the navigate inside `onTerminated`.
      // Holding the promise pending keeps the FinishQuitButtons
      // modal in its "Finishing…" state right up until that nav.
      // The only consumer is `FinishQuitButtons` and it tolerates a
      // never-resolving promise on success because the modal is
      // unmounted along with the arena.
      void resolve;
    });
  }, [terminating, wsStatus, sendFinish, sendQuit, sessionId, id, navigate]);

  const handleFinish = useCallback(() => initiateExit('FINISH'), [initiateExit]);
  const handleQuit = useCallback(() => initiateExit('QUIT'), [initiateExit]);

  // Track unmount so the safety-net timeout never setState's after
  // the arena navigates away.
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (exitRejectRef.current) {
        try {
          // Resolve no-op rejection so any pending dialog fades out
          // cleanly if React keeps it mounted briefly post-nav.
          exitRejectRef.current(new Error('UNMOUNTED'));
        } catch { /* ignored */ }
        exitRejectRef.current = null;
      }
    };
  }, []);

  // Stable error sink for `sendEvent` rejections. The WS layer can
  // reject for three reasons today: SOCKET_NOT_OPEN (offline),
  // EVENT_ACK_TIMEOUT (server slow), and SESSION_TERMINATED. None of
  // these should crash the arena; the offline buffer in task 9 will
  // turn the first into a "store-and-replay" instead. We log at
  // `console.warn` so dev builds still see the trace but production
  // stays quiet.
  const swallowSendError = useCallback((err) => {
    // eslint-disable-next-line no-console
    console.warn('proctoring sendEvent rejected', err);
  }, []);

  // ── On-event screenshot capture (shared) ────────────────────────
  // Captures a JPEG from a given video element and uploads it bound to
  // the WS-acknowledged event_id. Used by both the webcam path
  // (face events) and the screen-capture path (tab switch / fullscreen).
  const captureAndUpload = useCallback(async (eventId, sourceEl) => {
    if (!sessionId || eventId == null) return;
    if (typeof eventId !== 'number') return;
    if (!sourceEl || !sourceEl.videoWidth || !sourceEl.videoHeight) return;

    let canvas = screenshotCanvasRef.current;
    if (!canvas) {
      canvas = document.createElement('canvas');
      screenshotCanvasRef.current = canvas;
    }
    canvas.width = sourceEl.videoWidth;
    canvas.height = sourceEl.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    try {
      ctx.drawImage(sourceEl, 0, 0, canvas.width, canvas.height);
    } catch {
      return;
    }

    const blob = await new Promise((resolve) => {
      canvas.toBlob((b) => resolve(b), 'image/jpeg', SCREENSHOT_JPEG_QUALITY);
    });
    if (!blob) return;

    const file = new File([blob], `${eventId}.jpg`, { type: 'image/jpeg' });
    const fd = new FormData();
    fd.append('session_id', String(sessionId));
    fd.append('event_id', String(eventId));
    fd.append('captured_at', new Date().toISOString().replace('Z', ''));
    fd.append('file', file, `${eventId}.jpg`);
    try {
      await proctoringApi.uploadScreenshot(fd);
    } catch (err) {
      // eslint-disable-next-line no-console
      console.warn('proctoring screenshot upload failed', err);
    }
  }, [sessionId]);

  // Convenience wrappers that read the correct ref at call time.
  const captureFromWebcam = useCallback((eventId) => {
    return captureAndUpload(eventId, videoRef.current);
  }, [captureAndUpload]);

  const captureFromScreen = useCallback((eventId) => {
    // Wait 1s so the screen-capture stream renders the new app/desktop
    // that the candidate switched to. Without this delay, the canvas
    // draws the stale previous frame.
    return new Promise((resolve) => setTimeout(resolve, 1000))
      .then(() => captureAndUpload(eventId, screenVideoRef.current));
  }, [captureAndUpload]);

  // ── Tab / focus monitor ─────────────────────────────────────────
  // TAB_SWITCH → send event + capture screen screenshot.
  // WINDOW_BLUR → send event only.
  // FOCUS_RESTORED → send event only.
  const handleFocusEvent = useCallback(({ type, payload }) => {
    sendEvent(type, payload ?? {})
      .then((eventId) => {
        if (type === 'TAB_SWITCH') {
          captureFromScreen(eventId);
        }
      })
      .catch(swallowSendError);
  }, [sendEvent, swallowSendError, captureFromScreen]);

  useTabFocusMonitor({
    onEvent: handleFocusEvent,
    onHiddenChange: setIsHidden,
    enabled: Boolean(session),
  });

  // ── Copy / paste / context-menu blocker ─────────────────────────
  // ALL three are silently blocked everywhere including Monaco editor
  // (blockEditor=true). Right-click is disabled. Ctrl+C/V/X/Z are
  // suppressed both at the DOM level (this hook, capture phase) and
  // inside Monaco (onMount keybinding overrides in ProblemSolveEmbed).
  // No score events, no WS frames — purely enforcement.
  useCopyPasteBlocker({
    rootRef: arenaRef,
    enabled: Boolean(session),
    reportEvents: false,
    blockEditor: true,
  });

  // ── Fullscreen exit handler (wired to FullscreenGuard) ──────────
  // When the user Alt+Tabs out of fullscreen, the browser may or may
  // not fire visibilitychange — but the fullscreen exit itself IS the
  // reliable signal. We send TAB_SWITCH IMMEDIATELY (sync, before any
  // async canvas ops) so the event always reaches the server even if
  // the tab goes into the background and throttles JS execution.
  // Screenshot capture follows as a fire-and-forget best-effort path.
  const handleFullscreenExit = useCallback(() => {
    // Fire TAB_SWITCH synchronously — must happen BEFORE any async
    // canvas operations. In a background tab, canvas.toBlob callbacks
    // are throttled or never fire, so the event would be lost if we
    // put sendEvent inside the callback.
    sendEvent('TAB_SWITCH', {}).catch(swallowSendError);

    // Best-effort screenshot: capture what's on screen before the tab
    // fully loses focus. This is fire-and-forget — if the tab is
    // already backgrounded, canvas ops may silently fail; that's
    // acceptable because the event already reached the server above.
    requestAnimationFrame(() => {
      const sourceEl = screenVideoRef.current;
      if (!sourceEl || !sourceEl.videoWidth) return;

      let canvas = screenshotCanvasRef.current;
      if (!canvas) {
        canvas = document.createElement('canvas');
        screenshotCanvasRef.current = canvas;
      }
      canvas.width = sourceEl.videoWidth;
      canvas.height = sourceEl.videoHeight;
      const ctx = canvas.getContext('2d');
      try { ctx.drawImage(sourceEl, 0, 0, canvas.width, canvas.height); } catch { return; }

      canvas.toBlob((blob) => {
        if (!blob) return;
        const file = new File([blob], 'ts-' + Date.now() + '.jpg', { type: 'image/jpeg' });
        const fd = new FormData();
        fd.append('session_id', String(sessionId));
        fd.append('event_id', '');
        fd.append('captured_at', new Date().toISOString().replace('Z', ''));
        fd.append('file', file, 'tab-switch.jpg');
        proctoringApi.uploadScreenshot(fd).catch(() => {});
      }, 'image/jpeg', SCREENSHOT_JPEG_QUALITY);
    });
  }, [sendEvent, swallowSendError, sessionId]);

  // ── Camera lifecycle ────────────────────────────────────────────
  // Acquire a 320×240 video-only MediaStream on mount and bind it to
  // the corner-pip preview. The same stream feeds `useFaceDetector`
  // (which reads from `videoRef`) and the on-event screenshot capture
  // path below — Req 19 explicitly forbids continuous video upload,
  // so the stream stays in-browser and a JPEG is only written on
  // `NO_FACE` / `MULTIPLE_FACES`.
  //
  // `cameraStatus` drives the blocking modal: while it is anything
  // other than `'ready'` the arena UI is dimmed and the candidate is
  // asked to grant permission / replug the webcam (Req 7.6, Req 3.2).
  // We also forward the matching Suspicious_Event over the WS so the
  // server records the camera failure even before any face inference
  // could fire.
  //
  //   cameraStatus ∈ 'idle' | 'requesting' | 'ready' |
  //                  'denied' | 'lost' | 'unsupported'
  const [cameraStatus, setCameraStatus] = useState('idle');

  useEffect(() => {
    if (!session) return undefined;
    if (
      typeof navigator === 'undefined' ||
      !navigator.mediaDevices ||
      typeof navigator.mediaDevices.getUserMedia !== 'function'
    ) {
      setCameraStatus('unsupported');
      sendEvent(EVENT_TYPES.WEBCAM_PERMISSION_DENIED, {
        reason: 'getUserMedia is not available in this browser',
      }).catch(swallowSendError);
      return undefined;
    }

    let cancelled = false;
    let acquiredStream = null;
    let endedHandlers = [];

    const onTrackEnded = () => {
      if (cancelled) return;
      setCameraStatus('lost');
      sendEvent(EVENT_TYPES.WEBCAM_STREAM_LOST, {}).catch(swallowSendError);
    };

    const acquire = async () => {
      setCameraStatus('requesting');
      try {
        const stream = await navigator.mediaDevices.getUserMedia(CAMERA_CONSTRAINTS);
        if (cancelled) {
          // Component unmounted during the permission prompt — release
          // tracks immediately so the camera light goes off.
          stream.getTracks().forEach((t) => { try { t.stop(); } catch { /* ignored */ } });
          return;
        }
        acquiredStream = stream;
        const videoEl = videoRef.current;
        if (videoEl) {
          videoEl.srcObject = stream;
          // `play()` returns a promise; ignore rejections (autoplay
          // policies are forgiving for muted streams but Safari can
          // still throw before the first user gesture).
          const playPromise = videoEl.play();
          if (playPromise && typeof playPromise.catch === 'function') {
            playPromise.catch(() => { /* ignored — autoplay blocked, frames still flow */ });
          }
        }
        // Listen for track-ended so unplugging the webcam mid-session
        // emits WEBCAM_STREAM_LOST and surfaces the blocking modal.
        endedHandlers = stream.getTracks().map((track) => {
          const handler = () => onTrackEnded();
          track.addEventListener('ended', handler);
          return { track, handler };
        });
        setCameraStatus('ready');
      } catch (err) {
        if (cancelled) return;
        setCameraStatus('denied');
        sendEvent(EVENT_TYPES.WEBCAM_PERMISSION_DENIED, {
          name: err?.name ?? 'UnknownError',
        }).catch(swallowSendError);
      }
    };

    acquire();

    return () => {
      cancelled = true;
      endedHandlers.forEach(({ track, handler }) => {
        try { track.removeEventListener('ended', handler); } catch { /* ignored */ }
      });
      endedHandlers = [];
      const videoEl = videoRef.current;
      if (videoEl) {
        try { videoEl.pause(); } catch { /* ignored */ }
        videoEl.srcObject = null;
      }
      if (acquiredStream) {
        acquiredStream.getTracks().forEach((t) => {
          try { t.stop(); } catch { /* ignored */ }
        });
        acquiredStream = null;
      }
    };
    // We intentionally only re-run on `session` changes — `sendEvent`
    // and `swallowSendError` are stable refs from `useCallback`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session]);

  // ── Screen-capture stream ───────────────────────────────────────
  // The screen stream is acquired on the Entry page (inside the user
  // gesture) and passed via a module-level holder. The Arena picks it
  // up and binds it to a hidden <video> element for on-demand frame
  // capture during TAB_SWITCH / FULLSCREEN_EXIT events.
  useEffect(() => {
    if (!session) return undefined;
    let cancelled = false;

    // Small delay so the Entry page has time to set the stream.
    const timer = setTimeout(() => {
      if (cancelled) return;
      const stream = window.__proctoringScreenStream;
      if (!stream) return;
      window.__proctoringScreenStream = null;

      const svEl = screenVideoRef.current;
      if (svEl) {
        svEl.srcObject = stream;
        svEl.play().catch(() => { /* autoplay */ });
      }

      stream.getTracks().forEach((track) => {
        track.addEventListener('ended', () => {
          if (screenVideoRef.current) {
            screenVideoRef.current.srcObject = null;
          }
        });
      });
    }, 500);

    return () => {
      cancelled = true;
      clearTimeout(timer);
      const svEl = screenVideoRef.current;
      if (svEl) {
        try { svEl.pause(); } catch { /* */ }
        svEl.srcObject = null;
      }
    };
  }, [session]);

  // ── Face-detector event handler ─────────────────────────────────
  // Reduces the hook's `{ type, faceCount }` callback into the matching
  // Suspicious_Event over the WS, then — for `NO_FACE` / `MULTIPLE_FACES`
  // — captures and uploads a screenshot bound to the resolved
  // `event_id`. `FACE_PRESENT_OK` is forwarded but does not trigger a
  // capture (it is informational; Req 7.5 maps the on-wire event to
  // `FACE_STATE_RESTORED`).
  //
  // Local mirror of `faceCount` so the corner pill animates without
  // waiting for a state-machine transition.
  const [faceCount, setFaceCount] = useState(1);

  const handleFaceEvent = useCallback(({ type, faceCount: count }) => {
    setFaceCount(count);
    if (type === 'NO_FACE') {
      sendEvent(EVENT_TYPES.NO_FACE, { faceCount: 0 })
        .then((eventId) => captureFromWebcam(eventId))
        .catch(swallowSendError);
      return;
    }
    if (type === 'MULTIPLE_FACES') {
      sendEvent(EVENT_TYPES.MULTIPLE_FACES, { faceCount: count })
        .then((eventId) => captureFromWebcam(eventId))
        .catch(swallowSendError);
      return;
    }
    if (type === 'FACE_PRESENT_OK') {
      sendEvent(EVENT_TYPES.FACE_STATE_RESTORED, { faceCount: 1 })
        .catch(swallowSendError);
    }
  }, [sendEvent, captureFromWebcam, swallowSendError]);

  useFaceDetector({
    videoRef,
    onFaceEvent: handleFaceEvent,
    intervalMs: FACE_INFERENCE_INTERVAL_MS,
  });

  // Whether the camera-blocking modal should be shown. `'requesting'`
  // briefly during the permission prompt is also blocking so the
  // candidate sees the explanatory copy while the browser dialog is
  // up. `'ready'` clears the modal.
  const cameraBlocked = cameraStatus !== 'ready';
  const cameraBlockText = (() => {
    switch (cameraStatus) {
      case 'denied':
        return 'Camera permission was denied. Re-enable webcam access in your browser settings, then reload this page to resume the contest.';
      case 'lost':
        return 'The webcam stream stopped. Reconnect or replug the camera, then reload this page to resume the contest.';
      case 'unsupported':
        return 'This browser does not expose a camera API. Switch to a recent Chrome / Firefox / Edge build to take a proctored contest.';
      case 'requesting':
        return 'Allow camera access in your browser to start the proctored contest. The stream stays in your browser — only on-event screenshots are uploaded.';
      default:
        return 'Initialising webcam…';
    }
  })();

  // ── Connection chip computed values ─────────────────────────────
  const statusMeta = STATUS_LOOKUP[wsStatus] ?? STATUS_LOOKUP.connecting;
  const bandColor = BAND_COLOR[band] ?? C.muted;
  const chipBorder = useMemo(() => (
    band === 'HIGH' ? `1px solid ${C.error}`
    : band === 'MEDIUM' ? `1px solid ${C.warning}`
    : `1px solid ${C.border}`
  ), [band]);

  // ── Contest problem list ────────────────────────────────────────
  // Fetched once on mount. The shape mirrors `/api/problems/contest/{id}`
  // (a `ProblemDTO[]`), which gives us {id, title, level, timeLimit,
  // memoryLimit, …} per problem — everything the left rail needs to
  // render its cards. Fetch errors and empty contests both fall through
  // to the "No problems available" state in the right pane (Req 10);
  // the proctoring shell stays active either way.
  const [problems, setProblems] = useState([]);
  const [problemsLoading, setProblemsLoading] = useState(true);
  const [problemsError, setProblemsError] = useState(false);
  const [activeProblemId, setActiveProblemId] = useState(null);

  // Per-problem submission summary, keyed by problem id. Populated from
  // `/api/submissions/user` so the rail badges reflect "✓ submitted"
  // state; refreshed after a successful submit via the embed callback.
  const [problemSubs, setProblemSubs] = useState({});

  const refreshUserSubmissions = useCallback(() => {
    api.get('/submissions/user')
      .then(res => {
        const map = {};
        (res.data || []).forEach(sub => {
          if (sub.problemId && (!map[sub.problemId] || new Date(sub.submittedAt) > new Date(map[sub.problemId].submittedAt))) {
            map[sub.problemId] = sub;
          }
        });
        setProblemSubs(map);
      })
      .catch(() => { /* ignored — rail badges degrade gracefully */ });
  }, []);

  useEffect(() => {
    if (!session || !id) return;
    let cancelled = false;
    setProblemsLoading(true);
    setProblemsError(false);
    api.get(`/problems/contest/${id}`)
      .then(res => {
        if (cancelled) return;
        const list = Array.isArray(res.data) ? res.data : [];
        setProblems(list);
        setProblemsLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setProblems([]);
        setProblemsError(true);
        setProblemsLoading(false);
      });
    refreshUserSubmissions();
    return () => { cancelled = true; };
  }, [session, id, refreshUserSubmissions]);

  // ── URL hash ↔ active problem sync ──────────────────────────────
  // The active problem id is mirrored in `location.hash` (`#problem=N`)
  // so a refresh keeps the candidate on the same problem. We default
  // to the first problem if the hash is absent or refers to an id that
  // isn't part of this contest's list (e.g. cross-contest paste).
  useEffect(() => {
    if (problemsLoading) return;
    if (problems.length === 0) {
      setActiveProblemId(null);
      return;
    }
    const hashMatch = (window.location.hash || '').match(/problem=(\d+)/);
    const hashId = hashMatch ? Number(hashMatch[1]) : NaN;
    const hashHit = problems.find(p => p.id === hashId);
    const fallback = problems[0]?.id ?? null;
    setActiveProblemId(hashHit ? hashHit.id : fallback);
  }, [problems, problemsLoading]);

  // Push hash whenever the active problem changes. `replace` keeps the
  // browser history clean — switching problems should not pollute the
  // back-stack with intermediate entries.
  useEffect(() => {
    if (activeProblemId == null) return;
    const next = `#problem=${activeProblemId}`;
    if (window.location.hash !== next) {
      const url = `${window.location.pathname}${window.location.search}${next}`;
      window.history.replaceState(null, '', url);
    }
  }, [activeProblemId]);

  // React to manual hash edits (e.g. paste a refreshed link). Browser
  // emits `hashchange` on direct navigation but not on programmatic
  // replaceState — that's fine, our own writes don't need to round-trip.
  useEffect(() => {
    const onHashChange = () => {
      const m = (window.location.hash || '').match(/problem=(\d+)/);
      const next = m ? Number(m[1]) : NaN;
      if (problems.find(p => p.id === next)) {
        setActiveProblemId(next);
      }
    };
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [problems]);

  const onSelectProblem = useCallback((pid) => {
    setActiveProblemId(pid);
  }, []);

  const onProblemSubmissionComplete = useCallback(() => {
    refreshUserSubmissions();
  }, [refreshUserSubmissions]);

  // Bounce back to entry if route state is missing. Done AFTER hooks
  // so React's hook-order rule is preserved.
  if (!session) {
    return <Navigate to={`/contests/${id}/proctored/entry`} replace />;
  }

  return (
    <div ref={arenaRef} style={styles.root}>
      <FullscreenGuard
        targetRef={arenaRef}
        onFullscreenExit={handleFullscreenExit}
      />

      {/* Connection-loss guard (Req 11.5, Req 24). Pauses the arena
          and shows a countdown the moment the proctoring WebSocket
          goes silent (combined "open + recent server frame" signal
          from useProctoringSocket). When the offline budget expires
          the arena terminates the session as HEARTBEAT_TIMEOUT. */}
      <DisconnectionGuard
        connected={connected}
        onTimeoutExceeded={onOfflineTimeoutExceeded}
        maxOfflineSeconds={MAX_OFFLINE_SECONDS}
      />

      {/* Connection / risk chip */}
      <div style={{ ...styles.chip, border: chipBorder }} aria-live="polite">
        <div style={styles.chipRow}>
          <span>
            <span style={{ ...styles.chipDot, backgroundColor: statusMeta.color }} />
            <span style={styles.chipValue}>{statusMeta.label}</span>
          </span>
          <span style={styles.chipLabel}>WS</span>
        </div>
        <div style={styles.chipRow}>
          <span style={styles.chipLabel}>Band</span>
          <span style={{ ...styles.chipValue, color: bandColor }}>{band ?? 'LOW'}</span>
        </div>
        <div style={styles.chipRow}>
          <span style={styles.chipLabel}>Score</span>
          <span style={styles.chipValue}>{Number.isFinite(score) ? score : 0}</span>
        </div>
      </div>

      <div style={styles.topBar}>
        <p style={styles.brand}>Proctored Contest · Arena</p>
        <div style={styles.topBarRight}>
          <span style={styles.contestId}>Contest #{id}</span>
          <FinishQuitButtons
            onFinish={handleFinish}
            onQuit={handleQuit}
            disabled={terminating}
          />
        </div>
      </div>

      <div style={styles.body}>
        {/* Sticky proctoring banner — Req: candidate is always reminded
            activity is recorded. Sits above the 2-column workspace and
            stays sticky as the right pane scrolls. */}
        <div style={styles.banner} role="status">
          <div style={styles.bannerLeft}>
            <span style={styles.bannerDot} aria-hidden="true" />
            <span style={styles.bannerLabel}>Proctored Contest</span>
            <span style={styles.bannerCopy}>· all activity is being monitored</span>
          </div>
          <div style={styles.bannerScore}>
            risk score <span style={{ color: bandColor, marginLeft: '4px' }}>
              {Number.isFinite(score) ? score : 0}
            </span>
          </div>
        </div>

        {/* 2-column workspace: rail + embedded problem-solve UI */}
        <div style={styles.workspace}>
          <aside style={styles.rail} aria-label="Contest problems">
            <div style={styles.railHeader}>
              {problemsLoading
                ? 'Loading problems…'
                : `${problems.length} ${problems.length === 1 ? 'Problem' : 'Problems'}`}
            </div>
            {!problemsLoading && problems.length > 0 && (
              <div style={styles.railList}>
                {problems.map((p, idx) => {
                  const letter = String.fromCharCode(65 + idx);
                  const active = p.id === activeProblemId;
                  const diff = DIFF_CFG[p.level] || { color: C.outline, label: p.level || '—' };
                  const sub = problemSubs[p.id];
                  const solved = sub && sub.status === 'AC';
                  return (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => onSelectProblem(p.id)}
                      style={{
                        ...styles.problemCard,
                        ...(active ? styles.problemCardActive : null),
                      }}
                      onMouseEnter={(e) => {
                        if (!active) {
                          e.currentTarget.style.backgroundColor = C.surfaceHi;
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!active) {
                          e.currentTarget.style.backgroundColor = 'transparent';
                        }
                      }}
                      aria-pressed={active}
                    >
                      <span
                        style={{
                          ...styles.letterCircle,
                          ...(active ? styles.letterCircleActive : null),
                        }}
                      >
                        {letter}
                      </span>
                      <span style={styles.problemMeta}>
                        <span style={styles.problemTitle} title={p.title}>
                          {p.title}
                        </span>
                        <span style={styles.problemSub}>
                          <span style={{ ...styles.diffBadge, color: diff.color, borderColor: diff.color }}>
                            {diff.label}
                          </span>
                          <span style={styles.problemLimits}>
                            {p.timeLimit}s · {p.memoryLimit}MB
                          </span>
                          {solved && (
                            <span
                              className="material-symbols-outlined"
                              style={{ fontSize: '14px', color: C.success, fontVariationSettings: "'FILL' 1" }}
                              aria-label="Solved"
                            >
                              check_circle
                            </span>
                          )}
                        </span>
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
            {!problemsLoading && problems.length === 0 && (
              <div style={styles.railEmpty}>
                {problemsError
                  ? 'Could not load problems. Refresh to retry.'
                  : 'No problems available.'}
              </div>
            )}
          </aside>

          <section style={styles.rightPane} aria-label="Problem workspace">
            {activeProblemId != null ? (
              <ProblemSolveEmbed
                key={activeProblemId}
                problemId={activeProblemId}
                onSubmissionComplete={onProblemSubmissionComplete}
                proctored
              />
            ) : (
              <div style={styles.emptyRight}>
                <div style={styles.emptyCard}>
                  <h2 style={styles.emptyHeading}>No problems available in this contest</h2>
                  <p style={styles.emptyCopy}>
                    {problemsError
                      ? 'We couldn\u2019t reach the problem service. Try refreshing the page \u2014 your proctoring session is still active.'
                      : 'The contest organiser hasn\u2019t attached any problems yet. Sit tight; the proctoring shell is still active.'}
                  </p>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>

      {/* Hidden-tab pause overlay (Req 5.5). The placeholder UI does
          not have an editor or submit button yet, but rendering the
          overlay now makes the read-only intent visible end-to-end. */}
      {isHidden && (
        <div style={styles.hiddenOverlay}>
          contest paused — return to this tab to continue
        </div>
      )}

      {/* Webcam corner-pip preview + face-status pill. Stays at a low
          z-index so the disconnection guard / camera-block modal
          layer above it. The video element is ALWAYS mounted so
          `videoRef` is available for `useFaceDetector` and
          screenshot capture even before the stream is acquired —
          a placeholder is overlaid only while the camera is not
          yet ready. */}
      <div style={styles.webcamWrap} aria-hidden="true">
        <video
          ref={videoRef}
          style={{
            ...styles.webcamVideo,
            display: cameraStatus === 'ready' ? 'block' : 'none',
          }}
          autoPlay
          muted
          playsInline
        />
        {cameraStatus !== 'ready' && (
          <div style={styles.webcamPlaceholder}>
            {cameraStatus === 'requesting' ? 'requesting camera…' : 'no camera'}
          </div>
        )}
        <div style={styles.facePillRow}>
          <span style={styles.facePillLabel}>FACE</span>
          <span
            style={{ ...styles.facePillValue, color: faceMeta(faceCount).color }}
          >
            {faceMeta(faceCount).label}
          </span>
        </div>
      </div>

      {/* Hidden screen-capture video element. The stream is acquired via
          getDisplayMedia on mount — no video is stored or transmitted,
          only single JPEG frames are captured on TAB_SWITCH /
          FULLSCREEN_EXIT events. Hidden from view; exists only as a
          drawImage source for canvas. */}
      <video
        ref={screenVideoRef}
        autoPlay muted playsInline
        style={{ position: 'fixed', top: -9999, left: -9999, width: 1, height: 1, opacity: 0, pointerEvents: 'none' }}
        aria-hidden="true"
      />

      {/* Camera-blocking modal (Req 7.6, Req 3.2). Rendered while the
          camera is not yet ready — denied permission, lost stream,
          unsupported browser, or pending prompt. Sits above every
          other overlay so the contest UI is visually paused. */}
      {cameraBlocked && (
        <div style={styles.cameraBlockOverlay} role="dialog" aria-modal="true">
          <div style={styles.cameraBlockCard}>
            <h2 style={styles.cameraBlockHeading}>Webcam required</h2>
            <p style={styles.cameraBlockBody}>{cameraBlockText}</p>
            {(cameraStatus === 'denied' || cameraStatus === 'lost') && (
              <button
                type="button"
                style={styles.cameraBlockButton}
                onClick={() => window.location.reload()}
              >
                Reload page
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
