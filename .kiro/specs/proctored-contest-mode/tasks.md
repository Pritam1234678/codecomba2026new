# Implementation Plan: Proctored Contest Mode

## Overview

Convert the feature design into a series of prompts for a code-generation
LLM that will implement each step with incremental progress. Each task
builds on the previous tasks and ends with wiring things together. There
should be no hanging or orphaned code that is not integrated into a
previous step. The 14 top-level tasks below mirror the implementation
roadmap in `design.md` exactly: V7 migration → admin extension API →
consent + preflight shell → WebSocket skeleton → risk engine → browser
detectors → screenshot pipeline → browser AI → IndexedDB offline buffer
→ admin dashboard → lockout enforcement → finish/quit → rate limits →
end-to-end smoke verification.

The backend uses Java 21 / Spring Boot 3.5 in the new package
`com.example.codecombat2026.proctoring.*`. The frontend uses JavaScript +
React 19 (no TypeScript per Req 23.8) under `frontend/src/proctoring/`.
Sub-tasks marked with `*` are optional (property-based tests, integration
tests) and can be skipped per the user's "skip optional tests, focus on
shipping" directive — the mandatory verification gate is the smoke test
in task 14.

## Tasks

- [x] 1. V7 migration, entities, repositories, and configuration surface
  - [x] 1.1 Create the V7 Flyway migration for the six proctoring tables
    - Add `src/main/resources/db/migration/V7__proctoring.sql` with the
      DDL block from `design.md` (proctored_contests, proctoring_sessions,
      proctoring_events, proctoring_screenshots, proctoring_consent_acks,
      proctoring_admin_audit).
    - Use `CREATE TABLE IF NOT EXISTS`, `DO $$ … END$$` blocks for FKs
      keyed on `pg_constraint.conname`, and `CREATE INDEX IF NOT EXISTS`
      to match the V5/V6 idempotent style.
    - Confirm the migration runs on Spring boot with
      `spring.flyway.validate-on-migrate=true` and produces all
      indexes (`idx_ps_contest_flagged`, `idx_ps_user_ended`,
      `idx_pe_session_server_ts`, `idx_pe_type_server_ts`,
      `idx_pshot_session`, `idx_pshot_captured_at`, `idx_paa_session`).
    - _Requirements: 1.1, 1.2, 13.1, 13.2, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_
  - [x] 1.2 Add the six JPA entities under `proctoring/entity/`
    - Create `ProctoredContest`, `ProctoringSession`, `ProctoringEvent`,
      `ProctoringScreenshot`, `ProctoringConsentAck`,
      `ProctoringAdminAudit` annotated with `@Entity`,
      `@Table(name=…)`, Lombok `@Data`/`@NoArgsConstructor`/
      `@AllArgsConstructor`.
    - Add Java enums `RiskBand` (`LOW`, `MEDIUM`, `HIGH`) and
      `EndReason` (`CONTEST_ENDED`, `SELF_FINISHED`, `SELF_QUIT`,
      `ADMIN_FORCED`, `HEARTBEAT_TIMEOUT`); map both with
      `@Enumerated(EnumType.STRING)` so the DB CHECK constraints stay
      authoritative.
    - Map `payload_json` on `ProctoringEvent` as a `String` column and
      parse it with Jackson on read; never store raw screenshot bytes
      in any entity (`storage_ref` only).
    - Verify `spring.jpa.hibernate.ddl-auto=validate` continues to pass
      against V7.
    - _Requirements: 1.1, 13.1, 14.1, 14.2, 14.3_
  - [x] 1.3 Add the six Spring Data JPA repositories under
    `proctoring/repository/`
    - `ProctoredContestRepository`: `findByContestId`,
      `existsByContestId`.
    - `ProctoringSessionRepository`: `findByContestIdAndUserId`,
      `findByContestIdAndUserIdAndEndedAtIsNull`,
      `findByContestIdAndFlagged`,
      `findByContestIdAndEndedAtIsNull`,
      `existsByUserIdAndContestIdAndEndReasonIn`.
    - `ProctoringEventRepository`:
      `findBySessionIdOrderByServerTimestampAsc`,
      `deleteByServerTimestampBefore` (returns int).
    - `ProctoringScreenshotRepository`:
      `findBySessionIdOrderByCapturedAtAsc`,
      `findByCapturedAtBefore(Instant, Pageable)`.
    - `ProctoringConsentRepository`:
      `existsByUserIdAndContestIdAndConsentVersion`.
    - `ProctoringAdminAuditRepository`:
      `findBySessionIdOrderByActedAtDesc`.
    - _Requirements: 1.6, 13.2, 13.9, 14.6, 21.2, 21.3, 24.6_
  - [x] 1.4 Create `ProctoringConfig` with all 13 tunable keys
    - Add `proctoring/config/ProctoringConfig.java` annotated with
      `@Configuration` + `@ConfigurationProperties(prefix="proctoring")`
      + Lombok `@Data`. Include the defaults from Req 20.1 plus
      `weights` map (Req 12.2 defaults) and `Bands` inner record
      (`lowMax=50`, `mediumMax=100`).
    - Append the 13 `proctoring.*` keys to
      `src/main/resources/application.properties` using the
      `${ENV_VAR:default}` pattern, and mirror them in
      `.env.production-vm` and `.env.example` so operators have a
      single ops surface.
    - Verify `@EnableScheduling` is on `Codecombat2026Application`; add
      it if missing.
    - _Requirements: 7.1, 9.7, 9.8, 11.4, 11.5, 12.2, 12.3, 17.2, 17.4, 17.6, 20.1, 20.2_
  - [x] 1.5 Property test (jqwik) — Property 1 + Property 2
    - **Property 1: Proctored extension lifecycle (atomic + UPCOMING-gated + derived flag)**
    - **Validates: Requirements 1.1, 1.3, 1.4, 1.5, 1.6, 1.8**
    - **Property 2: Consent ack round-trip and skip-on-existing**
    - **Validates: Requirements 2.3, 2.5, 2.6**

