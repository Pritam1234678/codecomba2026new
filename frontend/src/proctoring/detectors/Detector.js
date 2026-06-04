/**
 * Detector — base class for all browser-side proctoring signal detectors.
 *
 * The detector plugin interface is intentionally minimal so future detectors
 * (mobile phone, gaze, audio, object detection) can slot in without changing
 * the inference loop, the WebSocket transport, or the server-side risk engine.
 *
 * Lifecycle:
 *   1. `init(config)`  — called once after the registry is built. Loads any
 *      model files, warms the worker, opens device handles, etc.
 *   2. `process(imageBitmap)` — called on every inference tick (default 1 Hz)
 *      with the latest frame. Returns an array of
 *      `{ event_type, payload }` observations the upstream hook will forward
 *      over the WebSocket.
 *   3. `dispose()` — called once on session end for cleanup.
 *
 * All three methods are async so the loop can `await` worker round-trips.
 *
 * Validates: Requirements 7.7, 22.1, 22.2
 */
export class Detector {
  /** Async one-time init. Loads model files, warms the worker. */
  // eslint-disable-next-line no-unused-vars
  async init(config) {}

  /**
   * Called once per inference tick with the latest frame.
   * @param {ImageBitmap} imageBitmap
   * @returns {Promise<Array<{event_type: string, payload: object}>>}
   */
  // eslint-disable-next-line no-unused-vars
  async process(imageBitmap) {
    return [];
  }

  /** Async cleanup on session end. */
  async dispose() {}
}
