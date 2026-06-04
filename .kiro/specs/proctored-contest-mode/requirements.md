# Requirements Document

## Introduction

Proctored Contest Mode adds a lightweight, AI-assisted proctoring layer to
the existing CodeCombat 2026 contest pipeline. Admins create contests the
same way they do today, plus an optional 1:1 extension table
(`proctored_contests`) whose presence marks a Contest as proctored.
When a user enters such a contest the browser enforces fullscreen, blocks
copy/paste and right-click, runs an in-browser face detection model, logs
every suspicious event over a WebSocket to the backend, and uploads a
screenshot only on suspicious events (never continuously). The backend
persists events, computes a per-session risk score from configurable
weights, and surfaces flagged sessions to admins through a live dashboard.

The feature is explicitly scoped as **practical, low-cost monitoring** for
hundreds-to-thousands of concurrent users on a single Oracle A1 VM plus
Valkey plus PostgreSQL 18. It is **not** a military-grade anti-cheat
system. There is no continuous video recording, no server-side AI / GPU
inference, and no identity verification. The detector architecture is
designed so that future add-ons (mobile-phone detection, gaze estimation,
audio monitoring, object detection) can be plugged in without reworking
the event, risk, or storage pipelines.

This document captures **only** the requirements. Architecture, ER
diagrams, WebSocket frame format, AI library selection, folder structure,
and implementation roadmap are deferred to the design phase.

## Glossary

- **System** — The CodeCombat 2026 web platform as a whole (Spring Boot
  backend, React frontend, PostgreSQL 18, Valkey).
- **Backend** — The Spring Boot 3.5 application running on Java 21.
- **Frontend** — The existing React 19 + Vite SPA in `frontend/`.
- **Database** — The PostgreSQL 18 instance managed by Flyway.
- **Cache** — The Valkey instance used for hot state and rate limiting.
- **Admin** — An authenticated user with `ROLE_ADMIN`.
- **Candidate** — An authenticated non-admin user actively taking part in
  a Proctored_Contest.
- **Contest** — The existing `contests` row representing a coding contest.
- **Proctored_Contest** — A Contest that has a corresponding row in the
  new `proctored_contests` extension table. The base `contests` row
  continues to hold name, description, time window, and active status;
  the `proctored_contests` row holds proctoring-specific data and links
  back to the Contest via a unique foreign key `contest_id`.
- **Proctoring_Session** — A row in the new `proctoring_sessions` table
  representing one Candidate's attempt at one Proctored_Contest, created
  on entry and closed on exit, disqualification, or contest end.
- **Proctoring_Client** — The browser-side proctoring module (React) that
  enforces UI restrictions, runs the AI_Detector, captures screenshots,
  and streams events over WebSocket.
- **Proctoring_Service** — The Spring Boot subsystem that owns session
  lifecycle, event ingestion, risk scoring, and screenshot handling.
- **AI_Detector** — The in-browser face-detection runner. MVP detects
  face presence and face count; the runner is a pluggable interface that
  later detectors (gaze, mobile, audio, objects) implement.
- **Suspicious_Event** — A discrete observation that may indicate
  misconduct. MVP categories: `TAB_SWITCH`, `WINDOW_BLUR`,
  `FULLSCREEN_EXIT`, `COPY_ATTEMPT`, `PASTE_ATTEMPT`,
  `CONTEXT_MENU_ATTEMPT`, `NO_FACE`, `MULTIPLE_FACES`,
  `WEBCAM_PERMISSION_DENIED`, `WEBCAM_STREAM_LOST`, `DEVTOOLS_OPEN`.
- **Event_Ingestion** — The WebSocket endpoint plus persistence pipeline
  that receives Suspicious_Event records and writes them to the Database
  (with a Cache-backed buffer for spikes).
- **Risk_Engine** — The Backend component that converts a stream of
  Suspicious_Event rows into a numeric `risk_score` per
  Proctoring_Session, using configurable per-event weights.
- **Risk_Band** — The classification of a `risk_score` into `LOW`
  (0–50), `MEDIUM` (51–100), or `HIGH` (>100). Thresholds are
  configurable.
- **Flagged_Session** — A Proctoring_Session whose Risk_Band is `HIGH`
  or which has been manually flagged by an Admin.
- **Screenshot_Store** — The VM filesystem storage tier rooted at
  `uploads/proctoring/sessions/{session_id}/{event_id}.jpg` that holds
  per-event screenshots together with their metadata row.
- **Admin_Dashboard** — The new `/admin/proctoring` UI surface providing
  live candidate view, per-session drill-down, screenshot viewer, and
  manual override controls.
- **Force_End** — An Admin-initiated WebSocket message that terminates a
  Proctoring_Session immediately and prevents further submissions.
- **Risk_Weight_Config** — The set of `(event_type → score_delta)`
  mappings and Risk_Band thresholds that drive Risk_Engine scoring.