- [x] 2. Admin extension API + frontend "Proctored" badge
  - [x] 2.1 Implement `ProctoredContestController` (admin)
    - Create `proctoring/controller/ProctoredContestController.java` at
      `@RequestMapping("/api/admin/proctoring/contests")`, all methods
      `@PreAuthorize("hasRole('ADMIN')")`.
    - `POST /{contestId}` inserts a `proctored_contests` row
      (idempotent: returns existing if present); reject with
      `ProctoringStateConflictException("CONTEST_NOT_UPCOMING")` (409)
      if `contests.status != 'UPCOMING'`. Body:
      `{ "consentVersion": 1 }` optional.
    - `DELETE /{contestId}` deletes the row; same UPCOMING guard.
    - Add `ProctoringStateConflictException`, `ProctoringNotFoundException`, and `ProctoringForbiddenException` under
      `proctoring/exception/` and wire them into
      `GlobalExceptionHandler` (404 / 403 / 409 with `error` code).
    - _Requirements: 1.3, 1.4, 1.5, 16.1, 16.4_
  - [x] 2.2 Extend `ContestController` to surface the derived `proctored` flag
    - Inject `ProctoredContestRepository` into `ContestController`
      (existing class — minimal change).
    - In `getContestById` and `getContestDetail`, add
      `proctored = proctoredContestRepo.existsByContestId(id)` and
      include it in the response payload.
    - Do not modify the `Contest` entity.
    - _Requirements: 1.2, 1.6_
  - [x] 2.3 Add the "Proctored" badge to `ContestList.jsx`
    - In `frontend/src/pages/ContestList.jsx`, render a small pill next
      to existing `Live`/`Upcoming`/`Ended` badges when
      `contest.proctored === true`. Use palette token
      `border: 1px solid C.secondary; color: C.secondary` matching the
      existing live-status pill style.
    - _Requirements: 1.7_
  - [x] 2.4 Update `ContestDetail.jsx` to route proctored contests to entry shell
    - In `frontend/src/pages/ContestDetail.jsx`, when
      `contest.proctored === true` and the candidate is registered,
      replace the "Start Solving" `navigate('/problems/...')` call with
      `navigate('/contests/${id}/proctored/entry')`.
    - Add a visible "Proctored" badge to the hero badges row using the
      same pill style as 2.3.
    - _Requirements: 1.7_
  - [x] 2.5 Property test (jqwik) — Property 1 (admin endpoint integration layer)
    - **Property 1: Proctored extension lifecycle**
    - **Validates: Requirements 1.3, 1.4, 1.5, 1.8**

- [x] 3. Consent + preflight shell and entry REST endpoints
  - [x] 3.1 Implement `ProctoringConsentService`
    - Create `proctoring/service/ProctoringConsentService.java` with
      `recordAck(userId, contestId, consentVersion, ip, userAgent)`
      returning the inserted `ProctoringConsentAck`, and
      `hasAck(userId, contestId, consentVersion)` returning a boolean.
    - Reject mismatched `consentVersion` (400) by comparing with
      `proctored_contests.consent_version`.
    - _Requirements: 2.3, 2.5, 2.6_
  - [x] 3.2 Implement `ProctoringSessionService` lifecycle skeleton
    - Create `proctoring/service/ProctoringSessionService.java` with
      `createSession`, `finish`, `quit`, `forceEnd`, `heartbeatTimeout`,
      `closeAllForContest`, `isLocked`, `getActiveSession`. Implement
      every terminating method as a single conditional UPDATE
      (`SET ended_at=NOW(), end_reason=:reason WHERE id=:id AND ended_at IS NULL`)
      so duplicate close attempts are no-ops.
    - Add a `@PostConstruct` hook that runs
      `UPDATE proctoring_sessions SET ended_at=NOW(), end_reason='HEARTBEAT_TIMEOUT' WHERE ended_at IS NULL`
      to finalize sessions left active across a JVM restart.
    - Add a `@Scheduled(fixedDelay=30000)` sweep that calls
      `closeAllForContest(contestId)` for every contest with
      `end_time < NOW()` and an active session (Req 13.5).
    - Maintain the `proctoring:contest:{cid}:active` Valkey set on
      create / close.
    - _Requirements: 2.4, 13.3, 13.4, 13.5, 13.6, 13.7, 13.8, 13.9, 16.3, 24.6_
  - [x] 3.3 Implement `ProctoringEntryController`
    - Create `proctoring/controller/ProctoringEntryController.java` at
      `@RequestMapping("/api/proctoring")` (JWT auth).
    - `GET /contests/{cid}/eligibility` → returns
      `{ registered, proctored, consentAccepted, locked, lockReason, consentVersion }`.
      Reads `contest_registrations`, `proctored_contests`,
      `proctoring_consent_acks`, and `proctoring_sessions`.
    - `POST /contests/{cid}/consent` → records an ack via
      `ProctoringConsentService`. Reads `client_ip` from
      `X-Forwarded-For[0]` and `User-Agent` from header.
    - `POST /contests/{cid}/sessions` → creates a session via
      `ProctoringSessionService.createSession` and mints a fresh
      `wsTicket`; returns
      `{ sessionId, wsTicket, startedAt }`. Returns 409
      `LOCKED_OUT { endReason }`, 409 `ALREADY_ACTIVE { sessionId }`,
      or 403 `CONSENT_MISSING` per `design.md`.
    - `POST /sessions/{id}/ws-ticket` → mints a fresh ticket for
      reconnect (verifies session belongs to caller and is active).
    - _Requirements: 2.3, 2.5, 2.6, 3.4, 13.3, 13.4, 13.9, 16.1, 16.3_
  - [x] 3.4 Build the candidate entry shell pages
    - Add `frontend/src/proctoring/pages/ProctoredContestEntry.jsx`
      that calls `GET /eligibility` then renders either:
      consent screen (Req 2.1, 2.2), preflight check
      (`PreflightCheck.jsx`, Req 3.x), terminal screen
      (lockout — Req 13.9, 24.6), or transitions to arena.
    - Add `frontend/src/proctoring/components/ConsentDialog.jsx`
      listing every monitored signal (fullscreen, tab/focus, copy/paste,
      webcam-based face detection, on-event screenshot capture).
    - Add `frontend/src/proctoring/components/PreflightCheck.jsx`
      sequencing the five checks (webcam, stream + face, fullscreen API,
      AI model, WS reachability) with retry per step.
    - Add `frontend/src/proctoring/pages/ProctoredContestArena.jsx`
      placeholder rendering the existing problem-solve UI; full
      proctoring overlay is wired in tasks 6, 8, 9.
    - Add lazy-loaded routes in `frontend/src/App.jsx` for
      `/contests/:id/proctored/entry`, `/proctored/arena`,
      `/proctored/terminated`, plus admin
      `/admin/proctoring/contests/:id` and
      `/admin/proctoring/sessions/:id` (placeholders, real components
      land in tasks 10–11).
    - Use the Practice/Contest palette tokens from `ContestDetail.jsx`
      and viewport-gate at 1024px via `useResponsive` per Req 23.9.
    - _Requirements: 2.1, 2.2, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5, 23.8, 23.9_
  - [x] 3.5 Add the `proctoringApi.js` REST client
    - Create `frontend/src/proctoring/services/proctoringApi.js` with
      methods `eligibility(cid)`, `consent(cid, version)`,
      `createSession(cid)`, `mintWsTicket(sid)`, `finish(sid)`,
      `quit(sid)`, `uploadScreenshot(formData)` — built on top of the
      existing `services/api.js` axios instance.
    - _Requirements: 2.3, 13.3, 13.6, 13.8, 24.2, 24.5_
  - [x] 3.6 Property tests (jqwik) — Property 2 + Property 3
    - **Property 2: Consent ack round-trip and skip-on-existing**
    - **Validates: Requirements 2.3, 2.5, 2.6**
    - **Property 3: Session lifecycle, close reasons, and lockout**
    - **Validates: Requirements 13.4, 13.5, 13.6, 13.7, 13.8, 13.9, 19.3, 24.2, 24.5, 24.6**

