import { useEffect, useRef, useState } from 'react';
import { createFaceDetector } from '../detectors/face';

// ─── useFaceDetector ────────────────────────────────────────────────────────
//
// Drives the MediaPipe face-detector plugin against a live `<video>`
// element on a fixed inference interval and reduces the raw
// `FACE_OBSERVATION` stream into the discrete suspicious events that the
// proctoring WebSocket cares about. All transition logic lives here so
// detector plugins stay stateless and reusable (Req 7.7).
//
// Public contract
// ───────────────
// Inputs:
//   videoRef     React ref pointing at a `<video>` element with the live
//                MediaStream attached (camera).
//   onFaceEvent  Callback invoked once per state transition into a
//                FIRED state, and once per return to FACE_PRESENT_OK.
//                Shape: `{ type, faceCount }`.
//                  • `type='NO_FACE'`         — 3+ s of zero faces
//                  • `type='MULTIPLE_FACES'`  — 2+ s of two-or-more faces
//                  • `type='FACE_PRESENT_OK'` — informational restore
//   intervalMs   Inference cadence in ms; default 1000 (Req 7.1).
//
// Returns:
//   { status, faceCount, lastInferenceAt }
//     status ∈ 'idle' | 'loading' | 'ready' | 'error'
//
// State machine (per design):
//   FACE_PRESENT_OK         (1 face)
//   FACE_ABSENT_PENDING     (0 faces, < NO_FACE_THRESHOLD_MS)
//   FACE_ABSENT_FIRED       (0 faces ≥ NO_FACE_THRESHOLD_MS — emit NO_FACE)
//   MULTIPLE_FACES_PENDING  (≥2 faces, < MULTI_FACE_THRESHOLD_MS)
//   MULTIPLE_FACES_FIRED    (≥2 faces ≥ MULTI_FACE_THRESHOLD_MS — emit MULTIPLE_FACES)
//
// Transitions are debounced by *time elapsed in the pending state*, not by
// frame count, so a slow inference loop (browser tab throttled, etc.)
// still fires at the right wall-clock moment. The hook re-arms — i.e. is
// ready to fire again — only after the candidate returns to
// FACE_PRESENT_OK and a fresh `FACE_PRESENT_OK` info event has been
// emitted.
//
// Validates: Requirements 5 (face detection cadence), 7.1, 7.2, 7.3, 7.4,
// 7.5, 7.7, 19 (event-driven only, no continuous video upload)

const STATE_PRESENT = 'FACE_PRESENT_OK';
const STATE_ABSENT_PENDING = 'FACE_ABSENT_PENDING';
const STATE_ABSENT_FIRED = 'FACE_ABSENT_FIRED';
const STATE_MULTI_PENDING = 'MULTIPLE_FACES_PENDING';
const STATE_MULTI_FIRED = 'MULTIPLE_FACES_FIRED';

// Per design.md Req 7.3 / 7.4: NO_FACE fires only after a continuous run
// of zero-face frames ≥ `noFaceThresholdSeconds` (default 5 s — suppresses
// transient occlusions like a hand passing the lens), while MULTIPLE_FACES
// fires IMMEDIATELY on the first frame with ≥2 faces (no debounce — a single
// two-face frame is already suspicious). These match the backend
// `ProctoringConfig` defaults so client- and server-side semantics agree.
const NO_FACE_THRESHOLD_MS = 5000;