- **Consent_Acknowledgement** — A persisted record that a Candidate has
  read and accepted the proctoring terms before a Proctoring_Session is
  created.

## Requirements

### Requirement 1: Proctored extension table on contests

**User Story:** As an Admin, I want to mark a Contest as proctored when I
create or edit it by adding it to a separate proctored-contests table,
so that registered Candidates are subject to the proctoring layer when
they enter that Contest, without changing the schema or behaviour of
non-proctored Contests.

#### Acceptance Criteria

1. THE Database SHALL contain a new `proctored_contests` table with
   columns including its own `id` primary key and a non-null
   `contest_id` foreign key referencing `contests(id)`, and SHALL
   enforce a uniqueness constraint on `contest_id` so that the
   relationship between a Contest and its proctoring extension is 1:1.
2. THE Database SHALL NOT add a `proctored` boolean column to the
   existing `contests` table; a Contest's proctored status SHALL be
   determined solely by the existence of a `proctored_contests` row
   referencing the Contest.
3. WHEN an Admin creates a Contest through the existing admin contest
   API with a request payload that asks for proctoring, THE Backend
   SHALL insert the base `contests` row and the corresponding
   `proctored_contests` row in a single transaction.
4. WHEN an Admin edits a Contest whose status is `UPCOMING`, THE
   Backend SHALL allow adding a `proctored_contests` row to convert a
   regular Contest into a Proctored_Contest, and SHALL allow deleting
   an existing `proctored_contests` row to convert it back to a
   regular Contest.
5. IF an Admin attempts to add or delete a `proctored_contests` row
   for a Contest whose status is `LIVE` or `ENDED`, THEN THE Backend
   SHALL reject the change with HTTP `409 Conflict` and a reason
   message.
6. WHEN the existing contest detail API returns a Contest, THE Backend
   SHALL include a derived `proctored` boolean field in the response
   payload computed from the existence of a `proctored_contests` row.
7. WHEN the existing contest list UI renders a Contest, THE Frontend
   SHALL display a visible "Proctored" badge for any Contest whose
   derived `proctored` field is `TRUE`.
8. WHEN an Admin adds a `proctored_contests` row for a Contest that
   already has rows in the existing `contest_registrations` table,
   THE Backend SHALL preserve those registrations unchanged; existing
   registrations SHALL continue to count as registered for the
   Proctored_Contest, and the Consent_Acknowledgement step in
   Requirement 2 SHALL gate entry rather than re-registration.

### Requirement 2: Candidate consent before entering a Proctored_Contest

**User Story:** As a Candidate, I want to be told what is monitored and
asked for explicit consent before a Proctored_Contest starts, so that I
can decide whether to participate.

#### Acceptance Criteria

1. WHEN a Candidate navigates to the entry page of a Proctored_Contest,
   THE Frontend SHALL display a consent screen listing every monitored
   signal (fullscreen enforcement, tab/focus tracking, copy/paste
   blocking, webcam-based face detection, on-event screenshot capture).
2. WHILE the consent screen is visible, THE Frontend SHALL prevent the
   Candidate from accessing the contest problems UI.
3. WHEN the Candidate accepts the consent, THE Backend SHALL create a
   Consent_Acknowledgement row capturing `(user_id, contest_id,
   accepted_at, consent_version)`.
4. WHEN the Candidate accepts the consent and a Proctoring_Session does
   not yet exist for `(user_id, contest_id)`, THE Backend SHALL create
   the Proctoring_Session.
5. IF the Candidate declines the consent, THEN THE Frontend SHALL return
   the Candidate to the contest list and SHALL NOT create a
   Proctoring_Session.
6. WHEN a Consent_Acknowledgement already exists for the current
   `consent_version`, THE Frontend SHALL skip the consent screen and
   proceed directly to the pre-flight check defined in Requirement 3.

### Requirement 3: Pre-flight environment check

**User Story:** As a Candidate, I want the System to check my browser
environment before the Proctoring_Session starts, so that I am not
disqualified mid-contest because of a setup problem I could have fixed up
front.

#### Acceptance Criteria

1. WHEN the consent screen is accepted, THE Frontend SHALL run a
   pre-flight check that verifies, in order: webcam permission granted,
   webcam stream active with at least one detectable face, Fullscreen
   API supported, the AI_Detector model loaded, and a WebSocket
   connection to the Backend established.
2. IF the webcam permission is denied, THEN THE Frontend SHALL display a
   blocking message that the Proctored_Contest cannot start without
   camera access and SHALL NOT transition to the contest UI.
3. IF the AI_Detector model fails to load after the configured retry
   budget, THEN THE Frontend SHALL display a blocking error and SHALL
   NOT transition to the contest UI.
4. IF the WebSocket connection cannot be established within the
   configured timeout, THEN THE Frontend SHALL display a retry
   affordance and SHALL NOT transition to the contest UI until the
   connection succeeds.
5. WHEN every pre-flight check passes, THE Frontend SHALL request
   fullscreen entry through a user gesture and transition to the
   contest UI.