- [x] 4. WebSocket skeleton (handler + ticket + handshake + heartbeat)
  - [x] 4.1 Implement `ProctoringWsTicketService`
    - Create `proctoring/ws/ProctoringWsTicketService.java` with
      `mint(userId)` and `consume(ticket)` using Valkey key prefix
      `proctoring:ws:ticket:` (32-byte hex, 60 s TTL, atomic `GETDEL`,
      fail-closed on Valkey errors). Mirror the existing
      `WsTicketService` but with the proctoring prefix so a ticket
      cannot be replayed against the compiler WS.
    - _Requirements: 9.2, 16.2, 16.3_
  - [x] 4.2 Implement `ProctoringWebSocketConfig` + handshake interceptor
    - Create `proctoring/ws/ProctoringWebSocketConfig.java` annotated
      with `@Configuration @EnableWebSocket` registering the handler at
      `/api/proctoring/ws` with
      `setAllowedOriginPatterns(allowedOrigins.split(","))` (no
      wildcard — read from `APP_ALLOWED_ORIGINS` env via existing
      `SecurityConfig` source).
    - Add a `HandshakeInterceptor` that captures
      `X-Forwarded-For[0]` into `attrs["ip"]`, calls
      `ticketService.consume`, loads the session, verifies
      `session.userId == ticketUserId` and `session.endedAt == null`,
      and stores `userId`, `sessionId`, `contestId` in attrs. Reject
      mismatches with HTTP 401 at the upgrade.
    - _Requirements: 9.2, 9.3, 16.2, 16.3_
  - [x] 4.3 Implement `ProctoringSessionRegistry`
    - Create `proctoring/ws/ProctoringSessionRegistry.java` holding
      `ConcurrentHashMap<Long, WebSocketSession>` plus
      `register`, `unregister`, `isConnected`, `send`,
      `terminate(sessionId, reason, endReason, closeCode)`,
      `connectedSessionIds`. `register` rejects a duplicate by closing
      the new connection with code 4409 and leaving the existing one
      intact (Req 9.4). On register/unregister, set/delete the Valkey
      `proctoring:session:{sid}:connected` projection.
    - _Requirements: 9.4, 10.2, 13.7, 18.3_
  - [x] 4.4 Implement `ProctoringWebSocketHandler`
    - Create `proctoring/ws/ProctoringWebSocketHandler.java` extending
      `TextWebSocketHandler`. In `afterConnectionEstablished` register
      the session, send the initial `RISK_UPDATE` from current Valkey
      state, and schedule a per-session heartbeat-timeout task on a
      shared `ScheduledExecutorService` with delay
      `heartbeatTimeoutSeconds`.
    - In `handleTextMessage` parse the JSON `type` field and dispatch:
      `EVENT` → forward to `ProctoringEventService.ingest` (added in
      task 5), respond with `EVENT_ACK { client_correlation_id, event_id }`;
      `HEARTBEAT` → reset timeout task, respond `HEARTBEAT_ACK { server_time }`;
      `FINISH` → call `sessionService.finish`, push
      `SESSION_TERMINATED { reason, end_reason: SELF_FINISHED }`,
      close 1000;
      `QUIT` → call `sessionService.quit`, push
      `SESSION_TERMINATED { reason, end_reason: SELF_QUIT }`, close
      1000.
    - On heartbeat timeout: emit a `HEARTBEAT_TIMEOUT` event into the
      session via `eventService.ingest`, push `SESSION_TERMINATED`,
      close 4408. Refresh
      `proctoring:session:{sid}:lastEventAt` (90 s TTL) on every
      inbound frame.
    - Add JSON DTOs under `proctoring/ws/frame/` for `EventFrame`,
      `HeartbeatFrame`, `RiskUpdateFrame`, `WarningFrame`,
      `SessionTerminatedFrame`, `BufferAckFrame`,
      `RateLimitExceededFrame`, `EventAckFrame`.
    - Use `CloseStatus(4000+offset, reason)` for codes 4003, 4401,
      4403, 4408, 4409, 4413, 4500.
    - _Requirements: 9.1, 9.3, 9.5, 9.6, 9.7, 9.8, 10.1, 10.2, 13.6, 13.7, 13.8, 24.2, 24.5_
  - [x] 4.5 Build `useProctoringSocket` hook (initial)
    - Create `frontend/src/proctoring/hooks/useProctoringSocket.js`
      that opens
      `wss://${apiHost}/api/proctoring/ws?ticket=…&sessionId=…`,
      sends `HEARTBEAT` every `heartbeatIntervalSeconds`, surfaces
      inbound frames via callbacks, and handles close codes 4003,
      4408, 4409. Add a `sendEvent(eventType, payload)` helper that
      wraps the frame as `{ type: 'EVENT', client_correlation_id, … }`
      and resolves with the `event_id` from the matching `EVENT_ACK`.
    - Add exponential-backoff reconnect skeleton (full replay logic
      lands in task 9).
    - _Requirements: 9.1, 9.5, 9.7, 10.1, 10.4, 11.5_
  - [x] 4.6 Property test (jqwik) — Property 5 + Property 6 (auth gate)
    - **Property 5: Server-pushed frames map one-to-one to triggering actions**
    - **Validates: Requirements 10.1, 10.2, 10.3, 12.5**
    - **Property 6: Auth gate is total**
    - **Validates: Requirements 9.2, 9.3, 9.4, 15.4, 16.1, 16.2, 16.3, 16.4, 16.5**

