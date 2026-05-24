# Requirements Document

## Introduction

Live Duel Mode is a real-time 1v1 competitive coding feature for the existing CodeCombat platform. Two authenticated users enter a matchmaking queue, are paired into a single match, are served the same problem, and submit code in parallel. The first user whose submission passes every test case wins; if both pass, the earlier accepted-submission timestamp wins; if neither passes within the time budget, the match ends as a draw; if a user disconnects past the grace period, the match ends as abandoned in favor of the remaining user.

The feature must layer on top of the existing Spring Boot 3.5.9 / Java 21 / PostgreSQL / Valkey / SSE + WebSocket stack without introducing new transports, new code-execution paths, or multi-instance coordination requirements. Code execution reuses `SubmissionWorkerPool` with new metadata fields (`duelId`). Live presence and verdicts reuse the existing SSE infrastructure at `/api/submissions/stream` plus a new SSE topic for duel-room events. Persistence is done in new PostgreSQL tables only — no schema rewrites of existing tables. Valkey holds the matchmaking queue, presence state, and idempotency locks. The platform target is a single Oracle A1 Flex VM (1 OCPU, 6GB RAM) and a userbase of fewer than 500 active users, so the design favors simplicity and correctness over horizontal scalability.

## Glossary

- **Duel_Service**: Backend service that owns duel match lifecycle (creation, state transitions, finalization). Single-JVM in-memory state guarded by Valkey atomic primitives.
- **Matchmaking_Service**: Backend service that maintains the duel matchmaking queue and pairs waiting users. Backed by a Valkey list and one Valkey hash.
- **Duel_Match**: The persistent record of one 1v1 contest. Has a `matchId` (UUID), two `userId`s, a `problemId`, a `status` (`WAITING`, `IN_PROGRESS`, `FINISHED`), an `outcome` (`USER_A_WIN`, `USER_B_WIN`, `DRAW`, `ABANDONED`, or null while live), `started_at`, `ended_at`, and a `winner_user_id` (nullable).
- **Duel_Problem_Pool**: The set of problems flagged as duel-eligible (new boolean column `duel_eligible` on the existing `problems` table is forbidden by constraints; instead a new join table `duel_eligible_problems` references `problems.id`).
- **Duel_Submission**: A `submissions` row produced inside a duel. The same `submissions` table is reused; the link from a submission to a duel is stored in a new `duel_submissions` join table (one-to-many: a duel can have many submissions, a submission belongs to at most one duel).
- **Duel_Live_Channel**: The SSE channel `/api/duels/{matchId}/stream` used to push opponent-presence and progress events to both participants of a match.
- **Duel_Admin_Console**: The admin observability surface (REST endpoints under `/api/admin/duels/*` plus a panel inside `/admin/dashboard`).
- **Duel_Frontend**: The two React pages — `/duel` (lobby + Find Match) and `/duel/:matchId` (live arena).
- **Match_Cooldown**: A per-user time window after a `FINISHED` match during which the user cannot enter a new match (default 5 seconds).
- **Reconnect_Grace_Period**: A per-user time window during which a transient disconnect does not abandon the match (default 30 seconds).
- **AC**: Submission verdict where `submissions.status = 'AC'` and `test_cases_passed = total_test_cases`.
- **First_AC_Time**: The `submitted_at` timestamp of the earliest AC submission a user produced inside a given duel. Used as the win-time tiebreaker.
- **Idempotency_Key**: A short-lived Valkey key (`duel:enqueue:{userId}`) set with `SET NX EX 5` to absorb double-clicks on Find Match.
- **Active_Match**: A `Duel_Match` whose status is `WAITING` or `IN_PROGRESS`.
- **SubmissionWorkerPool**: The existing background worker pool that consumes `submission:queue` from Valkey and runs sandboxed code (unchanged by this feature; only the job payload gains an optional `duelId`).
- **SseEmitterRegistry**: The existing per-userId SSE registry. A duel-scoped registry (`DuelSseEmitterRegistry`) is added alongside it, keyed by `matchId` instead of `userId`, so both participants receive the same room-level events.