### Requirement 4: Fullscreen enforcement

**User Story:** As an Admin, I want Candidates to remain in fullscreen
during a Proctored_Contest, so that other windows cannot be referenced
without leaving a trace.

#### Acceptance Criteria

1. WHEN the Candidate transitions into the contest UI, THE Frontend
   SHALL invoke the Fullscreen API on the contest root element.
2. WHILE a Proctoring_Session is active, THE Frontend SHALL listen for
   the `fullscreenchange` event.
3. WHEN the `fullscreenchange` event fires and fullscreen is no longer
   active, THE Frontend SHALL emit a `FULLSCREEN_EXIT` Suspicious_Event
   to the Backend with a client-side timestamp.
4. WHEN a `FULLSCREEN_EXIT` Suspicious_Event is emitted, THE Frontend
   SHALL display a blocking modal that pauses the editor and prompts
   the Candidate to re-enter fullscreen.
5. WHILE the fullscreen-exit modal is visible, THE Frontend SHALL block
   all contest UI interactions including code editing, submission
   controls, navigation, and any form inputs.
6. WHEN the Candidate re-enters fullscreen, THE Frontend SHALL dismiss
   the modal and resume the contest UI.

### Requirement 5: Tab and window focus monitoring

**User Story:** As an Admin, I want to know when a Candidate switches
tabs, minimises the window, or otherwise loses focus, so that suspicious
context switches contribute to the risk score.

#### Acceptance Criteria

1. WHILE a Proctoring_Session is active, THE Frontend SHALL listen for
   `visibilitychange`, `blur`, and `focus` events on the window.
2. WHEN the document `visibilitychange` event reports `hidden`, THE
   Frontend SHALL emit a `TAB_SWITCH` Suspicious_Event with a client
   timestamp.
3. WHEN the window `blur` event fires while the document remains
   visible, THE Frontend SHALL emit a `WINDOW_BLUR` Suspicious_Event
   with a client timestamp.
4. WHEN focus is restored after a `TAB_SWITCH` or `WINDOW_BLUR`, THE
   Frontend SHALL emit a `FOCUS_RESTORED` informational event including
   the duration of the away interval.
5. WHILE the document is hidden, THE Frontend SHALL keep the editor in
   a read-only state and SHALL block code submission.

### Requirement 6: Copy, paste, right-click, and text-selection blocking

**User Story:** As an Admin, I want common shortcuts for moving code in
and out of the contest UI to be blocked or logged, so that Candidates
cannot trivially paste from external sources.

#### Acceptance Criteria

1. WHILE a Proctoring_Session is active, THE Frontend SHALL prevent the
   default behaviour of `copy`, `cut`, and `paste` events on the
   contest UI root.
2. WHEN a `copy`, `cut`, or `paste` event is intercepted, THE Frontend
   SHALL emit the corresponding Suspicious_Event (`COPY_ATTEMPT`,
   `CUT_ATTEMPT`, or `PASTE_ATTEMPT`) with a client timestamp.
3. WHILE a Proctoring_Session is active, THE Frontend SHALL prevent
   the default `contextmenu` event on the contest UI root and SHALL
   emit a `CONTEXT_MENU_ATTEMPT` Suspicious_Event.
4. WHILE a Proctoring_Session is active, THE Frontend SHALL apply CSS
   `user-select: none` to non-editor regions of the contest UI, and
   this style SHALL remain applied for the entire duration of the
   Proctoring_Session regardless of browser compatibility behaviour.
5. WHERE the Candidate uses the in-editor copy/paste within the same
   editor instance, THE Frontend SHALL allow the operation without
   emitting a Suspicious_Event.

### Requirement 7: Browser-side AI face detection

**User Story:** As an Admin, I want the System to detect when no face or
multiple faces are visible on the Candidate's webcam, so that those
observations contribute to the risk score without sending video to the
server.

#### Acceptance Criteria

1. WHILE a Proctoring_Session is active, THE AI_Detector SHALL run
   inference on webcam frames at a configurable interval, defaulting to
   one inference per second.
2. THE AI_Detector SHALL run entirely in the browser and SHALL NOT
   transmit raw video frames to the Backend.
3. WHEN the AI_Detector reports zero faces continuously for at least
   the configured `noFaceThresholdSeconds` (default 5 seconds), THE
   Frontend SHALL emit a `NO_FACE` Suspicious_Event.
4. WHEN the AI_Detector reports two or more faces in a single frame,
   THE Frontend SHALL emit a `MULTIPLE_FACES` Suspicious_Event with the
   detected face count.
5. WHEN the face count returns to exactly one after a `NO_FACE` or
   `MULTIPLE_FACES` event, THE Frontend SHALL emit a
   `FACE_STATE_RESTORED` informational event.
6. IF the webcam stream stops while a Proctoring_Session is active,
   THEN THE Frontend SHALL emit a `WEBCAM_STREAM_LOST` Suspicious_Event
   and SHALL display a blocking modal until the stream is restored.