- [x] 5. Risk scoring engine + Valkey counters + RISK_UPDATE pushback
  - [x] 5.1 Implement `RiskScoringEngine`
    - Create `proctoring/service/RiskScoringEngine.java` with
      `weightFor(eventType)` (returns the `proctoring.weights` value,
      or 0 for unknown with a single warning log per type per JVM
      lifetime), pure `bandFor(score)`,
      `applyDelta(sessionId, delta)` that runs `INCRBY` on
      `proctoring:session:{sid}:score` and compares with
      `proctoring:session:{sid}:band`, `persistBand(sessionId, band)`
      that issues `UPDATE proctoring_sessions SET risk_band=:b, flagged=(b=='HIGH') WHERE id=:id`,
      and `rescore(sessionId)` that replays
      `findBySessionIdOrderByServerTimestampAsc` and rewrites both the
      DB row and Valkey counters.
    - Return a `RiskUpdate(int newScore, RiskBand oldBand, RiskBand newBand, boolean changed)` record from `applyDelta`.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 18.1, 22.2, 22.3_
  - [x] 5.2 Implement `ProctoringEventService.ingest`
    - Create `proctoring/service/ProctoringEventService.java` with
      `ingest(sessionId, frame, replayed)` returning the inserted
      `event_id`. Order: weight lookup → INSERT
      `proctoring_events` → `riskEngine.applyDelta` → on band change
      `riskEngine.persistBand` + broadcast → return id.
    - For replayed frames, set `replayed=TRUE` and check the soft
      dedup key `proctoring:dedup:{sid}:{ts_ms}:{type}` via `SETNX EX 600`;
      if present, persist the row but force `score_delta=0`.
    - _Requirements: 9.5, 9.6, 11.2, 11.3, 12.4, 12.7, 14.1, 22.2, 22.3_
  - [x] 5.3 Wire band-change broadcast (candidate WS + admin SSE)
    - Inject `ProctoringSessionRegistry` into `ProctoringEventService`;
      on `RiskUpdate.changed == true`, push
      `RISK_UPDATE { risk_score, risk_band }` to the candidate WS via
      registry, and publish a `RISK_BAND_CHANGED` event onto the admin
      SSE bus (a simple in-process `ApplicationEventPublisher` event
      consumed by the admin SSE bridge added in task 10).
    - _Requirements: 10.1, 12.5, 12.6, 18.3_
  - [x] 5.4 Add the Valkey-to-DB flush job
    - Create a `@Scheduled(fixedDelay=5000)` method in
      `RiskScoringEngine` (or a sibling `RiskScoreFlusher`) that walks
      `connectedSessionIds()`, reads
      `proctoring:session:{sid}:score`/`:band`, and persists them in a
      single batch UPDATE so the DB projection lags ≤ 5 s.
    - _Requirements: 18.1, 18.2_
  - [x] 5.5 Property test (jqwik) — Property 4
    - **Property 4: Risk engine is deterministic, monotonic, and band-consistent**
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 18.1, 18.2, 22.2, 22.3**

- [x] 6. Browser event detectors (fullscreen, tab/focus, copy/paste)
  - [x] 6.1 Implement `useFullscreen` + `FullscreenGuard`
    - Add `frontend/src/proctoring/hooks/useFullscreen.js` wrapping
      `Element.requestFullscreen` / `fullscreenchange`. Target is the
      contest root `<div ref={arenaRef}>` (resolved Q2: contest root,
      not `document.documentElement`).
    - Add `frontend/src/proctoring/components/FullscreenGuard.jsx`
      that listens for fullscreen-exit, emits `FULLSCREEN_EXIT` via
      `useProctoringSocket`, displays a blocking modal pausing the
      editor, and reinvokes `requestFullscreen` on user gesture.
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_
  - [x] 6.2 Implement `useTabFocusMonitor`
    - Add `frontend/src/proctoring/hooks/useTabFocusMonitor.js` that
      attaches `visibilitychange`, `blur`, `focus` listeners. Emit
      `TAB_SWITCH` on `document.hidden`, `WINDOW_BLUR` on window blur
      while visible, `FOCUS_RESTORED { duration_ms }` on restore.
    - Set the editor read-only and block submission while
      `document.hidden` is true.
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  - [x] 6.3 Implement `useCopyPasteBlocker`
    - Add `frontend/src/proctoring/hooks/useCopyPasteBlocker.js` that
      attaches `copy`, `cut`, `paste`, `contextmenu` listeners on the
      contest UI root, calls `preventDefault`, and emits
      `COPY_ATTEMPT` / `CUT_ATTEMPT` / `PASTE_ATTEMPT` /
      `CONTEXT_MENU_ATTEMPT`. Apply CSS `user-select: none` to
      non-editor regions.
    - Detect Monaco editor target via
      `event.target.closest('.monaco-editor')` and allow in-editor
      copy/paste without emitting an event (Req 6.5).
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [x] 6.4 Wire the three detector hooks into `ProctoredContestArena`
    - Update `frontend/src/proctoring/pages/ProctoredContestArena.jsx`
      to mount `FullscreenGuard`, `useTabFocusMonitor`, and
      `useCopyPasteBlocker` once the WebSocket is connected. Wire
      emitted events through `useProctoringSocket.sendEvent` so they
      stream over the WS.
    - Render the existing problem-solve UI as the arena content
      (reuse `ProblemSolve.jsx` content via composition, or import its
      hooks).
    - _Requirements: 4.5, 5.5, 6.1_
  - [x] 6.5 Property test (fast-check) — Property 9
    - **Property 9: Browser event detectors emit one event per real transition**
    - **Validates: Requirements 4.3, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.5**