## Requirements

### Requirement 1: Matchmaking Queue Entry and Cancellation

**User Story:** As an authenticated user, I want to click a Find Match button to enter a matchmaking queue and to cancel my entry before being paired, so that I can opt in and out of duels without committing prematurely.

#### Acceptance Criteria

1. WHEN an authenticated user submits a `POST /api/duels/queue` request, THE Matchmaking_Service SHALL append the user to the duel matchmaking queue and SHALL return a response containing the user's queue token within 200ms under nominal load.
2. WHEN the same authenticated user submits a `POST /api/duels/queue` request while already present in the matchmaking queue, THE Matchmaking_Service SHALL return HTTP 200 with the existing queue token rather than enqueuing a duplicate entry.
3. WHEN an authenticated user submits a `DELETE /api/duels/queue` request and the user is present in the matchmaking queue, THE Matchmaking_Service SHALL remove the user from the matchmaking queue and SHALL return HTTP 204 within 200ms under nominal load.
4. IF an authenticated user submits a `POST /api/duels/queue` request while the user already has an Active_Match, THEN THE Matchmaking_Service SHALL reject the request with HTTP 409 and a body containing the existing `matchId`.
5. WHEN a user has been in the matchmaking queue for more than 120 seconds without being paired, THE Matchmaking_Service SHALL remove the user from the queue and SHALL emit an SSE `queue_timeout` event on `/api/submissions/stream` for that user.
6. WHILE a user is present in the matchmaking queue, THE Matchmaking_Service SHALL set a Valkey TTL of 180 seconds on the queue entry so that orphaned entries from crashed clients are reclaimed automatically.
7. WHEN an authenticated user submits a second `POST /api/duels/queue` request within 1 second of a previous accepted request from the same user, THE Matchmaking_Service SHALL treat the second request as idempotent using the Idempotency_Key and SHALL NOT enqueue a duplicate.

### Requirement 2: Match Pairing and Lifecycle State Transitions

**User Story:** As an authenticated user in the matchmaking queue, I want to be paired with another waiting user and progress through a defined match lifecycle, so that the duel has a deterministic outcome and can be reasoned about.

#### Acceptance Criteria

1. WHEN two distinct users are present in the matchmaking queue and neither has an Active_Match and neither is in Match_Cooldown, THE Matchmaking_Service SHALL atomically remove both users from the queue, SHALL create a new `Duel_Match` row with `status = 'WAITING'`, SHALL select a problem per Requirement 3, and SHALL transition the match to `status = 'IN_PROGRESS'` with `started_at = now()` within 500ms of the second user becoming available.
2. THE Duel_Service SHALL ensure that for every `Duel_Match`, the lifecycle status transitions occur only along the directed edges `WAITING → IN_PROGRESS`, `WAITING → FINISHED`, and `IN_PROGRESS → FINISHED`.
3. WHEN a `Duel_Match` transitions to `status = 'FINISHED'`, THE Duel_Service SHALL set `ended_at = now()`, SHALL set `outcome` to exactly one of `USER_A_WIN`, `USER_B_WIN`, `DRAW`, or `ABANDONED`, SHALL set `winner_user_id` to the winning user's id when outcome is `USER_A_WIN` or `USER_B_WIN`, and SHALL set `winner_user_id = NULL` when outcome is `DRAW` or `ABANDONED`, enforced by a CHECK constraint `((outcome IN ('USER_A_WIN','USER_B_WIN') AND winner_user_id IS NOT NULL) OR (outcome IN ('DRAW','ABANDONED') AND winner_user_id IS NULL))` on the `duel_matches` row.
4. THE Duel_Service SHALL persist the `Duel_Match` row in PostgreSQL such that once `status = 'FINISHED'` is committed, no subsequent update to `outcome`, `winner_user_id`, or `ended_at` is permitted.
5. WHILE a `Duel_Match` has `status = 'IN_PROGRESS'` and the elapsed time since `started_at` exceeds 600 seconds, THE Duel_Service SHALL transition the match to `status = 'FINISHED'` with `outcome = 'DRAW'` if neither user has produced an AC submission inside the duel.
6. WHEN a `Duel_Match` is created, THE Duel_Service SHALL assign the two participants to deterministic seats `userA` and `userB` ordered by ascending `userId`, so that the seat assignment is reproducible from the participant ids alone.