7. THE AI_Detector SHALL expose a detector-plugin interface that
   accepts new detectors (e.g. mobile-phone, gaze, audio) without
   changes to the Event_Ingestion, Risk_Engine, or Screenshot_Store.

### Requirement 8: On-event screenshot capture

**User Story:** As an Admin, I want a screenshot captured at the moment a
Suspicious_Event fires, so that I have evidence to review without storing
continuous video.

#### Acceptance Criteria

1. WHEN a Suspicious_Event of type `TAB_SWITCH`, `FULLSCREEN_EXIT`,
   `MULTIPLE_FACES`, `NO_FACE`, or `WEBCAM_STREAM_LOST` is emitted,
   THE Frontend SHALL capture a single still frame from the active
   webcam stream.
2. THE captured frame SHALL be encoded as JPEG with quality and maximum
   dimensions configured by the Backend (defaults: quality `0.7`, max
   `640x480`).
3. WHEN the still frame is encoded, THE Frontend SHALL upload the bytes
   to the Backend together with `(session_id, event_id, captured_at,
   event_type)`.
4. THE Backend SHALL reject screenshot uploads larger than the
   configured `maxScreenshotBytes` (default 256 KB) with HTTP `413`.
5. THE Backend SHALL reject screenshot uploads whose declared MIME type
   is not `image/jpeg` or `image/png` with HTTP `415`.
6. WHEN a screenshot is accepted, THE Backend SHALL persist a row in
   `proctoring_screenshots` linking it to the originating event row.
7. THE System SHALL NOT capture or upload screenshots on a periodic
   timer; capture is strictly event-driven.

### Requirement 9: Suspicious event ingestion over WebSocket

**User Story:** As a Candidate, I want my events delivered to the
Backend reliably and in real time, so that the Admin_Dashboard reflects
my Proctoring_Session state without large delays.

#### Acceptance Criteria

1. WHEN a Proctoring_Session is created, THE Frontend SHALL open a
   WebSocket connection to the Backend at the proctoring endpoint
   defined in the design phase.
2. WHEN the WebSocket handshake occurs, THE Backend SHALL authenticate
   the connection using the existing JWT mechanism and SHALL reject
   any unauthenticated connection with a close code `4401`.
3. WHEN the WebSocket is authenticated, THE Backend SHALL bind it to
   exactly one `(user_id, session_id)` pair.
4. IF a second WebSocket connection is opened for a `(user_id,
   session_id)` already bound, THEN THE Backend SHALL close the new
   connection with code `4409` and SHALL leave the existing connection
   intact.
5. WHEN the Frontend emits a Suspicious_Event, THE Frontend SHALL send
   it as a JSON frame containing at minimum `event_type`,
   `client_timestamp`, and an optional `payload` object.
6. WHEN the Backend receives a Suspicious_Event frame, THE Backend
   SHALL persist a row in `proctoring_events` with the server-side
   receive timestamp and the originating `session_id`.
7. WHILE a Proctoring_Session is active, THE Frontend SHALL send a
   heartbeat frame at a configurable interval (default 15 seconds).
8. IF the Backend receives no heartbeat from a connected
   Proctoring_Session for the configured `heartbeatTimeoutSeconds`
   (default 45 seconds), THEN THE Backend SHALL emit a
   `HEARTBEAT_TIMEOUT` Suspicious_Event into the session and SHALL
   close the WebSocket with code `4408`.

### Requirement 10: Server-to-client WebSocket messages

**User Story:** As a Candidate, I want the System to inform me when my
risk score changes or when the Admin takes action, so that I am not
surprised by a session ending.

#### Acceptance Criteria

1. WHEN the Risk_Engine updates the `risk_score` of a
   Proctoring_Session, THE Backend SHALL push a `RISK_UPDATE` frame to
   that session's WebSocket containing the new score and Risk_Band.
2. WHEN an Admin issues a Force_End on a Proctoring_Session, THE
   Backend SHALL push a `SESSION_TERMINATED` frame containing a reason
   string and SHALL close the WebSocket with code `4003`.
3. WHEN an Admin issues a non-terminating warning on a
   Proctoring_Session, THE Backend SHALL push a `WARNING` frame
   containing the Admin-supplied message.
4. WHEN a `SESSION_TERMINATED` frame is received, THE Frontend SHALL
   block all submission UI and display a terminal screen with the
   reason string.

### Requirement 11: Offline buffering and replay on reconnect

**User Story:** As a Candidate, I want my events preserved if my
internet drops briefly, so that a temporary network glitch does not
discard evidence or unfairly raise my risk score.

#### Acceptance Criteria

1. WHILE the WebSocket is disconnected, THE Frontend SHALL buffer every
   emitted Suspicious_Event in IndexedDB with its original
   `client_timestamp`.
2. WHEN the WebSocket reconnects, THE Frontend SHALL replay every
   buffered Suspicious_Event in original timestamp order before
   sending any new events.