- [x] 7. Screenshot upload pipeline + retention sweep
  - [x] 7.1 Implement `ProctoringScreenshotService`
    - Create `proctoring/service/ProctoringScreenshotService.java`
      with `upload(sessionId, eventId, capturedAt, mimeType, bytes, uploadingUserId)`
      and `serveAdmin(screenshotId)`. Implement the validation order
      from `design.md` exactly: ownership → active → MIME whitelist →
      byte size → magic-byte sniff (`FF D8 FF` JPEG, `89 50 4E 47 0D 0A 1A 0A` PNG)
      → event FK → rate limit (added in task 13) → disk write →
      INSERT row.
    - Path is built only from validated `Long` values:
      `Paths.get("uploads/proctoring/sessions", String.valueOf(sessionId), eventId + ".jpg")`.
      Use `Files.createDirectories(parent)` + `Files.write(path, bytes, CREATE, WRITE, TRUNCATE_EXISTING)`.
    - _Requirements: 8.4, 8.5, 8.6, 14.2, 14.3, 17.4, 17.5_
  - [x] 7.2 Implement `ProctoringScreenshotController`
    - Create `proctoring/controller/ProctoringScreenshotController.java`
      with `POST /api/proctoring/screenshots`
      (`consumes=MULTIPART_FORM_DATA_VALUE`, JWT) accepting
      `session_id`, `event_id`, `captured_at`, `file` and delegating
      to `ProctoringScreenshotService.upload`. Return
      `201 { screenshotId, byteSize }`. Map service exceptions to
      403/413/415/429.
    - `GET /api/admin/proctoring/sessions/{sid}/screenshots/{shotId}`
      (`hasRole('ADMIN')`) streams bytes via `InputStreamResource`
      with `Cache-Control: private, no-store`. Verify
      `row.session_id == sid` and file existence on disk; return 404
      if the file was already purged.
    - _Requirements: 8.3, 8.4, 8.5, 14.2, 14.4, 15.4_
  - [x] 7.3 Implement `ProctoringRetentionJob`
    - Create `proctoring/service/ProctoringRetentionJob.java` with
      `@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")` method
      `purge()` that iterates
      `findByCapturedAtBefore(now-30d, PageRequest.of(0,500))` deleting
      files via `Files.deleteIfExists` (per-row `try/catch`), then
      `deleteAllInBatch`, then
      `eventRepo.deleteByServerTimestampBefore(now-30d)`. Log
      `Proctoring retention purge: {N} screenshots, {M} events`.
    - _Requirements: 21.2, 21.3, 21.4_
  - [x] 7.4 Harden static path exposure in `WebConfig` and `SecurityConfig`
    - Replace the broad `addResourceHandler("/uploads/**")` mapping in
      `WebConfig` with explicit per-prefix handlers (e.g.
      `/uploads/profile-photos/**` only) so
      `/uploads/proctoring/**` is never served statically.
    - In `SecurityConfig` add
      `requestMatchers("/uploads/proctoring/**").denyAll()` and
      `requestMatchers("/uploads/profile-photos/**").permitAll()`.
    - Verify the existing profile-photo flow still works.
    - _Requirements: 14.3, 15.4, 16.1_
  - [x] 7.5 Property tests (jqwik) — Property 8 + Property 12
    - **Property 8: Event ingestion and screenshot upload are round-trip faithful**
    - **Validates: Requirements 8.1, 8.3, 8.6, 8.7, 9.5, 9.6, 14.1, 14.2, 14.3, 18.2**
    - **Property 12: Retention purges exactly at 30 days**
    - **Validates: Requirements 21.2, 21.3, 21.4**

- [x] 8. Browser AI (MediaPipe + face detector state machine + screenshot trigger)
  - [x] 8.1 Add `@mediapipe/tasks-vision` and ship wasm + model assets
    - Add `@mediapipe/tasks-vision` to
      `frontend/package.json` `dependencies` (pinned exact version) and
      run `npm install`.
    - Commit MediaPipe wasm runtime files under
      `frontend/public/wasm/` and the
      `blaze_face_short_range.tflite` model under
      `frontend/public/models/` so they load same-origin (matches the
      self-hosted fonts pattern in `frontend/public/fonts/`).
    - _Requirements: 7.1, 7.2_
  - [x] 8.2 Implement `faceDetector.worker.js`
    - Create `frontend/src/proctoring/workers/faceDetector.worker.js`
      that initialises a MediaPipe `FaceDetector` (paths
      `/wasm/`, `/models/blaze_face_short_range.tflite`,
      `runningMode: 'IMAGE'`) and responds to
      `{ type: 'frame', bitmap, frameId }` with
      `{ type: 'result', frameId, faceCount, confidence }`.
      `bitmap.close()` after each detection.
    - _Requirements: 7.1, 7.2_
  - [x] 8.3 Implement detector plugin classes
    - Create `frontend/src/proctoring/detectors/Detector.js` (base
      class with `init`, `process`, `dispose`),
      `DetectorRegistry.js` (`register`, `initAll`, `processFrame`,
      `disposeAll`), and `FaceDetector.js` (the only MVP detector;
      delegates inference to the worker and returns
      `[{ event_type, payload }]`).
    - _Requirements: 7.7, 22.1, 22.2_
  - [x] 8.4 Implement `useFaceDetector` hook with state machine
    - Create `frontend/src/proctoring/hooks/useFaceDetector.js` that
      requests `getUserMedia({ video: {320×240, 15 fps}, audio: false })`,
      initialises `DetectorRegistry`, runs an inference tick at
      `aiInferenceIntervalMs` via `OffscreenCanvas.transferToImageBitmap`,
      and implements the state machine (`ONE_FACE`,
      `NO_FACE_PENDING`, `NO_FACE`, `MULTIPLE_FACES`) emitting
      `NO_FACE`, `MULTIPLE_FACES`, `FACE_STATE_RESTORED`,
      `WEBCAM_STREAM_LOST`, `WEBCAM_PERMISSION_DENIED`.
    - On every screenshot-triggering event (`TAB_SWITCH`,
      `FULLSCREEN_EXIT`, `MULTIPLE_FACES`, `NO_FACE`,
      `WEBCAM_STREAM_LOST`), capture a JPEG via
      `canvas.toBlob('image/jpeg', screenshotJpegQuality)` at
      `screenshotMaxWidth × screenshotMaxHeight`, await the
      `EVENT_ACK` for the originating event, then
      `proctoringApi.uploadScreenshot(formData)`.
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.7_
  - [x] 8.5 Wire `useFaceDetector` into `ProctoredContestArena`
    - Update `ProctoredContestArena.jsx` to mount `useFaceDetector`
      and a small `WebcamPreview.jsx` corner-pip preview. Block the
      arena UI with a modal while
      `WEBCAM_STREAM_LOST` or `WEBCAM_PERMISSION_DENIED` is active
      (Req 7.6, 3.2).
    - _Requirements: 3.2, 7.6, 8.1_
  - [x] 8.6 Property tests (fast-check) — Property 10 + Property 13
    - **Property 10: Face-detector state machine emits one event per real transition**
    - **Validates: Requirements 7.2, 7.3, 7.4, 7.5, 7.6**
    - **Property 13: Detector plugin extensibility is type-agnostic**
    - **Validates: Requirements 7.7, 22.1, 22.2, 22.3, 22.4**

