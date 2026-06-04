// ─── face detector plugin ──────────────────────────────────────────────────
//
// MVP face-detection plugin running MediaPipe Tasks on the main thread
// (per design.md §"Worker Offload" — worker offload is optional, the main
// thread budget is comfortably under one inference-per-second of CPU).
//
// Implements the lightweight plugin contract called out in tasks.md task
// 8.4 so future detectors (mobile phone, gaze, audio, object) can slot in
// without changing the inference loop, the WebSocket transport, or the
// server-side risk engine (Req 7.7, 22.1):
//
//   {
//     id,                 // stable string identifier ('face', …)
//     init(videoEl, cfg)  // one-time async setup; resolves when ready
//     step()              // run inference once, return Array<{ type, payload }>
//     dispose()           // async cleanup on session end
//   }
//
// `step()` returns raw frame observations rather than semantic events; the
// stateful reduction (single→none→fired with debounce, etc.) lives in
// `useFaceDetector.js` so all detector plugins stay stateless and the
// transition logic can be unit-tested in isolation.
//
// MediaPipe Tasks (`@mediapipe/tasks-vision`) is **dynamically imported**
// the first time `init()` runs so the ~600 KB gzipped chunk + the 2 MB
// wasm runtime + 3 MB face model are loaded only when the proctored arena
// actually mounts. Non-proctored bundles stay completely untouched
// (design.md §"Bundle Footprint").
//
// Validates: Requirements 7.1, 7.2, 7.7, 22.1, 22.2

const WASM_PATH = '/wasm';
const MODEL_PATH = '/models/blaze_face_short_range.tflite';

/**
 * Build a fresh face-detector plugin instance. The factory shape (rather
 * than a class) keeps the consumer ergonomics symmetric across plugins:
 *
 *   const face = createFaceDetector();
 *   await face.init(videoEl);
 *   for (;;) { const events = await face.step(); … }
 *   await face.dispose();
 *
 * @returns {{
 *   id: string,
 *   init: (videoEl: HTMLVideoElement, config?: object) => Promise<void>,
 *   step: () => Promise<Array<{type: string, payload: object}>>,
 *   dispose: () => Promise<void>,
 * }}
 */
export function createFaceDetector() {
  let detector = null;
  let videoEl = null;
  let disposed = false;
  let lastTimestampMs = 0;

  async function init(targetVideoEl /* , config */) {
    if (disposed) {
      throw new Error('face detector already disposed');
    }
    if (!targetVideoEl) {
      throw new Error('face detector requires a <video> element');
    }
    videoEl = targetVideoEl;

    // Lazy-import keeps the MediaPipe chunk out of the main bundle.
    // Vite splits this into its own chunk because of the dynamic import.
    const { FilesetResolver, FaceDetector } = await import(
      '@mediapipe/tasks-vision'
    );

    const fileset = await FilesetResolver.forVisionTasks(WASM_PATH);
    detector = await FaceDetector.createFromOptions(fileset, {
      baseOptions: { modelAssetPath: MODEL_PATH },
      // Use VIDEO mode so we can pass the live <video> element directly
      // and let MediaPipe pull the latest decoded frame internally — no
      // extra `OffscreenCanvas`/`drawImage` round-trip on the main thread.
      runningMode: 'VIDEO',
    });
  }

  async function step() {
    if (disposed || !detector || !videoEl) return [];

    // Skip frames before the <video> has actual pixel data; otherwise
    // MediaPipe throws on a zero-sized texture.
    if (
      videoEl.readyState < 2 /* HAVE_CURRENT_DATA */ ||
      !videoEl.videoWidth ||
      !videoEl.videoHeight
    ) {
      return [];
    }

    // MediaPipe requires strictly monotonic timestamps in VIDEO mode.
    // Use `performance.now()` rounded to ms and bump on collision.
    let ts = Math.floor(performance.now());
    if (ts <= lastTimestampMs) ts = lastTimestampMs + 1;
    lastTimestampMs = ts;

    let result;
    try {
      result = detector.detectForVideo(videoEl, ts);
    } catch (err) {
      // Surface the failure as a synthetic event so the hook can decide
      // whether to escalate (e.g. WEBCAM_STREAM_LOST). Inference errors
      // are common during the first few frames after a tab returns from
      // background, so we don't tear the detector down here.
      return [
        {
          type: 'FACE_INFERENCE_ERROR',
          payload: { message: err && err.message ? err.message : 'detect_failed' },
        },
      ];
    }

    const detections = (result && result.detections) || [];
    const faceCount = detections.length;
    const confidence =
      detections[0]?.categories?.[0]?.score != null
        ? Number(detections[0].categories[0].score)
        : null;

    return [
      {
        type: 'FACE_OBSERVATION',
        payload: { faceCount, confidence },
      },
    ];
  }

  async function dispose() {
    if (disposed) return;
    disposed = true;
    const d = detector;
    detector = null;
    videoEl = null;
    if (d && typeof d.close === 'function') {
      try {
        d.close();
      } catch {
        // best-effort cleanup; nothing to do if the runtime is already gone
      }
    }
  }

  return { id: 'face', init, step, dispose };
}

export default createFaceDetector;