### Requirement 3: Problem Selection From Duel-Eligible Pool

**User Story:** As a participant in a freshly paired duel, I want both me and my opponent to receive the same problem chosen from a curated duel-eligible pool that neither of us has already solved together, so that the duel is fair and reuses platform content.

#### Acceptance Criteria

1. WHEN a `Duel_Match` is created, THE Duel_Service SHALL select exactly one `problemId` from the Duel_Problem_Pool such that the same `problemId` is recorded for both participants.
2. WHEN selecting a problem, THE Duel_Service SHALL exclude any `problemId` for which both participants already have a row in `user_problem_solved` for that problem.
3. IF every problem in the Duel_Problem_Pool is excluded by Acceptance Criterion 2, THEN THE Duel_Service SHALL select a problem from the Duel_Problem_Pool uniformly at random regardless of solved history and SHALL log a `duel.problem_pool.exhausted` warning.
4. IF the Duel_Problem_Pool contains zero problems at the moment of selection, THEN THE Duel_Service SHALL fail the match creation, SHALL NOT enqueue either user, SHALL return both users to the matchmaking queue if they re-request, and SHALL emit an SSE `pairing_failed` event with reason `no_eligible_problem` to both users.
5. WHEN selecting a problem from candidates eligible per Acceptance Criterion 2, THE Duel_Service SHALL choose uniformly at random from the candidate set so that no single problem dominates the pairing distribution.
6. THE Duel_Service SHALL store the selected `problemId` on the `duel_matches` row at the moment of creation, and THE Duel_Service SHALL NOT permit the `problemId` of a `FINISHED` match to be changed.

### Requirement 4: Real-Time Opponent Presence and Progress Updates

**User Story:** As a duel participant, I want to see my opponent's name and live status (typing, compiling, submitted, last AC count), so that the duel feels tense and live rather than blind.

#### Acceptance Criteria

1. WHEN a user opens an SSE subscription on `/api/duels/{matchId}/stream` with a valid SSE ticket, THE Duel_Live_Channel SHALL register the subscription against the `matchId` and SHALL immediately push a `room_state` event containing both participants' `userId`, `username`, and current `progress_status`.
2. WHEN a participant submits any code via `POST /api/submissions` with the duel's `matchId`, THE Duel_Service SHALL emit a `progress` event on the Duel_Live_Channel for that match with fields `userId`, `event = 'submitted'`, and `submissionId` within 100ms of the submission row being created.
3. WHEN the SubmissionWorkerPool finalizes a duel-tagged submission with status `JUDGING`, `AC`, `WA`, `TLE`, `RE`, `CE`, or `MLE`, THE Duel_Service SHALL emit a `progress` event on the Duel_Live_Channel with fields `userId`, `event = 'verdict'`, `submissionId`, `status`, `test_cases_passed`, and `total_test_cases` within 200ms of finalization.
4. WHEN a participant emits a typing heartbeat via `POST /api/duels/{matchId}/heartbeat` with a non-empty payload, THE Duel_Live_Channel SHALL emit a `progress` event with `userId` and `event = 'typing'` to the opponent only, no more than once per 1500ms per user.
5. WHILE a `Duel_Match` has `status = 'IN_PROGRESS'`, THE Duel_Live_Channel SHALL deliver every `progress` event to every active subscription on that `matchId`, including all tabs of either participant.
6. IF a participant attempts to subscribe to `/api/duels/{matchId}/stream` for a `matchId` they are not a participant of, THEN THE Duel_Live_Channel SHALL reject the subscription with HTTP 403.