- [x] 9. IndexedDB offline buffer + replay on reconnect
  - [x] 9.1 Implement IndexedDB schema and helpers
    - Create `frontend/src/proctoring/services/eventBuffer.js` that
      opens an IndexedDB DB `proctoring`, store `events` keyed on
      autoincrement id with index on `client_timestamp`. Provide
      `add(event)`, `peekOldest(n)`, `deleteByIds(ids)`,
      `count()`, `purgeAll()`. Cap at `maxOfflineEvents` (default
      1000) by deleting the oldest rows on overflow and emitting a
      single `BUFFER_OVERFLOW` flag for the next reconnect.
    - _Requirements: 11.1, 11.2, 11.4_
  - [x] 9.2 Implement `useEventBuffer` and integrate with `useProctoringSocket`
    - Create `frontend/src/proctoring/hooks/useEventBuffer.js`. Update
      `useProctoringSocket` so that:
      while disconnected, every event from `sendEvent` is appended to
      the buffer instead of sent;
      on reconnect, mint a fresh ws ticket via `mintWsTicket`,
      drain the buffer in `client_timestamp` order at ≤ 100 events/s
      with `replayed: true` (and emit `BUFFER_OVERFLOW` once if the
      flag is set), and on receipt of `BUFFER_ACK` purge the replayed
      ids from IndexedDB before switching to live mode.
    - Implement exponential backoff `[1s, 2s, 4s, 8s, 16s, 30s, 30s, …]`
      with 50% jitter, capped at 30 s.
    - _Requirements: 11.1, 11.2, 11.3, 11.4_
  - [x] 9.3 Server-side replay dedup + `BUFFER_ACK` emission
    - Confirm `ProctoringEventService.ingest` honours the
      `proctoring:dedup:{sid}:{ts_ms}:{type}` key (added in 5.2) and
      emit a single `BUFFER_ACK { replayed_count }` from the WS handler
      after the last `replayed=true` frame in a contiguous batch.
      Detect end-of-batch by a 1 s idle window after the final replay.
    - _Requirements: 11.2, 11.3_
  - [x] 9.4 Implement `DisconnectionGuard` modal and `maxOfflineSeconds` cap
    - Add `frontend/src/proctoring/components/DisconnectionGuard.jsx`
      that mounts in `ProctoredContestArena` and shows a blocking
      modal once disconnect duration exceeds
      `maxOfflineSeconds` (default 60). While the modal is up the
      editor is paused and submissions are blocked.
    - _Requirements: 11.5_
  - [x] 9.5 Property test (fast-check) — Property 11
    - **Property 11: Offline buffer and replay preserve order, cap, and idempotency**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4**

- [x] 10. Admin live dashboard + drill-down + force-end + warning + audit (SSE)
  - [x] 10.1 Wire admin SSE channel using existing patterns
    - Reuse `SseTicketService` and `SseEmitterRegistry` (existing in
      repo) for the proctoring admin channel. Add
      `POST /api/admin/proctoring/contests/{cid}/stream/ticket`
      (admin-only) to mint a ticket, and
      `GET /api/admin/proctoring/contests/{cid}/stream` that consumes
      the ticket and registers an `SseEmitter` keyed by `cid`.
    - Add a `@Component` listener that consumes the
      `RISK_BAND_CHANGED` ApplicationEvent published in 5.3 plus
      `SESSION_STARTED` and `SESSION_ENDED` events emitted by
      `ProctoringSessionService`, and fans them out to the matching
      `cid` emitters.
    - _Requirements: 12.5, 15.1, 15.2, 18.3_
  - [x] 10.2 Implement `ProctoringAdminController`
    - Create `proctoring/controller/ProctoringAdminController.java`
      at `@RequestMapping("/api/admin/proctoring")`,
      `@PreAuthorize("hasRole('ADMIN')")`.
    - `GET /contests/{cid}/sessions?flagged=…` builds the live list
      from Valkey: `SMEMBERS proctoring:contest:{cid}:active` →
      `MGET` for `score`, `band`, `lastEventAt`, `connected` per sid →
      DB join for `username`. Compute `connected` as
      `registry.isConnected(sid) AND lastEventAt < 90s` (per resolved
      Q6).
    - `GET /sessions/{id}` returns
      `{ session, events, screenshots, submissions }`. Submissions are
      pulled by `(user_id, contest_id, submitted_at BETWEEN session.started_at AND COALESCE(session.ended_at, NOW()))` per
      resolved Q4.
    - `POST /sessions/{id}/force-end` calls `sessionService.forceEnd`,
      writes a `proctoring_admin_audit` row, and pushes
      `SESSION_TERMINATED { ADMIN_FORCED }` via registry close 4003.
    - `POST /sessions/{id}/warn` writes a `WARNING` audit row and pushes
      a `WARNING { admin_id, message, acted_at }` frame; does not close
      the session.
    - `POST /sessions/{id}/rescore` calls `riskEngine.rescore` and
      returns `{ riskScore, riskBand, flagged }`.
    - _Requirements: 10.2, 10.3, 12.8, 15.1, 15.3, 15.4, 15.5, 15.6, 15.7, 19.4, 21.5_
  - [x] 10.3 Wire `ProctoringAdminAudit` writes
    - Inject `ProctoringAdminAuditRepository` into the admin
      controller; persist
      `(admin_id, session_id, action, acted_at, reason)` rows on every
      force-end and warning. The table is append-only (no DELETE
      endpoint).
    - _Requirements: 15.7, 21.5_
  - [x] 10.4 Build `AdminProctoringDashboard.jsx`
    - Create
      `frontend/src/proctoring/pages/admin/AdminProctoringDashboard.jsx`
      with a live table (columns `username`, `risk_score`, `risk_band`,
      `flagged`, `last_event_at`, `connected`), a
      `flagged=true` filter toggle, and per-row buttons opening
      drill-down or invoking force-end / warn (with confirmation).
      Subscribe to the SSE stream via `EventSource` after minting a
      ticket; re-render rows on `RISK_BAND_CHANGED` /
      `SESSION_STARTED` / `SESSION_ENDED`.
    - Reuse `RiskBadge.jsx` (LOW=success, MEDIUM=warning, HIGH=error)
      under `proctoring/components/`.
    - _Requirements: 15.1, 15.2, 15.5, 15.6_
  - [x] 10.5 Build `AdminProctoringSession.jsx` drill-down
    - Create
      `frontend/src/proctoring/pages/admin/AdminProctoringSession.jsx`
      rendering the chronological event log (`EventTimeline.jsx`),
      screenshot grid (`ScreenshotGallery.jsx` calling the streaming
      admin endpoint with a JWT-bearing `<img>` blob fetcher), risk
      score chart, submissions list, and force-end / warn / rescore
      controls.
    - Render any registered `event_type` generically (no per-type code
      branches) per Req 22.4.
    - _Requirements: 15.3, 15.4, 15.6, 15.7, 19.4, 22.4_
  - [x] 10.6 Property tests (jqwik) — Property 14 + Property 16
    - **Property 14: Admin actions write exactly one audit row each**
    - **Validates: Requirements 15.7, 21.5**
    - **Property 16: Admin flagged filter is exact**
    - **Validates: Requirements 15.1, 15.5**

