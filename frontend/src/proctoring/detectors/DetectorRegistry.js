/**
 * DetectorRegistry — fan-out container for `Detector` plugins.
 *
 * The registry owns the list of detectors and exposes a single fan-out
 * surface to the inference loop:
 *
 *   - `register(detector)` — add a detector before `initAll`.
 *   - `initAll(config)`    — call `init(config)` on every detector in parallel.
 *   - `processFrame(bitmap)` — run every detector against the same frame and
 *     return the concatenated observations as
 *     `[{ event_type, payload }, …]`.
 *   - `disposeAll()` — call `dispose()` on every detector in parallel.
 *
 * The transport layer (`useProctoringSocket`) is agnostic to which detector
 * produced which event, so adding a new detector is purely a registration
 * concern.
 *
 * Validates: Requirements 7.7, 22.1, 22.2
 */
export class DetectorRegistry {
  constructor() {
    this.detectors = [];
  }

  register(detector) {
    this.detectors.push(detector);
  }

  async initAll(config) {
    return Promise.all(this.detectors.map((d) => d.init(config)));
  }

  async processFrame(bitmap) {
    const events = [];
    for (const d of this.detectors) {
      events.push(...(await d.process(bitmap)));
    }
    return events;
  }

  async disposeAll() {
    return Promise.all(this.detectors.map((d) => d.dispose()));
  }
}