3. WHEN replaying buffered events, THE Frontend SHALL include a
   `replayed: true` marker so the Backend can distinguish real-time
   events from replayed ones.
4. WHEN the IndexedDB buffer reaches the configured
   `maxOfflineEvents` (default 1000), THE Frontend SHALL drop the
   oldest buffered events and SHALL emit a `BUFFER_OVERFLOW`
   Suspicious_Event on next reconnect.
5. WHILE the WebSocket is disconnected for longer than the configured
   `maxOfflineSeconds` (default 60 seconds), THE Frontend SHALL
   display a blocking modal informing the Candidate that the contest
   UI is paused until reconnection.

### Requirement 12: Risk scoring engine

**User Story:** As an Admin, I want each Proctoring_Session to carry a
single, comparable risk score derived from configurable per-event
weights, so that I can quickly triage which Candidates need review.

#### Acceptance Criteria

1. THE Backend SHALL store a Risk_Weight_Config consisting of an
   `event_type → score_delta` mapping and the Risk_Band thresholds.
2. THE Risk_Weight_Config SHALL ship with the following default
   weights: `TAB_SWITCH=20`, `WINDOW_BLUR=10`, `FULLSCREEN_EXIT=30`,
   `MULTIPLE_FACES=50`, `NO_FACE=40`, `COPY_ATTEMPT=5`,
   `PASTE_ATTEMPT=15`, `CONTEXT_MENU_ATTEMPT=2`,
   `WEBCAM_STREAM_LOST=40`, `HEARTBEAT_TIMEOUT=20`.
3. THE Risk_Weight_Config SHALL ship with default Risk_Band thresholds
   `LOW=0..50`, `MEDIUM=51..100`, `HIGH=>100`.
4. WHEN a Suspicious_Event is persisted, THE Risk_Engine SHALL add the
   configured `score_delta` for that event type to the
   Proctoring_Session's `risk_score`.
5. WHEN the `risk_score` of a Proctoring_Session crosses a Risk_Band
   threshold, THE Risk_Engine SHALL update the session's `risk_band`
   field and SHALL emit a `RISK_BAND_CHANGED` event onto the
   Admin_Dashboard live channel.
6. WHEN a Proctoring_Session enters Risk_Band `HIGH`, THE Backend
   SHALL set its `flagged` field to `TRUE`.
7. THE Risk_Engine SHALL be deterministic: the same ordered sequence
   of Suspicious_Event records SHALL produce the same final
   `risk_score` and `risk_band`.
8. THE Risk_Engine SHALL recompute a session's `risk_score` from the
   persisted event log on demand for an Admin-triggered "rescore"
   action.

### Requirement 13: Proctoring_Session lifecycle

**User Story:** As the System, I want a single source of truth for each
Candidate's participation in a Proctored_Contest, so that events,
screenshots, and risk scores hang off one row.

#### Acceptance Criteria

1. THE Database SHALL contain a `proctoring_sessions` table with
   columns including `id`, `contest_id`, `user_id`, `started_at`,
   `ended_at`, `end_reason`, `risk_score`, `risk_band`, `flagged`,
   and `client_ip`.
2. THE Database SHALL enforce a uniqueness constraint that at most one
   `proctoring_sessions` row exists per `(contest_id, user_id)` pair.
3. WHEN a Candidate consents and passes the pre-flight check, THE
   Backend SHALL insert a `proctoring_sessions` row and return its
   `id`.
4. IF a Candidate attempts to create a second Proctoring_Session for
   a `(contest_id, user_id)` pair that already has one with
   `ended_at IS NULL`, THEN THE Backend SHALL reject the request with
   HTTP `409 Conflict`.
5. WHEN the Proctored_Contest's `end_time` is reached, THE Backend
   SHALL close every active Proctoring_Session for that Contest with
   `end_reason='CONTEST_ENDED'`.
6. WHEN a Candidate finishes the contest and submits the "finish"
   action, THE Backend SHALL close the Proctoring_Session with
   `end_reason='SELF_FINISHED'`.
7. WHEN an Admin issues a Force_End, THE Backend SHALL close the
   Proctoring_Session with `end_reason='ADMIN_FORCED'`.
8. WHEN a Candidate clicks the Quit Contest control defined in
   Requirement 24, THE Backend SHALL close the Proctoring_Session with
   `end_reason='SELF_QUIT'`.
9. WHEN a Proctoring_Session is closed with `end_reason='ADMIN_FORCED'`
   or `end_reason='SELF_QUIT'`, THE Backend SHALL prevent the same
   `(user_id, contest_id)` pair from creating a new Proctoring_Session
   for the remainder of the Proctored_Contest's lifetime.

### Requirement 14: Database schema for events and screenshots

**User Story:** As a Database administrator, I want event and screenshot
data stored in dedicated tables that scale to thousands of concurrent
sessions, so that queries from the Admin_Dashboard remain fast.

#### Acceptance Criteria

