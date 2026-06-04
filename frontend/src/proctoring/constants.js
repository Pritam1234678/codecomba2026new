// Practice / Contest palette tokens for the proctoring surface.
//
// Mirrors the literal values inlined in `ContestDetail.jsx`,
// `ContestList.jsx`, `FullscreenGuard.jsx`, and `FinishQuitButtons.jsx`
// so the entire proctoring UI stays visually cohesive with the rest of
// the contest experience. Per design Req 23.8 we stay strictly in this
// palette — no neon greens, no AI-style colors.
export const C = {
  bg:         '#131313',
  surfaceCon: '#201f1f',
  surfaceLow: '#1c1b1b',
  surfaceHi:  '#2a2a2a',
  border:     '#50453b',
  primary:    '#f1bc8b',
  secondary:  '#e9c176',
  muted:      '#d4c4b7',
  outline:    '#9d8e83',
  onBg:       '#e5e2e1',
  error:      '#ffb4ab',
  success:    '#4ade80',
  warning:    '#facc15',
};

// Suspicious_Event types referenced from the candidate frontend. The
// server keeps `event_type` as a free-form `varchar(32)` (Req 22.2) so
// new detector plugins can register types without a schema change; this
// list documents every type the MVP frontend may emit.
export const EVENT_TYPES = Object.freeze({
  TAB_SWITCH:               'TAB_SWITCH',
  WINDOW_BLUR:              'WINDOW_BLUR',
  FOCUS_RESTORED:           'FOCUS_RESTORED',
  FULLSCREEN_EXIT:          'FULLSCREEN_EXIT',
  COPY_ATTEMPT:             'COPY_ATTEMPT',
  CUT_ATTEMPT:              'CUT_ATTEMPT',
  PASTE_ATTEMPT:            'PASTE_ATTEMPT',
  CONTEXT_MENU_ATTEMPT:     'CONTEXT_MENU_ATTEMPT',
  NO_FACE:                  'NO_FACE',
  MULTIPLE_FACES:           'MULTIPLE_FACES',
  FACE_STATE_RESTORED:      'FACE_STATE_RESTORED',
  WEBCAM_PERMISSION_DENIED: 'WEBCAM_PERMISSION_DENIED',
  WEBCAM_STREAM_LOST:       'WEBCAM_STREAM_LOST',
  HEARTBEAT_TIMEOUT:        'HEARTBEAT_TIMEOUT',
  RATE_LIMIT_EXCEEDED:      'RATE_LIMIT_EXCEEDED',
  BUFFER_OVERFLOW:          'BUFFER_OVERFLOW',
});

// WebSocket close codes used by the proctoring channel. Mirrors the
// server-side `CloseStatus` constants in the design spec.
export const WS_CLOSE = Object.freeze({
  ADMIN_FORCED:      4003,
  AUTH_FAILED:       4401,
  HEARTBEAT_TIMEOUT: 4408,
  DUPLICATE:         4409,
  PAYLOAD_TOO_LARGE: 4413,
});

// Minimum desktop viewport width — Req 23.9 gates the candidate
// experience to ≥ 1024 px.
export const MIN_DESKTOP_WIDTH = 1024;