export function useFaceDetector({
  videoRef,
  onFaceEvent,
  intervalMs = 1000,
} = {}) {
  const [status, setStatus] = useState('idle');
  const [faceCount, setFaceCount] = useState(0);
  const [lastInferenceAt, setLastInferenceAt] = useState(null);

  // Read callbacks via refs so callers don't need to memoise them — the
  // inference loop must not tear down on every parent re-render.
  const onFaceEventRef = useRef(onFaceEvent);
  useEffect(() => {
    onFaceEventRef.current = onFaceEvent;
  }, [onFaceEvent]);

  useEffect(() => {
    if (!videoRef) {
      setStatus('idle');
      return undefined;
    }

    let cancelled = false;
    let timer = null;
    let inFlight = false;
    const detector = createFaceDetector();

    // Mutable state-machine cursor. Lives in the effect closure so a new
    // mount starts fresh, and so React's render cycle can never observe
    // a half-applied transition.
    let machineState = STATE_PRESENT;
    let pendingSince = 0;

    const emit = (type, count) => {
      const cb = onFaceEventRef.current;
      if (typeof cb !== 'function') return;
      try {
        cb({ type, faceCount: count });
      } catch {
        // Never let a downstream callback error break the inference loop.
      }
    };

    /**
     * Reduce one face-count observation into the state machine.
     * `now` is passed in (rather than calling `Date.now()` again) so the
     * caller can record `lastInferenceAt` and the transition under the
     * same wall-clock instant.
     */
    const reduce = (count, now) => {
      if (count === 1) {
        // A single face is the only way to leave a FIRED or PENDING
        // state. Re-arm by transitioning back to FACE_PRESENT_OK and
        // emit the informational `FACE_PRESENT_OK` event when we were
        // not already there.
        if (machineState !== STATE_PRESENT) {
          machineState = STATE_PRESENT;
          pendingSince = 0;
          emit('FACE_PRESENT_OK', count);
        }
        return;
      }

      if (count === 0) {
        // Two-or-more faces and zero faces are mutually exclusive
        // pending tracks; entering one resets the other's timer.
        if (
          machineState === STATE_PRESENT ||
          machineState === STATE_MULTI_PENDING ||
          machineState === STATE_MULTI_FIRED
        ) {
          machineState = STATE_ABSENT_PENDING;
          pendingSince = now;
          return;
        }
        if (machineState === STATE_ABSENT_PENDING) {
          if (now - pendingSince >= NO_FACE_THRESHOLD_MS) {
            machineState = STATE_ABSENT_FIRED;
            emit('NO_FACE', count);
          }
          return;
        }
        // STATE_ABSENT_FIRED — already fired, do not re-emit.
        return;
      }

      // count >= 2 — MULTIPLE_FACES fires IMMEDIATELY (Req 7.4), no debounce.
      // A single frame with two-or-more faces is already suspicious, so we
      // skip the PENDING state entirely and emit on first observation.
      if (machineState !== STATE_MULTI_FIRED) {
        machineState = STATE_MULTI_FIRED;
        pendingSince = 0;
        emit('MULTIPLE_FACES', count);
      }
      // STATE_MULTI_FIRED — already fired, do not re-emit.
    };

    const tick = async () => {
      if (cancelled || inFlight) return;
      const videoEl = videoRef.current;
      if (!videoEl) return;
      inFlight = true;
      try {
        const observations = await detector.step();
        if (cancelled) return;
        for (const obs of observations) {
          if (obs.type !== 'FACE_OBSERVATION') continue;
          const count = Number(obs.payload?.faceCount ?? 0);
          const now = Date.now();
          setFaceCount(count);
          setLastInferenceAt(now);
          reduce(count, now);
        }
      } catch {
        // A single tick failure shouldn't tear down the loop; the next
        // interval will retry. Persistent failures bubble up via the
        // detector plugin's own error events when added later.
      } finally {
        inFlight = false;
      }
    };

    const start = async () => {
      setStatus('loading');
      const videoEl = videoRef.current;
      if (!videoEl) {
        if (!cancelled) setStatus('error');
        return;
      }
      try {
        await detector.init(videoEl);
        if (cancelled) {
          await detector.dispose();
          return;
        }
        setStatus('ready');
        // Run an immediate first tick so the UI doesn't sit at
        // faceCount=0 for an entire interval window after init.
        tick();
        timer = setInterval(tick, Math.max(100, intervalMs | 0));
      } catch {
        if (!cancelled) setStatus('error');
        try {
          await detector.dispose();
        } catch {
          // best-effort cleanup
        }
      }
    };

    start();

    return () => {
      cancelled = true;
      if (timer != null) {
        clearInterval(timer);
        timer = null;
      }
      // Fire-and-forget dispose; no need to await on unmount.
      detector.dispose().catch(() => {
        // best-effort cleanup
      });
    };
  }, [videoRef, intervalMs]);

  return { status, faceCount, lastInferenceAt };
}

export default useFaceDetector;