1. THE Database SHALL contain a `proctoring_events` table with
   columns including `id`, `session_id`, `event_type`,
   `client_timestamp`, `server_timestamp`, `payload_json`, and
   `replayed`.
2. THE Database SHALL contain a `proctoring_screenshots` table with
   columns including `id`, `session_id`, `event_id`, `captured_at`,
   `mime_type`, `byte_size`, and `storage_ref`.
3. THE `storage_ref` column SHALL hold a relative filesystem path
   under the existing VM uploads root (analogous to
   `uploads/profile-photos/`) using the path scheme
   `uploads/proctoring/sessions/{session_id}/{event_id}.jpg`.
4. THE Database SHALL define an index on `proctoring_events
   (session_id, server_timestamp)` to support per-session timelines.
5. THE Database SHALL define an index on `proctoring_screenshots
   (session_id)` to support per-session galleries.
6. THE Database SHALL define an index on `proctoring_sessions
   (contest_id, flagged)` to support the Admin_Dashboard flagged-list
   query.

### Requirement 15: Admin live dashboard

**User Story:** As an Admin, I want a live dashboard showing every
active Candidate's risk state during a Proctored_Contest, so that I can
intervene before the contest ends.

#### Acceptance Criteria

1. THE Frontend SHALL provide an admin route (path defined in design
   phase) that lists every active Proctoring_Session for a selected
   Proctored_Contest with columns `username`, `risk_score`,
   `risk_band`, `flagged`, `last_event_at`, and `connected`.
2. WHILE the Admin_Dashboard is open, THE Frontend SHALL receive
   live updates via a server-pushed channel and SHALL re-render
   affected rows without a full page reload.
3. WHEN the Admin clicks a row, THE Frontend SHALL navigate to a
   per-session drill-down showing the chronological event log,
   the screenshot gallery, and a current risk-score chart.
4. WHEN the Admin views a screenshot, THE Backend SHALL serve the
   image bytes only to authenticated Admin requests.
5. THE Admin_Dashboard SHALL provide a filter for `flagged=TRUE`.
6. THE Admin_Dashboard SHALL allow an Admin to issue a Force_End on
   a session and SHALL allow an Admin to send a non-terminating
   `WARNING` to a session.
7. WHEN an Admin issues a Force_End or a `WARNING`, THE Backend
   SHALL persist an audit row `(admin_id, session_id, action,
   acted_at, reason)`.

### Requirement 16: Authentication and authorisation

**User Story:** As the System, I want every proctoring entry point to
be authenticated, so that Candidates cannot impersonate each other and
Admins cannot be impersonated.

#### Acceptance Criteria

1. THE Backend SHALL require a valid JWT for every REST endpoint under
   the proctoring API.
2. THE Backend SHALL require a valid JWT for every WebSocket
   connection to the proctoring endpoint.
3. THE Backend SHALL bind each Proctoring_Session to the JWT's
   `user_id` only after the JWT signature, expiry, and issuer have
   been validated, and SHALL reject any later request whose validated
   JWT `user_id` does not match the session's `user_id`.
4. THE Backend SHALL require `ROLE_ADMIN` for every endpoint under
   the Admin_Dashboard API.
5. IF a JWT expires while a WebSocket is open, THEN THE Backend SHALL
   close the WebSocket with code `4401` at the next heartbeat.

### Requirement 17: Rate limiting and abuse protection on event ingestion

**User Story:** As an Operator, I want the Event_Ingestion path to
withstand a malicious or buggy client flooding events, so that one
Candidate cannot degrade service for everyone else.

#### Acceptance Criteria

1. THE Backend SHALL apply a per-Proctoring_Session rate limit to
   inbound Suspicious_Event frames using a Cache-backed counter.
2. THE default per-session rate limit SHALL be 30 events per second
   averaged over a 10-second window.
3. WHEN a Proctoring_Session exceeds its rate limit, THE Backend
   SHALL drop the offending frame and SHALL emit a single
   `RATE_LIMIT_EXCEEDED` Suspicious_Event into that session per
   window.
4. THE Backend SHALL apply a per-Proctoring_Session screenshot upload
   rate limit defaulting to 10 uploads per minute.
5. WHEN a Proctoring_Session exceeds the screenshot rate limit, THE
   Backend SHALL respond `429 Too Many Requests` and SHALL NOT
   persist the upload.
6. THE Backend SHALL reject any inbound Suspicious_Event frame whose
   payload is larger than `maxEventBytes` (default 4 KB) with a close
   code `4413`.

### Requirement 18: Scalability and hot-state strategy

**User Story:** As an Operator, I want the proctoring layer to handle
the published concurrency targets on the existing infrastructure, so
that adding proctored contests does not require a hardware upgrade for
the MVP target.

#### Acceptance Criteria

1. THE Backend SHALL keep per-session live state (`risk_score`,
   `risk_band`, `last_event_at`, `connected`) in the Cache so that
   the Admin_Dashboard live query does not hit the Database for every
   tick.