- [x] 11. Lockout enforcement + submission integration + terminal screen
  - [x] 11.1 Enforce lockout on session creation
    - In `ProctoringEntryController.createSession` (added in 3.3),
      consult `sessionService.isLocked(contestId, userId)` first and
      return 409 `LOCKED_OUT { endReason }` if true.
    - In the eligibility response, include
      `locked` and `lockReason` so the frontend entry page can render
      the terminal screen directly.
    - _Requirements: 13.9, 24.6_
  - [x] 11.2 Add `proctoringSessionId` field to `SubmissionJob` + gate in `SubmissionService`
    - Add a nullable `Long proctoringSessionId` field to
      `SubmissionJob` (mirroring how `duelId` was added).
    - In `SubmissionService.submitCodeAsync`, when the candidate is
      currently inside a proctoring session, set
      `proctoringSessionId`. Before enqueueing call
      `proctoringSessionService.isLocked(contestId, userId)` and throw
      `ProctoringForbiddenException("LOCKED_OUT")` if true. Per
      resolved Q4, do NOT add a column to `submissions`.
    - _Requirements: 19.1, 19.2, 19.3_
  - [x] 11.3 Wire proctoring exceptions into `GlobalExceptionHandler`
    - Map `ProctoringNotFoundException → 404`,
      `ProctoringForbiddenException → 403`,
      `ProctoringStateConflictException → 409` with
      `{ error, message, …payload }` body shape. Reuse the existing
      `MessageResponse` pattern.
    - _Requirements: 13.9, 16.3, 19.3_
  - [x] 11.4 Build `ProctoredContestTerminated.jsx`
    - Create
      `frontend/src/proctoring/pages/ProctoredContestTerminated.jsx`
      that reads the `endReason` from route state or eligibility
      response and renders the matching message
      (`SELF_QUIT`, `ADMIN_FORCED`, `HEARTBEAT_TIMEOUT`,
      `SELF_FINISHED`, `CONTEST_ENDED`). Never offer re-entry for the
      first three.
    - _Requirements: 13.9, 24.6_
  - [x] 11.5 Property test (jqwik) — Property 15
    - **Property 15: Submission integration is unchanged off-path and rejected on-lockout**
    - **Validates: Requirements 19.1, 19.2, 19.3, 19.4**

- [x] 12. Candidate Finish / Quit controls
  - [x] 12.1 Build `FinishQuitButtons.jsx` + confirmation dialogs
    - Create
      `frontend/src/proctoring/components/FinishQuitButtons.jsx`
      with two visually distinct buttons (Finish = primary, Quit =
      destructive). Each opens a confirmation dialog; the Quit dialog
      warns that quitting is permanent.
    - _Requirements: 24.1, 24.3, 24.4_
  - [x] 12.2 Implement `ProctoringFinishQuitController`
    - Create `proctoring/controller/ProctoringFinishQuitController.java`
      with `POST /api/proctoring/sessions/{id}/finish` and
      `POST /api/proctoring/sessions/{id}/quit` (JWT). Each calls
      `sessionService.finish` / `sessionService.quit` and, if the WS
      is connected, calls `registry.terminate` to push
      `SESSION_TERMINATED` and close cleanly.
    - Return `200 { endedAt, endReason }` or `409 ALREADY_ENDED`.
    - _Requirements: 13.6, 13.8, 24.2, 24.5_
  - [x] 12.3 Send `FINISH` / `QUIT` frames over the WebSocket
    - Update `useProctoringSocket` to support sending `FINISH` and
      `QUIT` frames; on receiving the corresponding
      `SESSION_TERMINATED` frame, navigate to
      `/contests/${id}/proctored/terminated` with `endReason` in
      route state.
    - _Requirements: 10.2, 10.4, 13.6, 13.8, 24.2, 24.5_
  - [x] 12.4 Mount Finish / Quit into `ProctoredContestArena`
    - Place `FinishQuitButtons` in the arena bottom bar. On confirm,
      prefer the WS frame path; fall back to the REST endpoint if the
      WS is currently disconnected (e.g. inside `DisconnectionGuard`).
    - _Requirements: 24.1, 24.2, 24.3, 24.5_

- [x] 13. Rate limits and payload-size cap
  - [x] 13.1 Implement `ProctoringRateLimiter`
    - Create `proctoring/service/ProctoringRateLimiter.java` with
      `allowEventFrame(sid)` (sorted-set sliding window
      `proctoring:rl:events:{sid}`, admit if cardinality
      ≤ `eventRateLimitPerSecond × 10`; on rejection
      use `proctoring:rl:events:{sid}:notified` so at most one
      `RATE_LIMIT_EXCEEDED` notification is emitted per 10 s window)
      and `allowScreenshotUpload(sid)` (`INCR proctoring:rl:shots:{sid} EX 60 NX`,
      admit if value ≤ `screenshotRateLimitPerMinute`).
    - Both fail open on Valkey errors per repo convention.
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_
  - [x] 13.2 Wire the limiter into the WS handler and screenshot controller
    - In `ProctoringWebSocketHandler`, gate every inbound `EVENT`
      frame through `allowEventFrame`. On rejection drop the frame and
      emit a single `RATE_LIMIT_EXCEEDED { window_seconds: 10 }` event
      into the session.
    - In `ProctoringScreenshotController`, gate uploads through
      `allowScreenshotUpload`. On rejection respond `429` with
      `Retry-After: 60`.
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_
  - [x] 13.3 Enforce inbound payload size cap
    - In `ProctoringWebSocketHandler.handleTextMessage`, reject any
      frame whose UTF-8 byte length exceeds `maxEventBytes` (default
      4096) by closing the connection with code 4413 (no per-frame
      ACK).
    - _Requirements: 17.6_
  - [x] 13.4 Property test (jqwik) — Property 7
    - **Property 7: Rate limits and size caps reject the right inputs**
    - **Validates: Requirements 8.4, 8.5, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6**