### Requirement 5: Duel Submission Pipeline Reusing SubmissionWorkerPool

**User Story:** As a duel participant, I want my code submissions inside a duel to use the same sandboxed execution path as practice submissions, so that judging behavior is consistent and no new code-execution surface is introduced.

#### Acceptance Criteria

1. WHEN an authenticated participant submits code via `POST /api/duels/{matchId}/submissions` with a `code` and `language` body, THE Duel_Service SHALL create a `submissions` row, SHALL create a corresponding `duel_submissions` row binding `submissionId` to `matchId`, SHALL enqueue a `SubmissionJob` onto `submission:queue` with the existing payload plus `duelId = matchId`, and SHALL return HTTP 202 with the `submissionId` within 100ms.
2. WHEN the SubmissionWorkerPool processes a `SubmissionJob` carrying a `duelId`, THE SubmissionWorkerPool SHALL execute the user's submitted source code inside the existing bwrap+prlimit sandbox using the same time and memory limits derived from the problem's `time_limit` and `memory_limit` fields. THE SubmissionWorkerPool SHALL NOT short-circuit, mock, or skip the sandboxed execution step for any duel job.
3. THE Duel_Service SHALL ensure that a `submissions` row produced inside a duel is linked to exactly one `duel_matches` row via `duel_submissions`, and SHALL ensure that the same `submissionId` cannot be linked to a second `duel_matches` row.
4. IF a user submits code to `POST /api/duels/{matchId}/submissions` for a `matchId` they are not a participant of, THEN THE Duel_Service SHALL reject the request with HTTP 403.
5. IF a user submits code to `POST /api/duels/{matchId}/submissions` for a `matchId` whose status is `FINISHED`, THEN THE Duel_Service SHALL reject the request with HTTP 409.
6. WHEN a submission produced inside a duel finalizes, THE SubmissionWorkerPool SHALL invoke the existing SseEmitterRegistry per-user verdict push AND SHALL invoke the new DuelSseEmitterRegistry per-match progress push so that personal verdicts and room progress both arrive.
7. WHEN a duel-tagged submission finalizes with any status, THE Duel_Service SHALL NOT increment any contest leaderboard, SHALL NOT call `LeaderboardCacheService.updateScore`, SHALL NOT call any other leaderboard update mechanism that may exist now or in the future, SHALL NOT write to `user_problem_solved`, and SHALL NOT update `users.total_points`. THE SubmissionWorkerPool SHALL gate every leaderboard-related side effect behind a `job.duelId == null` check so that adding new leaderboard side effects in the future does not silently leak into duel submissions.

### Requirement 6: Win Condition and Tie-Breaking

**User Story:** As a duel participant, I want a deterministic and transparent rule for who wins, so that the platform's verdict cannot be disputed.

#### Acceptance Criteria