2. THE Backend SHALL persist every Suspicious_Event durably to the
   Database; the Cache state is a derived projection.
3. THE Backend SHALL be horizontally scalable: any Backend instance
   SHALL be able to serve the Admin_Dashboard live view by reading
   the Cache, regardless of which instance owns a given Candidate's
   WebSocket.
4. THE Backend SHALL meet the MVP target of 100 concurrent
   Proctoring_Sessions on a single Oracle A1 VM with the existing
   Backend, Cache, and Database resources.
5. THE design SHALL document the scaling path to 1,000 and 10,000
   concurrent sessions including the additional infrastructure
   components required at each tier.

### Requirement 19: Submission integration

**User Story:** As a Candidate, I want my code submissions during a
Proctored_Contest to flow through the existing submission pipeline,
so that the proctoring layer does not change how my code is judged.

#### Acceptance Criteria

1. WHEN a Candidate submits code from within a Proctoring_Session,
   THE Backend SHALL route the submission through the existing
   submission worker pool unchanged.
2. THE Backend SHALL tag every submission made inside a
   Proctoring_Session with the `session_id`.
3. IF a Proctoring_Session is closed with `end_reason='ADMIN_FORCED'`,
   `end_reason='SELF_QUIT'`, or `end_reason='HEARTBEAT_TIMEOUT'`,
   THEN THE Backend SHALL reject any subsequent submission referencing
   that session with HTTP `403`.
4. WHEN the Admin_Dashboard drill-down is opened, THE Backend SHALL
   include the list of submissions associated with the session so
   that risk evidence and code can be reviewed together.

### Requirement 20: Configuration surface

**User Story:** As an Operator, I want every tunable parameter exposed
through configuration, so that I can adjust thresholds for a specific
Contest without redeploying the Backend.

#### Acceptance Criteria

1. THE Backend SHALL expose the following configuration keys with the
   listed defaults: `noFaceThresholdSeconds=5`,
   `aiInferenceIntervalMs=1000`, `heartbeatIntervalSeconds=15`,
   `heartbeatTimeoutSeconds=45`, `maxOfflineSeconds=60`,
   `maxOfflineEvents=1000`, `maxEventBytes=4096`,
   `maxScreenshotBytes=262144`, `screenshotMaxWidth=640`,
   `screenshotMaxHeight=480`, `screenshotJpegQuality=0.7`,
   `eventRateLimitPerSecond=30`, `screenshotRateLimitPerMinute=10`.
2. THE Backend SHALL load every key from `application.properties` and
   from `.env.production-vm` in the conventional Spring order.
3. THE Backend SHALL provide a single global Risk_Weight_Config for
   the MVP; per-Contest overrides of the Risk_Weight_Config are
   explicitly out of scope and SHALL NOT be implemented for the MVP.

### Requirement 21: Audit and retention

**User Story:** As a Compliance reviewer, I want clear records of what
was monitored, when, and for how long it is retained, so that I can
answer data-protection questions.

#### Acceptance Criteria

1. THE Backend SHALL persist Consent_Acknowledgement rows for at
   least the duration that the associated Proctored_Contest is
   retained.
2. THE Backend SHALL persist `proctoring_events` and
   `proctoring_screenshots` rows for exactly 30 days from each row's
   `captured_at` (for screenshots) or `server_timestamp` (for events).
3. WHEN the 30-day retention period elapses for a screenshot row, THE
   Backend SHALL purge the corresponding filesystem object at its
   `storage_ref` path and SHALL delete the `proctoring_screenshots`
   row in the same scheduled job.
4. THE Backend SHALL run a scheduled retention job that performs the
   purge described in Requirement 21.3 on a daily cadence and SHALL
   log the count of files and rows purged per run.
5. THE Backend SHALL log every Admin Force_End and `WARNING` action
   to an append-only audit table.

### Requirement 22: Detector extensibility (architectural requirement)

**User Story:** As a Future maintainer, I want to add new detectors
(mobile-phone presence, gaze direction, audio anomalies, object
detection) without changing the Event_Ingestion, Risk_Engine, or
Screenshot_Store, so that the MVP investment is not thrown away when
the next signal is added.

#### Acceptance Criteria

1. THE AI_Detector SHALL define a stable plugin interface accepting
   an input frame and returning zero or more typed observations.
2. WHEN a new detector emits a new `event_type`, THE Backend SHALL
   accept and persist the event provided the type is registered in
   the Risk_Weight_Config.
3. WHEN a new `event_type` has no entry in the Risk_Weight_Config,
   THE Backend SHALL persist the event with `score_delta=0` and
   SHALL log a warning.
4. THE Frontend SHALL render any registered `event_type` in the
   Admin_Dashboard event log without per-type code changes.

### Requirement 23: Non-goals (explicit exclusions)

**User Story:** As a Reviewer of this spec, I want non-goals stated
explicitly, so that scope creep is rejected with a clear reference.

#### Acceptance Criteria

