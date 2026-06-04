import { Detector } from './Detector';

/**
 * FaceDetector — the only MVP detector implementation.
 *
 * Combines the presence and count signals (they share the same model output,
 * so splitting them would double work) and delegates the heavy lifting to
 * `faceDetector.worker.js`, which runs MediaPipe Tasks on a Web Worker.
 *
 * This class is intentionally dumb: it forwards every frame to the worker
 * and returns the raw observation. The state machine that converts the
 * stream of `FACE_OBSERVATION`s into discrete `NO_FACE` / `MULTIPLE_FACES` /
 * `FACE_STATE_RESTORED` events lives in `useFaceDetector` (task 8.4),
 * because state transitions need access to React state and timing primitives.
 *
 * Wire protocol with the worker (see task 8.2):
 *   main → worker: { type: 'init', config }
 *   worker → main: { type: 'ready' }
 *                | { type: 'error', error }
 *   main → worker: { type: 'frame', bitmap, frameId }   (bitmap transferred)
 *   worker → main: { type: 'result', frameId, faceCount, confidence }
 *   main → worker: { type: 'dispose' }
 *
 * Validates: Requirements 7.7, 22.1, 22.2
 */
export class FaceDetector extends Detector {
  /**
   * @param {Worker} worker — instance of `faceDetector.worker.js`.
   */
  constructor(worker) {
    super();
    this.worker = worker;
    this.nextFrameId = 1;
    this.pendingFrames = new Map(); // frameId → { resolve, reject }
    this.readyResolvers = null;
    this.disposed = false;
    this.handleMessage = this.handleMessage.bind(this);
    this.handleError = this.handleError.bind(this);
    if (this.worker) {
      this.worker.addEventListener('message', this.handleMessage);
      this.worker.addEventListener('error', this.handleError);
    }
  }

  handleMessage(event) {
    const msg = event.data;
    if (!msg || typeof msg !== 'object') return;
    if (msg.type === 'ready') {
      if (this.readyResolvers) {
        this.readyResolvers.resolve();
        this.readyResolvers = null;
      }
      return;
    }
    if (msg.type === 'error') {
      const err = new Error(msg.error || 'face detector worker error');
      if (this.readyResolvers) {
        this.readyResolvers.reject(err);
        this.readyResolvers = null;
      }
      // Fail any in-flight frames so the inference loop does not stall.
      for (const [, pending] of this.pendingFrames) {
        pending.reject(err);
      }
      this.pendingFrames.clear();
      return;
    }
    if (msg.type === 'result' && typeof msg.frameId === 'number') {
      const pending = this.pendingFrames.get(msg.frameId);
      if (pending) {
        this.pendingFrames.delete(msg.frameId);
        pending.resolve({
          faceCount: msg.faceCount ?? 0,
          confidence: msg.confidence ?? 0,
        });
      }
    }
  }

  handleError(event) {
    const err = new Error(event?.message || 'face detector worker crashed');
    if (this.readyResolvers) {
      this.readyResolvers.reject(err);
      this.readyResolvers = null;
    }
    for (const [, pending] of this.pendingFrames) {
      pending.reject(err);
    }
    this.pendingFrames.clear();
  }

  async init(config) {
    if (!this.worker) {
      throw new Error('FaceDetector requires a worker instance');
    }
    const ready = new Promise((resolve, reject) => {
      this.readyResolvers = { resolve, reject };
    });
    this.worker.postMessage({ type: 'init', config: config ?? {} });
    await ready;
  }

  async process(imageBitmap) {
    if (this.disposed || !this.worker || !imageBitmap) return [];
    const frameId = this.nextFrameId++;
    const result = await new Promise((resolve, reject) => {
      this.pendingFrames.set(frameId, { resolve, reject });
      try {
        this.worker.postMessage(
          { type: 'frame', bitmap: imageBitmap, frameId },
          [imageBitmap]
        );
      } catch (err) {
        this.pendingFrames.delete(frameId);
        reject(err);
      }
    });
    return [
      {
        event_type: 'FACE_OBSERVATION',
        payload: {
          faceCount: result.faceCount,
          confidence: result.confidence,
        },
      },
    ];
  }

  async dispose() {
    if (this.disposed) return;
    this.disposed = true;
    if (this.worker) {
      try {
        this.worker.postMessage({ type: 'dispose' });
      } catch {
        // Worker may already be terminated; cleanup hook is best-effort.
      }
      this.worker.removeEventListener('message', this.handleMessage);
      this.worker.removeEventListener('error', this.handleError);
    }
    // Reject any in-flight frames so callers stop awaiting.
    const err = new Error('FaceDetector disposed');
    for (const [, pending] of this.pendingFrames) {
      pending.reject(err);
    }
    this.pendingFrames.clear();
  }
}