- [x] 14. End-to-end smoke verification
  - [x] 14.1 Author the proctoring smoke test script
    - Add `bench_proctoring.sh` in the repo root following the
      `bench_full.sh` style (curl-based, single bash file). The script
      provisions one `UPCOMING` proctored contest via the admin API,
      registers 5 candidate users, walks each one through:
      consent → preflight stub → session create → ws-ticket → open
      WebSocket → send a deterministic suspicious event sequence
      (`TAB_SWITCH`, `WINDOW_BLUR`, `FULLSCREEN_EXIT`,
      `MULTIPLE_FACES`, `NO_FACE`) → upload one screenshot per
      qualifying event → heartbeat for 60 s → finish or quit.
    - For one of the 5, exercise admin force-end via
      `POST /sessions/{id}/force-end` and assert the WS closes 4003,
      a `proctoring_admin_audit` row exists, and a re-entry attempt
      returns 409 `LOCKED_OUT`.
    - For another, drop the WS for 30 s mid-stream, replay 20 buffered
      events, and assert the dedup + `BUFFER_ACK` path is exercised
      (use a small Node helper invoked from bash for the WS frames).
    - The script asserts (via `psql` queries against the live DB):
      `proctoring_sessions` rows have the expected `risk_score` /
      `risk_band` / `flagged`; `proctoring_events` row counts match
      the script's event total; `proctoring_screenshots` rows exist
      with on-disk files at `storage_ref`; admin force-end /
      warning audits are recorded.
    - Exit non-zero on any assertion failure with a clear message.
    - _Requirements: 1.3, 2.3, 8.1, 8.6, 9.5, 10.2, 11.2, 11.3, 12.5, 13.6, 13.7, 13.8, 13.9, 15.7, 17.4, 18.4, 19.3, 21.5, 24.5_
  - [x] 14.2 Author the deploy + run wrapper
    - Add a top-level `deploy_proctoring_smoke.sh` that runs
      `./mvnw -q -DskipTests clean package`, copies the WAR to the VM
      pattern (`cp target/codecombat2026-0.0.1-SNAPSHOT.war ~/app.war`)
      with a guard so it only runs when `DEPLOY_ENV=vm`, restarts
      `codecombat` via `sudo systemctl restart codecombat`, waits for
      `/actuator/health` to return `UP`, and then invokes
      `bench_proctoring.sh`. Locally (without `DEPLOY_ENV=vm`) the
      wrapper just runs the Spring app via `./mvnw spring-boot:run` in
      the background and points the smoke test at it.
    - _Requirements: 18.4_
  - [x] 14.3 Integration test — schema columns + indexes (information_schema)
    - Add a `@SpringBootTest` that queries
      `information_schema.columns` and `pg_indexes` to assert every
      column and index promised by V7 actually exists (Req 13.1, 14.1,
      14.2, 14.4, 14.5, 14.6).
  - [x] 14.4 Integration test — admin API requires `ROLE_ADMIN`
    - Add an MVC test exercising every `/api/admin/proctoring/**`
      endpoint with a non-admin JWT and assert 403 (Req 16.4).

## Notes

- Sub-tasks marked with `*` are **OPTIONAL**. The user's directive is
  to skip optional tests and rely on the smoke test in task 14 as the
  verification gate. The optional tasks are: every property test
  (1.5, 2.5, 3.6, 4.6, 5.5, 6.5, 7.5, 8.6, 9.5, 10.6, 11.5, 13.4) and
  the two integration tests (14.3, 14.4).
- The mandatory shipping path is: 1.1 → 1.2 → 1.3 → 1.4 → 2.1 → 2.2 →
  2.3 → 2.4 → 3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 4.1 → 4.2 → 4.3 → 4.4 →
  4.5 → 5.1 → 5.2 → 5.3 → 5.4 → 6.1 → 6.2 → 6.3 → 6.4 → 7.1 → 7.2 →
  7.3 → 7.4 → 8.1 → 8.2 → 8.3 → 8.4 → 8.5 → 9.1 → 9.2 → 9.3 → 9.4 →
  10.1 → 10.2 → 10.3 → 10.4 → 10.5 → 11.1 → 11.2 → 11.3 → 11.4 →
  12.1 → 12.2 → 12.3 → 12.4 → 13.1 → 13.2 → 13.3 → 14.1 → 14.2.
- Each task references specific requirements via `_Requirements: …_`
  for traceability against `requirements.md`.
- The 14 top-level tasks mirror the implementation roadmap in
  `design.md` step-for-step.
- Open design questions Q1–Q6 are resolved per the user's
  "do the recommended" directive: raw `WebSocketHandler`, contest-root
  fullscreen target, no force-end-after-N-exits, no
  `proctoring_session_id` column on `submissions`, SSE for the admin
  dashboard, combined registry + Valkey `lastEventAt` signal for
  `connected`.
- Frontend is JavaScript only (Req 23.8), desktop only at ≥ 1024px
  (Req 23.9), JWT in REST + ticket on WS handshake (Req 16.2).
- Property-based tests, when run, use jqwik (backend) at
  `@Property(tries = 200)` and fast-check (frontend) at
  `numRuns: 200`, with comment headers tagging the design property as
  documented in `design.md` § Testing Strategy.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.4", "8.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "1.5", "4.1", "4.3", "8.2", "8.3", "9.1", "11.3", "13.3", "14.4"] },
    { "id": 3, "tasks": ["2.1", "2.2", "3.1", "3.2", "5.1", "6.1", "6.2", "6.3", "7.1", "7.3", "7.4", "10.3", "11.2", "13.1", "14.3"] },
    { "id": 4, "tasks": ["2.5", "3.3", "5.2", "5.4", "7.2", "10.1", "11.1", "12.2", "13.2"] },
    { "id": 5, "tasks": ["2.3", "2.4", "3.5", "3.6", "4.2", "5.5", "6.5", "7.5", "10.2", "12.1"] },
    { "id": 6, "tasks": ["3.4", "4.4", "11.4", "11.5"] },
    { "id": 7, "tasks": ["4.5", "4.6", "5.3", "9.3", "10.6"] },
    { "id": 8, "tasks": ["6.4", "8.4", "9.2", "13.4"] },
    { "id": 9, "tasks": ["8.5", "8.6", "9.4", "9.5", "12.3"] },
    { "id": 10, "tasks": ["10.4", "10.5", "12.4"] },
    { "id": 11, "tasks": ["14.1"] },
    { "id": 12, "tasks": ["14.2"] }
  ]
}
```