1. THE System SHALL NOT continuously record or store webcam video.
2. THE System SHALL NOT continuously record or store screen video.
3. THE Backend SHALL NOT perform AI inference on a server-side GPU
   for the MVP.
4. THE MVP SHALL NOT implement mobile-phone detection, gaze
   estimation, audio anomaly detection, or object detection.
5. THE MVP SHALL NOT implement KYC, ID-card capture, or any other
   identity verification.
6. THE MVP SHALL NOT implement per-Contest overrides of the
   Risk_Weight_Config; only the global Risk_Weight_Config defined in
   Requirement 12 SHALL apply.
7. THE Frontend SHALL NOT use Server-Sent Events or HTTP polling for
   the proctoring transport; bidirectional WebSocket as defined in
   Requirements 9 and 10 SHALL be the only supported transport.
8. THE proctoring frontend module SHALL be implemented in JavaScript
   and React matching the existing repo; TypeScript SHALL NOT be
   introduced for this feature.
9. THE proctoring UI SHALL target desktop browsers only with a
   minimum viewport width of `1024px`; mobile and tablet support
   SHALL NOT be in scope for the MVP.

### Requirement 24: Candidate exit controls

**User Story:** As a Candidate, I want explicit Finish and Quit
controls inside a Proctoring_Session, so that I can leave a
Proctored_Contest deliberately rather than by closing the browser
window.

#### Acceptance Criteria

1. WHILE a Proctoring_Session is active, THE Frontend SHALL render a
   "Finish Contest" control inside the contest UI that submits the
   Candidate's final state and ends the session.
2. WHEN the Candidate clicks "Finish Contest" and confirms the
   action in a confirmation dialog, THE Frontend SHALL send a finish
   request to the Backend and THE Backend SHALL close the
   Proctoring_Session with `end_reason='SELF_FINISHED'` per
   Requirement 13.6.
3. WHILE a Proctoring_Session is active, THE Frontend SHALL render a
   "Quit Contest" control inside the contest UI, visually
   distinguished from the "Finish Contest" control, that abandons
   the Candidate's attempt.
4. WHEN the Candidate clicks "Quit Contest", THE Frontend SHALL
   display a confirmation dialog warning that quitting is permanent
   and that the Candidate cannot re-enter the Proctored_Contest.
5. WHEN the Candidate confirms the quit dialog, THE Frontend SHALL
   send a quit request to the Backend and THE Backend SHALL close
   the Proctoring_Session with `end_reason='SELF_QUIT'` per
   Requirement 13.8 and SHALL apply the lockout defined in
   Requirement 13.9.
6. WHEN a Proctoring_Session has been closed with
   `end_reason='SELF_QUIT'`, `end_reason='ADMIN_FORCED'`, or
   `end_reason='HEARTBEAT_TIMEOUT'`, THE Frontend SHALL display a
   terminal screen identifying the reason and SHALL NOT offer any
   re-entry control for the same `(user_id, contest_id)` pair.

## Resolved Decisions

The following decisions have been resolved with the user and are
incorporated into the requirements above. They are recorded here for
traceability against the original draft.

1. **Proctored extension table.** A separate `proctored_contests` 1:1
   extension table is used instead of a boolean flag on `contests`
   (Requirement 1).
2. **Existing registrations carry over.** Existing
   `contest_registrations` rows continue to count when a Contest is
   converted to a Proctored_Contest; consent gates entry, not
   re-registration (Requirement 1.8).
3. **Webcam permission is a hard block.** No degraded-mode fallback
   (Requirement 3.2).
4. **Flagged is review-only.** No auto-disqualify or auto-pause
   (Requirement 12.6, Requirement 15).
5. **Filesystem storage with 30-day retention.** Screenshots are
   stored on the VM filesystem under
   `uploads/proctoring/sessions/{session_id}/{event_id}.jpg` and
   purged at exactly 30 days (Requirement 14.3, Requirement 21).
6. **60-second offline cap accepted** (Requirement 11.5).
7. **Global Risk_Weight_Config only for MVP.** Per-Contest overrides
   are excluded from MVP (Requirement 20.3, Requirement 23.6).
8. **Bidirectional WebSocket only.** SSE and HTTP polling are
   explicitly rejected (Requirement 23.7).
9. **AI inference at 1/sec, configurable.** The
   `aiInferenceIntervalMs=1000` default is the agreed baseline
   (Requirement 7.1, Requirement 20.1).
10. **JavaScript + React, no TypeScript** (Requirement 23.8).
11. **Force_End plus SELF_QUIT lockout.** ADMIN_FORCED and SELF_QUIT
    both lock out re-entry for the contest's lifetime
    (Requirement 13.7, 13.8, 13.9, Requirement 24).
12. **Desktop only.** Minimum viewport width 1024px; mobile and
    tablet are out of scope for MVP (Requirement 23.9).

## Iteration and Feedback

Reply with any wording changes; this document will be revised in
place before the design phase begins.
