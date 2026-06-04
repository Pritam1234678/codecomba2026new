// Web Worker that runs MediaPipe Tasks `FaceDetector` against
// transferred ImageBitmap frames. Lives off the main thread so the
// Monaco editor and React reconciliation are unaffected.
//
// Protocol (matches design.md):
//   { type: 'init' }                       -> { type: 'ready' }
//   { type: 'frame', bitmap, frameId }     -> { type: 'result', frameId, faceCount, confidence }
//   any failure                            -> { type: 'error', message }
//
// The worker always calls `bitmap.close()` after handling a frame,
// even on error, so the main thread's transferred ImageBitmap is
// reliably released.

import { FilesetResolver, FaceDetector } from '@mediapipe/tasks-vision';

let detector = null;

self.addEventListener('message', async (e) => {
  const data = e.data;

  if (!data || typeof data.type !== 'string') {
    return;
  }

  if (data.type === 'init') {
    try {
      const fileset = await FilesetResolver.forVisionTasks('/wasm/');
      detector = await FaceDetector.createFromOptions(fileset, {
        baseOptions: {
          modelAssetPath: '/models/blaze_face_short_range.tflite',
        },
        runningMode: 'IMAGE',
      });
      self.postMessage({ type: 'ready' });
    } catch (err) {
      detector = null;
      self.postMessage({
        type: 'error',
        message: err && err.message ? err.message : 'init_failed',
      });
    }
    return;
  }

  if (data.type === 'frame') {
    const { bitmap, frameId } = data;
    try {
      if (!detector) {
        throw new Error('detector_not_initialised');
      }
      const result = detector.detect(bitmap);
      const detections = (result && result.detections) || [];
      const faceCount = detections.length;
      const confidence =
        detections[0]?.categories?.[0]?.score ?? null;
      self.postMessage({
        type: 'result',
        frameId,
        faceCount,
        confidence,
      });
    } catch (err) {
      self.postMessage({
        type: 'error',
        frameId,
        message: err && err.message ? err.message : 'detect_failed',
      });
    } finally {
      // Always release the transferred bitmap, even on error,
      // so the main thread doesn't leak GPU/CPU memory.
      try {
        if (bitmap && typeof bitmap.close === 'function') {
          bitmap.close();
        }
      } catch {
        // ignore: nothing to do if close itself throws
      }
    }
    return;
  }
});