1. WHEN a duel-tagged submission for a `Duel_Match` with `status = 'IN_PROGRESS'` finalizes with `status = 'AC'` and `test_cases_passed = total_test_cases`, THE Duel_Service SHALL record the submission's `submitted_at` as the user's First_AC_Time for that match if no earlier First_AC_Time has been recorded for that user in that match.
2. WHEN exactly one participant has a First_AC_Time and the other does not at the moment of finalization, THE Duel_Service SHALL transition the match to `FINISHED` with `outcome` equal to `USER_A_WIN` if the AC-producing user is in seat A, otherwise `USER_B_WIN`, and SHALL set `winner_user_id` accordingly.
3. WHEN both participants have a First_AC_Time recorded, THE Duel_Service SHALL transition the match to `FINISHED` with `outcome` set to whichever seat's First_AC_Time is strictly earlier, using `submissions.id` as a deterministic tiebreaker if the two `submitted_at` timestamps are exactly equal.
4. THE Duel_Service SHALL implement the "first AC wins" decision using a Valkey atomic `SET NX` on key `duel:winner:{matchId}` with value `{userId}` so that two AC verdicts arriving milliseconds apart resolve to a single winner deterministically and the loser's verdict cannot overwrite the winner.
5. THE Duel_Service SHALL back the Valkey winner-claim with a PostgreSQL `UNIQUE` constraint on `duel_matches.winner_user_id` per `matchId` (i.e. `winner_user_id` is set via an UPDATE that uses `WHERE winner_user_id IS NULL` so the second writer's update affects zero rows) so that a Valkey eviction or restart cannot produce two winners.
6. WHILE a `Duel_Match` has `status = 'IN_PROGRESS'` and the elapsed time since `started_at` exceeds 600 seconds AND neither participant has a First_AC_Time, THE Duel_Service SHALL transition the match to `FINISHED` with `outcome = 'DRAW'` immediately at the 600-second boundary. THE Duel_Service SHALL evaluate this condition continuously via a per-match scheduled timer (not only on submission-finalize events) so that a duel with zero submissions still ends at 600 seconds.

### Requirement 7: Reconnection and Disconnect Handling

**User Story:** As a duel participant whose network briefly drops, I want a short reconnection window so that a transient blip does not cost me the match, but I want the match to resolve in finite time if I never come back.

#### Acceptance Criteria

1. WHEN a participant's last Duel_Live_Channel SSE subscription for a `matchId` closes, THE Duel_Service SHALL start a Reconnect_Grace_Period timer of 30 seconds for that `userId` in that `matchId`.
2. WHEN the same participant opens a new Duel_Live_Channel SSE subscription for the same `matchId` while a Reconnect_Grace_Period timer is active for that participant, THE Duel_Service SHALL cancel the timer and SHALL emit a `progress` event with `event = 'reconnected'` and the user's `userId`. WHERE no Reconnect_Grace_Period timer is active for the participant at the moment of subscription, THE Duel_Service SHALL NOT emit a `reconnected` event.
3. IF the Reconnect_Grace_Period expires without a new subscription for that participant AND the match status is still `IN_PROGRESS` at the moment of expiration, THEN THE Duel_Service SHALL transition the match to `FINISHED` with `outcome = 'ABANDONED'` and SHALL set `winner_user_id` to the opponent. IF the match status is no longer `IN_PROGRESS` at the moment of expiration, THEN THE Duel_Service SHALL ignore the timer expiration without further state change.
4. WHEN both participants' Reconnect_Grace_Period timers expire simultaneously, THE Duel_Service SHALL transition the match to `FINISHED` with `outcome = 'DRAW'` and `winner_user_id = NULL`.
5. WHEN a participant explicitly forfeits via `POST /api/duels/{matchId}/forfeit`, THE Duel_Service SHALL transition the match to `FINISHED` with `outcome = 'ABANDONED'` and `winner_user_id = the opponent` immediately, bypassing the Reconnect_Grace_Period.
6. IF a participant submits a forfeit request for a `matchId` whose status is `FINISHED`, THEN THE Duel_Service SHALL return HTTP 409 with the existing `outcome`.

### Requirement 8: Idempotency and Double-Click Protection on Find Match

**User Story:** As a user, I want clicking Find Match twice in quick succession to behave the same as clicking once, so that an impatient double-click does not break my queue state.

#### Acceptance Criteria

1. WHEN an authenticated user submits two `POST /api/duels/queue` requests within 1 second carrying the same `Idempotency-Key` header, THE Matchmaking_Service SHALL process the second request as a no-op and SHALL return the same response body as the first.
2. WHERE the `Idempotency-Key` header is absent, THE Matchmaking_Service SHALL synthesize an idempotency key as `duel:enqueue:{userId}` with a 5-second Valkey TTL, and SHALL still treat back-to-back requests as the same intent.
3. THE Matchmaking_Service SHALL implement enqueue idempotency using a Valkey `SET NX EX 5` on `duel:enqueue:{userId}` so that two concurrent enqueue requests from the same user produce exactly one queue entry under any thread-interleaving order.
4. THE Duel_Service SHALL implement match-creation idempotency by writing a Valkey `SET NX EX 60` on `duel:create:{sortedUserIdPair}` immediately before inserting the `duel_matches` row, so that two concurrent pairing attempts for the same pair of users produce exactly one match row.

### Requirement 9: Concurrency Safety on AC Submissions Arriving Milliseconds Apart

**User Story:** As a platform operator, I want the win-determination logic to be race-free when two AC submissions for the same duel finalize within milliseconds of each other, so that exactly one winner is recorded regardless of thread interleaving.

#### Acceptance Criteria

1. WHEN two duel-tagged submissions for the same `Duel_Match` finalize with `status = 'AC'` in any temporal order, THE Duel_Service SHALL record exactly one `winner_user_id` on the `duel_matches` row.
2. THE Duel_Service SHALL guarantee win uniqueness using two redundant mechanisms: a Valkey `SET NX` on `duel:winner:{matchId}` and a PostgreSQL `UPDATE duel_matches SET winner_user_id = ?, status = 'FINISHED', outcome = ?, ended_at = ? WHERE matchId = ? AND winner_user_id IS NULL AND status = 'IN_PROGRESS'` whose returned row count is checked.
3. IF the Valkey `SET NX` succeeds for a candidate winner but the corresponding PostgreSQL UPDATE returns 0 rows affected for any reason (including concurrent UPDATE by the other candidate, transient SQL error, or constraint violation), THEN THE Duel_Service SHALL treat the winner as already decided, SHALL re-read the `duel_matches` row to discover the actual winner, and SHALL emit the appropriate `match_finished` event without re-writing the database.
4. THE Duel_Service SHALL ensure that the loser's AC submission, even if its `submitted_at` is earlier in absolute time, does not overwrite the winner once the winner is committed; the database UPDATE's `WHERE winner_user_id IS NULL` clause SHALL be the authoritative gate.
5. WHEN both AC submissions finalize within the same millisecond and the Valkey + DB gates both pick the same winner, THE Duel_Service SHALL emit exactly one `match_finished` Duel_Live_Channel event.

### Requirement 10: Anti-Abuse — Concurrent Duel Limit and Cooldown

**User Story:** As a platform operator, I want each user to be limited to one active duel at a time and to face a short cooldown before queueing for another match, so that users cannot grief by spamming matches or holding multiple matches hostage.

#### Acceptance Criteria

1. THE Duel_Service SHALL enforce a per-user limit of at most one Active_Match by adding a partial unique index `CREATE UNIQUE INDEX ux_duel_active_user_a ON duel_matches(user_a_id) WHERE status IN ('WAITING', 'IN_PROGRESS')` and an equivalent index on `user_b_id`.
2. IF the Duel_Service attempts a `duel_matches` INSERT and the INSERT raises a `unique_violation` SQL state from the partial unique index of Acceptance Criterion 1, THEN THE Duel_Service SHALL roll back the transaction, SHALL release the matchmaking queue entries it removed, and SHALL emit a `pairing_failed` event with reason `concurrent_match` to the affected user. THE Duel_Service SHALL NOT emit a `concurrent_match` `pairing_failed` event in any other circumstance.
3. WHEN a `Duel_Match` transitions to `status = 'FINISHED'`, THE Duel_Service SHALL set a Valkey key `duel:cooldown:{userId}` with `EX 5` for each participant.
4. IF an authenticated user submits `POST /api/duels/queue` while `duel:cooldown:{userId}` exists in Valkey, THEN THE Matchmaking_Service SHALL reject the request with HTTP 429 and a `Retry-After` header equal to the remaining cooldown seconds.
5. THE Match_Cooldown duration SHALL be configurable via the environment variable `DUEL_COOLDOWN_SEC` with a default value of 5.

### Requirement 11: Admin Observability

**User Story:** As a platform admin, I want to see how many duels are active, how many users are queued, and how many duels finished today, so that I can confirm the feature is healthy and used.

#### Acceptance Criteria

1. WHEN an authenticated admin sends `GET /api/admin/duels/metrics`, THE Duel_Admin_Console SHALL return a JSON body containing `activeMatchCount`, `queueDepth`, `matchesFinishedToday`, and `matchesAbandonedToday` within 300ms.
2. WHEN an authenticated admin sends `GET /api/admin/duels?status=IN_PROGRESS&limit=50`, THE Duel_Admin_Console SHALL return a paginated list of in-progress matches with fields `matchId`, `userA_username`, `userB_username`, `problemId`, `started_at`, and `elapsed_seconds`.
3. WHEN an authenticated admin sends `POST /api/admin/duels/{matchId}/cancel`, THE Duel_Admin_Console SHALL transition the match to `FINISHED` with `outcome = 'ABANDONED'` and `winner_user_id = NULL`, SHALL emit a `match_finished` Duel_Live_Channel event, and SHALL return HTTP 200.
4. IF a non-admin user calls any `/api/admin/duels/*` endpoint, THEN THE Duel_Admin_Console SHALL reject the request with HTTP 403.
5. THE Duel_Admin_Console SHALL surface the metrics from Acceptance Criterion 1 inside the existing `/admin/dashboard` panel.

### Requirement 12: Persistent Match Record for History and Future ELO

**User Story:** As a product owner, I want every finished duel to persist enough data in PostgreSQL to compute future leaderboards and ELO ratings without replaying SSE events, so that historical accuracy survives Valkey flushes and JVM restarts.

#### Acceptance Criteria

1. THE Duel_Service SHALL persist every `Duel_Match` row in a new `duel_matches` table with columns `match_id UUID PRIMARY KEY`, `user_a_id BIGINT NOT NULL`, `user_b_id BIGINT NOT NULL`, `problem_id BIGINT NOT NULL`, `status VARCHAR NOT NULL CHECK (status IN ('WAITING', 'IN_PROGRESS', 'FINISHED'))`, `outcome VARCHAR CHECK (outcome IN ('USER_A_WIN', 'USER_B_WIN', 'DRAW', 'ABANDONED'))`, `winner_user_id BIGINT`, `started_at TIMESTAMP`, `ended_at TIMESTAMP`, and `created_at TIMESTAMP NOT NULL DEFAULT now()`.
2. THE Duel_Service SHALL persist every duel-bound submission link in a new `duel_submissions` table with columns `submission_id BIGINT PRIMARY KEY REFERENCES submissions(id)`, `match_id UUID NOT NULL REFERENCES duel_matches(match_id)`, and `is_first_ac BOOLEAN NOT NULL DEFAULT FALSE`, with an index on `match_id`.
3. THE Duel_Service SHALL persist the duel-eligible problem pool membership in a new `duel_eligible_problems` table with columns `problem_id BIGINT PRIMARY KEY REFERENCES problems(id)`, `added_at TIMESTAMP NOT NULL DEFAULT now()`, and `added_by BIGINT REFERENCES users(id)`.
4. THE Duel_Service SHALL ensure that for every duel-tagged `submissions` row, `submissions.submitted_at >= duel_matches.started_at AND submissions.submitted_at <= duel_matches.ended_at` once the match is `FINISHED`.
5. THE Duel_Service SHALL NOT modify any existing column definitions on the `submissions`, `problems`, `users`, or `contests` tables.
6. WHEN the `duel_matches` table is migrated, THE Duel_Service SHALL include the partial unique indexes from Requirement 10 Acceptance Criterion 1 in the same Flyway migration script `V<n>__live_duel_mode.sql`.

### Requirement 13: Frontend Pages — Lobby and Live Arena

**User Story:** As a user, I want a lobby page with a Find Match button and a live arena page with my code editor on one side and my opponent's status on the other, so that the duel feels like a real-time competitive experience.

#### Acceptance Criteria

1. WHEN an authenticated user navigates to `/duel`, THE Duel_Frontend SHALL render the duel lobby page inside a `<UserRoute>` guard, SHALL display a Find Match button, and SHALL display the user's recent duel history (last 10 matches with opponent username, problem title, outcome, ended_at).
2. WHEN the user clicks the Find Match button, THE Duel_Frontend SHALL disable the button, SHALL POST `/api/duels/queue`, SHALL open an SSE subscription on `/api/submissions/stream` to receive a `matched` event, and SHALL navigate to `/duel/{matchId}` upon receiving that event.
3. WHILE the lobby is awaiting a match, THE Duel_Frontend SHALL display a Cancel button that issues `DELETE /api/duels/queue`, re-enables the Find Match button on success, and exits the awaiting-match state.
4. WHEN an authenticated user navigates to `/duel/{matchId}` and is a participant of the match, THE Duel_Frontend SHALL render the live arena inside a `<UserRoute>` guard with a code editor for the user's code, the problem statement, an opponent panel showing the opponent's username and live status, and a submission history list scoped to the current duel.
5. IF an authenticated user navigates to `/duel/{matchId}` for a `matchId` they are not a participant of, THEN THE Duel_Frontend SHALL display a "You are not a participant of this match" empty state and SHALL NOT open any SSE or WebSocket connection for that `matchId`.
6. WHEN the live arena receives a `match_finished` Duel_Live_Channel event, THE Duel_Frontend SHALL display a result modal with the outcome and `winner_user_id` resolved to the winner's username, and SHALL provide a "Return to lobby" action that navigates to `/duel`.
7. THE Duel_Frontend SHALL acquire SSE subscriptions for the Duel_Live_Channel using the existing `SseTicketService` flow (POST `/api/duels/{matchId}/sse-ticket`, then GET `/api/duels/{matchId}/stream?ticket=...`) so that no JWT is leaked into URLs.

### Requirement 14: Time-Monotonic Event Ordering

**User Story:** As a platform operator auditing a duel after the fact, I want every event timestamp inside a duel to be bounded by the match's start and end times, so that no submission or progress event can plausibly be claimed to have happened outside the match window.

#### Acceptance Criteria

1. THE Duel_Service SHALL ensure that for every `submissions` row linked to a `Duel_Match` via `duel_submissions`, `submissions.submitted_at >= duel_matches.started_at`.
2. THE Duel_Service SHALL ensure that for every `submissions` row linked to a `Duel_Match` via `duel_submissions` whose `duel_matches.status = 'FINISHED'`, `submissions.submitted_at <= duel_matches.ended_at`.
3. THE Duel_Service SHALL ensure that for every `Duel_Match` with `status = 'FINISHED'`, `duel_matches.ended_at >= duel_matches.started_at`.
4. THE Duel_Service SHALL derive `submitted_at` from the same monotonic source as the existing `SubmissionService` (server-side `TimeUtil.now()`) so that client-clock drift cannot violate Acceptance Criteria 1 and 2.
5. IF a Duel_Live_Channel `progress` event is generated after `duel_matches.ended_at`, THEN THE Duel_Service SHALL drop the event AND SHALL log a `duel.event.late_drop` warning. THE drop step and the log step SHALL be independent so that a failure in one does not skip the other; partial success (event dropped but log failed, or log emitted but drop failed) SHALL be acceptable so long as no late event is delivered to a Duel_Live_Channel subscription.
