# Requirements Document

## Introduction

Private Contest Hosting extends the existing CodeCoder platform to allow verified users to host their own private coding contests. This feature integrates seamlessly into the current Spring Boot architecture, reusing the existing contest engine, judge system, proctoring infrastructure, leaderboard, and problem management. The feature does NOT introduce a separate microservice; all APIs, authentication, database tables, and cache keys coexist with the existing public contest system.

A verified user requests approval from an admin to become a Contest_Host. Once approved, the Contest_Host can create up to 2 private contests per month, each with a maximum of 100 participants and a maximum duration of 5 hours. The Contest_Host manages invitations through unique invite links, selects problems from the existing problem bank or generates new problems using the AI Problem Generator, and optionally enables proctoring. Participants access private contests through the same contest UI, submission pipeline, and leaderboard architecture as public contests, with isolation enforced through database foreign keys, cache key prefixes, and UI filters.

The feature introduces new database tables (`private_contests`, `contest_hosting_requests`, `private_contest_invitations`, `private_contest_participants`) that reference the existing `contests`, `users`, `problems`, and `submissions` tables. The dedicated Judge Worker VM (Oracle Cloud, 6GB RAM) processes submissions from private contests using the same judge queue and worker pool architecture, differentiated only by cache key prefixes (`private:contest:*`, `private:leaderboard:*`). The scheduler enforces business rules (2 contests per user per month, no overlapping contest times, max 100 participants, max 5 hours duration) at creation time and provides analytics dashboards for Contest_Hosts to monitor participation, submission rates, and problem difficulty.

This document captures the requirements. Architecture, database schema, API endpoints, cache key design, and implementation tasks are deferred to the design phase.

## Glossary

- **System** — The CodeCoder web platform (Spring Boot backend, React frontend, PostgreSQL 18, Valkey cache, Judge Workers).
- **Backend** — The Spring Boot 3 application running on Java 21 with Spring Security JWT authentication.
- **Frontend** — The React 19 + Vite SPA.
- **Database** — The PostgreSQL 18 instance managed by Flyway migrations.
- **Cache** — The Valkey instance used for hot state, rate limiting, leaderboards, and judge queues.
- **Admin** — An authenticated user with `ROLE_ADMIN`.
- **User** — An authenticated non-admin user with `ROLE_USER`.
- **Verified_User** — A User whose `enabled` field is `TRUE` and who has completed email verification.
- **Contest_Host** — A Verified_User who has been approved by an Admin to create private contests, indicated by an approved row in `contest_hosting_requests`.
- **Public_Contest** — A contest created by an Admin, visible to all users, managed through the existing `contests` table with no corresponding `private_contests` row.
- **Private_Contest** — A contest created by a Contest_Host, represented by a row in both the `contests` table (for contest engine integration) and the `private_contests` table (for ownership and business rules), accessible only by invitation.
- **Hosting_Request** — A row in `contest_hosting_requests` representing a Verified_User's request to become a Contest_Host, with status `PENDING`, `APPROVED`, or `REJECTED`.
- **Invite_Link** — A unique, non-guessable URL generated per Private_Contest that authenticates a recipient as eligible to join.
- **Invite_Token** — The cryptographically random string embedded in an Invite_Link, stored in `private_contest_invitations`.
- **Participant** — A User who has accepted an Invite_Link and is recorded in `private_contest_participants`, granting access to the Private_Contest.
- **Contest_Engine** — The existing Spring Boot contest subsystem that manages contest lifecycle, problem attachment, submission flow, and leaderboard calculation.
- **Judge_Queue** — The Valkey list `submission:queue` (for public contests) and `private:submission:queue` (for private contests) that holds pending judge jobs.
- **Judge_Worker** — The dedicated Oracle Cloud VM (6GB RAM) that drains judge queues, executes code in sandboxed environments, and writes verdicts.
- **Proctoring_System** — The existing proctored-contest-mode feature that can be optionally enabled per Private_Contest.
- **AI_Problem_Generator** — The existing AI service that generates coding problems from natural language prompts.
- **Leaderboard** — The existing leaderboard calculation and caching architecture, reused for Private_Contests with a `private:leaderboard:{contestId}` cache key prefix.
- **Analytics_Dashboard** — A new UI surface for Contest_Hosts to view participation metrics, submission stats, and problem performance for their Private_Contests.
- **Monthly_Quota** — The limit of 2 Private_Contests per Contest_Host per calendar month, enforced at creation time.
- **Participant_Limit** — The maximum of 100 Participants per Private_Contest, enforced at invitation acceptance time.
- **Duration_Limit** — The maximum contest duration of 5 hours (300 minutes), enforced at creation time.
- **Overlap_Check** — The validation that a Contest_Host's new Private_Contest does not have a time window that intersects with any of their existing Private_Contests.
- **Scheduler** — The Spring `@Scheduled` subsystem that transitions contest statuses (`UPCOMING` → `LIVE` → `ENDED`) and cleans up expired invite tokens.

## Requirements

### Requirement 1: Contest hosting request submission

**User Story:** As a Verified_User, I want to request approval to host private contests, so that I can organize coding competitions for my students, team, or community.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that accepts a Verified_User's hosting request with fields `reason` (text, max 500 characters) and `intended_use_case` (enum: `EDUCATION`, `RECRUITMENT`, `COMMUNITY`, `INTERNAL_TRAINING`, `OTHER`).
2. WHEN a Verified_User submits a hosting request, THE Backend SHALL create a row in `contest_hosting_requests` with status `PENDING`, `user_id`, `submitted_at`, `reason`, and `intended_use_case`.
3. IF a Verified_User already has a Hosting_Request with status `PENDING`, THEN THE Backend SHALL reject the new request with HTTP `409 Conflict` and message "You already have a pending hosting request".
4. IF a Verified_User already has a Hosting_Request with status `APPROVED`, THEN THE Backend SHALL reject the new request with HTTP `409 Conflict` and message "You are already approved to host contests".
5. WHEN a hosting request is created, THE Backend SHALL send an email notification to all Admins with a link to the admin approval dashboard.

### Requirement 2: Admin approval and rejection of hosting requests

**User Story:** As an Admin, I want to review and approve or reject hosting requests, so that only trustworthy users can host private contests.

#### Acceptance Criteria

1. THE Frontend SHALL provide an admin dashboard that lists all Hosting_Requests with status `PENDING`, showing `username`, `email`, `submitted_at`, `reason`, and `intended_use_case`.
2. WHEN an Admin approves a Hosting_Request, THE Backend SHALL update its status to `APPROVED`, set `reviewed_by` to the Admin's `user_id`, set `reviewed_at` to the current timestamp, and persist an optional `admin_notes` field.
3. WHEN an Admin rejects a Hosting_Request, THE Backend SHALL update its status to `REJECTED`, set `reviewed_by` and `reviewed_at`, persist `admin_notes`, and send an email to the requesting User with the rejection reason.
4. WHEN a Hosting_Request is approved, THE Backend SHALL send an email to the requesting User confirming approval and providing a link to the "Create Private Contest" page.
5. THE Backend SHALL NOT allow an Admin to approve or reject a Hosting_Request that is not in `PENDING` status, returning HTTP `409 Conflict` if attempted.
6. THE Backend SHALL record the approval or rejection action in an audit log table with `(admin_id, request_id, action, timestamp, notes)`.

### Requirement 3: Contest_Host monthly quota enforcement

**User Story:** As the System, I want to limit each Contest_Host to 2 Private_Contests per calendar month, so that hosting capacity is distributed fairly.

#### Acceptance Criteria

1. WHEN a Contest_Host attempts to create a Private_Contest, THE Backend SHALL count how many Private_Contests that Contest_Host has created in the current calendar month (defined as UTC month boundaries).
2. IF the count is already 2 or more, THEN THE Backend SHALL reject the creation request with HTTP `429 Too Many Requests` and message "You have reached your monthly limit of 2 private contests".
3. THE Backend SHALL include cancelled Private_Contests (those with status `CANCELLED` by the Contest_Host before `start_time`) in the monthly count.
4. THE Backend SHALL NOT include deleted Private_Contests (those removed by Admins) in the monthly count.
5. THE Backend SHALL provide an API endpoint that returns the Contest_Host's current month usage: `{ "used": 1, "limit": 2, "resets_at": "2026-02-01T00:00:00Z" }`.

### Requirement 4: Private contest creation with business rules

**User Story:** As a Contest_Host, I want to create a Private_Contest with a name, description, start time, end time, and optional proctoring settings, so that I can organize a coding competition for my invited participants.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that accepts a Private_Contest creation request from a Contest_Host with fields: `name` (string, max 200 chars), `description` (text, optional), `start_time` (ISO 8601 datetime), `end_time` (ISO 8601 datetime), and `enable_proctoring` (boolean, default `FALSE`).
2. WHEN a Contest_Host submits a creation request, THE Backend SHALL validate that `end_time` is after `start_time` and that the duration (`end_time - start_time`) does not exceed 5 hours (300 minutes).
3. IF the duration exceeds 5 hours, THEN THE Backend SHALL reject the request with HTTP `400 Bad Request` and message "Contest duration cannot exceed 5 hours".
4. WHEN a Contest_Host submits a creation request, THE Backend SHALL check for Overlap_Check: no existing Private_Contest by the same Contest_Host has a time window that intersects `[start_time, end_time)`.
5. IF an overlap is detected, THEN THE Backend SHALL reject the request with HTTP `409 Conflict` and message "This contest overlaps with your existing contest '{conflicting_contest_name}'".
6. WHEN all validations pass, THE Backend SHALL create a row in the `contests` table with the Contest_Engine's standard fields and SHALL create a linked row in `private_contests` with `host_user_id`, `created_at`, and `enable_proctoring`.
7. WHEN `enable_proctoring` is `TRUE`, THE Backend SHALL also create a row in the existing `proctored_contests` table referencing the `contests` row, reusing the proctoring infrastructure.
8. THE Backend SHALL generate a unique Invite_Token (cryptographically random, at least 32 bytes, base64url-encoded) for the Private_Contest and store it in `private_contest_invitations` with `contest_id`, `token`, `created_at`, and `expires_at` (default: 30 days from creation).
9. THE Backend SHALL return the created Private_Contest's `id`, `name`, `start_time`, `end_time`, and the Invite_Link in the response: `https://codecoder.in/contest/private/join?token={Invite_Token}`.

### Requirement 5: Invite link generation and management

**User Story:** As a Contest_Host, I want to generate and share a unique invite link for my Private_Contest, so that participants can join without manual approval.

#### Acceptance Criteria

1. WHEN a Private_Contest is created, THE Backend SHALL automatically generate an Invite_Link with a unique Invite_Token as described in Requirement 4.
2. THE Backend SHALL provide an API endpoint that allows the Contest_Host to regenerate the Invite_Token, invalidating the previous token and creating a new one with a fresh `expires_at` timestamp.
3. WHEN the Contest_Host regenerates an Invite_Token, THE Backend SHALL mark the old token row as `invalidated: TRUE` and create a new row with a new token value.
4. THE Backend SHALL provide an API endpoint that allows the Contest_Host to set a custom `expires_at` timestamp for the Invite_Token (minimum: current time, maximum: contest `end_time`).
5. THE Frontend SHALL display the Invite_Link prominently on the Private_Contest management page with a "Copy to Clipboard" button.
6. THE Frontend SHALL display the Invite_Link expiration time and provide a "Regenerate Link" button.

### Requirement 6: Participant invitation acceptance via invite link

**User Story:** As a User, I want to join a Private_Contest by clicking an invite link, so that I can participate without waiting for manual approval.

#### Acceptance Criteria

1. WHEN a User (authenticated or unauthenticated) navigates to an Invite_Link, THE Frontend SHALL display the Private_Contest name, description, start time, end time, and host username.
2. IF the User is not authenticated, THEN THE Frontend SHALL redirect to the login page with a return URL set to the Invite_Link.
3. WHEN an authenticated User clicks "Accept Invitation", THE Backend SHALL validate the Invite_Token: it must exist, not be invalidated, not be expired, and reference a Private_Contest with status `UPCOMING` or `LIVE`.
4. IF the Invite_Token is invalid or expired, THEN THE Backend SHALL reject the request with HTTP `404 Not Found` and message "This invitation link is invalid or has expired".
5. IF the Private_Contest has already reached the Participant_Limit of 100, THEN THE Backend SHALL reject the request with HTTP `429 Too Many Requests` and message "This contest has reached its maximum capacity of 100 participants".
6. WHEN validation passes, THE Backend SHALL create a row in `private_contest_participants` with `contest_id`, `user_id`, and `joined_at`.
7. THE Backend SHALL enforce a unique constraint on `(contest_id, user_id)` so that a User cannot join the same Private_Contest multiple times.
8. WHEN a User successfully joins, THE Frontend SHALL redirect to the Private_Contest detail page.

### Requirement 7: Participant list visibility

**User Story:** As a Contest_Host, I want to view the list of participants who have joined my Private_Contest, so that I can track attendance and remove participants if needed.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that returns the list of Participants for a Private_Contest, including `username`, `email`, `full_name`, and `joined_at`, only when the requesting User is the Contest_Host.
2. IF a User who is not the Contest_Host attempts to access the participant list, THEN THE Backend SHALL return HTTP `403 Forbidden`.
3. THE Frontend SHALL display the participant list on the Private_Contest management page with columns for username, email, joined date, and a "Remove" action.
4. WHEN the Contest_Host clicks "Remove" on a Participant, THE Backend SHALL delete the corresponding `private_contest_participants` row, preventing that User from accessing the Private_Contest.
5. THE Backend SHALL NOT allow removing a Participant after the Private_Contest status transitions to `LIVE`.

### Requirement 8: Problem selection from existing problem bank

**User Story:** As a Contest_Host, I want to browse and select problems from the existing CodeCoder problem bank to add to my Private_Contest, so that I can reuse high-quality problems without creating new ones.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that returns a paginated, filterable list of all problems marked as `visibility: PUBLIC` or `visibility: PRIVATE_AVAILABLE`, excluding problems marked as `visibility: ADMIN_ONLY`.
2. THE Frontend SHALL display a problem browser on the Private_Contest management page with filters for difficulty, tags, and search by title.
3. WHEN the Contest_Host selects a problem, THE Frontend SHALL add it to a "Selected Problems" list without immediately persisting the selection.
4. WHEN the Contest_Host clicks "Save Problem Selection", THE Backend SHALL create rows in the existing `contest_problems` junction table linking the Private_Contest to the selected problems.
5. THE Backend SHALL reuse the existing many-to-many relationship between `contests` and `problems` defined by the `contest_problems` table.
6. THE Backend SHALL allow the Contest_Host to reorder the selected problems, and SHALL persist the order in a `display_order` column in `contest_problems`.
7. THE Backend SHALL NOT allow the Contest_Host to modify the problem selection after the Private_Contest status transitions to `LIVE`.

### Requirement 9: AI-assisted problem generation

**User Story:** As a Contest_Host, I want to generate new coding problems using the AI Problem Generator, so that I can create custom problems tailored to my contest theme.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that accepts a natural language prompt (max 1000 characters) and optional parameters `difficulty` (enum: `EASY`, `MEDIUM`, `HARD`) and `topic` (string, max 100 chars).
2. WHEN the Contest_Host submits a generation request, THE Backend SHALL invoke the existing AI_Problem_Generator service and return the generated problem as a structured response with fields: `title`, `description`, `input_format`, `output_format`, `constraints`, `sample_test_cases`, and `hidden_test_cases`.
3. THE Frontend SHALL display the generated problem in a preview pane with an "Accept" button and an "Edit" button.
4. WHEN the Contest_Host clicks "Accept", THE Backend SHALL create a row in the `problems` table with `visibility: PRIVATE_OWNED`, `created_by: {Contest_Host user_id}`, and the generated content.
5. WHEN the Contest_Host clicks "Edit", THE Frontend SHALL populate an editable problem form with the generated content, allowing the Contest_Host to modify any field before saving.
6. THE Backend SHALL apply a rate limit of 5 problem generation requests per Contest_Host per day, enforced via a Cache-backed counter.
7. IF the Contest_Host exceeds the rate limit, THEN THE Backend SHALL return HTTP `429 Too Many Requests` with message "You have reached your daily limit of 5 AI-generated problems".
8. THE generated problem SHALL be automatically added to the "Selected Problems" list for the Private_Contest, but SHALL NOT be persisted to the `contest_problems` junction table until the Contest_Host clicks "Save Problem Selection" as defined in Requirement 8.

### Requirement 10: Problem editing for AI-generated and custom problems

**User Story:** As a Contest_Host, I want to edit problems I have created or generated, so that I can fix errors or adjust difficulty before the contest starts.

#### Acceptance Criteria

1. THE Backend SHALL allow a Contest_Host to edit any problem where `created_by` equals their `user_id` and the problem is not yet attached to a `LIVE` or `ENDED` contest.
2. WHEN the Contest_Host edits a problem, THE Backend SHALL update the `problems` row with the new content and set `updated_at` to the current timestamp.
3. THE Backend SHALL NOT allow editing problems with `visibility: PUBLIC` unless the Contest_Host is also an Admin.
4. THE Backend SHALL NOT allow editing a problem after it has been attached to a Private_Contest that has transitioned to `LIVE` status.

### Requirement 11: Private contest visibility and access control

**User Story:** As a Participant, I want to see only the Private_Contests I have joined, and as a Contest_Host, I want to see only the Private_Contests I have created, so that contests are isolated.

#### Acceptance Criteria

1. THE Backend SHALL NOT include Private_Contests in the public contest list API endpoint that returns Public_Contests.
2. THE Backend SHALL provide a separate API endpoint `/api/contests/private/my-contests` that returns Private_Contests where the requesting User is the Contest_Host.
3. THE Backend SHALL provide a separate API endpoint `/api/contests/private/joined` that returns Private_Contests where the requesting User is a Participant.
4. WHEN a User attempts to access a Private_Contest detail page, THE Backend SHALL verify that the User is either the Contest_Host or a Participant; if neither, THE Backend SHALL return HTTP `403 Forbidden`.
5. WHEN a User attempts to submit code to a Private_Contest, THE Backend SHALL verify that the User is a Participant and that the contest status is `LIVE`; if not, THE Backend SHALL return HTTP `403 Forbidden` or HTTP `409 Conflict` respectively.

### Requirement 12: Contest lifecycle and status transitions

**User Story:** As the System, I want Private_Contests to follow the same lifecycle as Public_Contests, so that the existing Contest_Engine can manage both types uniformly.

#### Acceptance Criteria

1. THE Backend SHALL reuse the existing `contests.status` enum with values `UPCOMING`, `LIVE`, and `ENDED`.
2. THE Scheduler SHALL transition Private_Contest status from `UPCOMING` to `LIVE` when `start_time` is reached, using the same `@Scheduled` method as Public_Contests.
3. THE Scheduler SHALL transition Private_Contest status from `LIVE` to `ENDED` when `end_time` is reached, using the same `@Scheduled` method as Public_Contests.
4. WHEN a Private_Contest transitions to `LIVE`, THE Backend SHALL initialize the Leaderboard cache key `private:leaderboard:{contestId}` as an empty sorted set.
5. WHEN a Private_Contest transitions to `ENDED`, THE Backend SHALL freeze the Leaderboard and persist final rankings to the Database.

### Requirement 13: Submission flow integration with existing judge queue

**User Story:** As a Participant, I want my code submissions in a Private_Contest to be judged with the same reliability and speed as Public_Contest submissions, so that the contest experience is consistent.

#### Acceptance Criteria

1. WHEN a Participant submits code to a Private_Contest problem, THE Backend SHALL create a row in the existing `submissions` table with `contest_id`, `user_id`, `problem_id`, `code`, `language`, and `status: PENDING`.
2. THE Backend SHALL push the submission job to the `private:submission:queue` Valkey list (separate from the public `submission:queue`).
3. THE Judge_Worker SHALL drain both `submission:queue` (public contests) and `private:submission:queue` (private contests) using a fair round-robin or priority-weighted strategy defined in the design phase.
4. THE Judge_Worker SHALL execute the submission code in the same sandboxed environment (bwrap + prlimit) as public contest submissions.
5. WHEN the Judge_Worker completes execution, THE Judge_Worker SHALL write the verdict (`ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `MEMORY_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR`) to the `submissions` row.
6. THE Backend SHALL reuse the existing SSE (Server-Sent Events) mechanism to push the verdict to the Participant's browser in real time.
7. THE Backend SHALL update the `private:leaderboard:{contestId}` sorted set with the Participant's score using the same scoring logic as Public_Contests (points per problem, penalty for wrong submissions, tie-breaking by submission time).

### Requirement 14: Leaderboard isolation and caching

**User Story:** As a Participant, I want to view a real-time leaderboard showing only participants in my Private_Contest, so that I can track my rank against my peers.

#### Acceptance Criteria

1. THE Backend SHALL maintain a separate Valkey sorted set for each Private_Contest with key `private:leaderboard:{contestId}`.
2. WHEN a Participant submits an accepted solution, THE Backend SHALL update the `private:leaderboard:{contestId}` sorted set with the Participant's updated score and submission time.
3. THE Backend SHALL provide an API endpoint `/api/contests/private/{contestId}/leaderboard` that returns the sorted leaderboard with columns: `rank`, `username`, `score`, `penalty`, `last_submission_time`.
4. THE Frontend SHALL display the leaderboard on the Private_Contest detail page, auto-refreshing every 10 seconds while the contest is `LIVE`.
5. THE Backend SHALL NOT include Private_Contest leaderboards in the global leaderboard or any cross-contest ranking.

### Requirement 15: Proctoring integration for private contests

**User Story:** As a Contest_Host, I want to enable proctoring for my Private_Contest, so that I can enforce integrity monitoring for high-stakes assessments.

#### Acceptance Criteria

1. WHEN a Contest_Host creates a Private_Contest with `enable_proctoring: TRUE`, THE Backend SHALL create a row in the existing `proctored_contests` table referencing the `contests` row.
2. THE Backend SHALL reuse the entire Proctored Contest Mode feature defined in the `proctored-contest-mode` spec, including consent screens, pre-flight checks, face detection, screenshot capture, event ingestion, and risk scoring.
3. THE Contest_Host SHALL have access to the Admin_Dashboard for their Private_Contest, showing participant proctoring sessions, risk scores, and screenshots.
4. THE Contest_Host SHALL NOT have access to proctoring data for other Contest_Hosts' Private_Contests or for Public_Contests.
5. THE Backend SHALL enforce that only the Contest_Host or an Admin can view proctoring data for a Private_Contest.

### Requirement 16: Analytics dashboard for contest hosts

**User Story:** As a Contest_Host, I want to view analytics for my Private_Contest, including participation rate, submission stats, and problem difficulty distribution, so that I can evaluate contest quality.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint `/api/contests/private/{contestId}/analytics` that returns:
   - Total invited participants (count of `private_contest_participants` rows)
   - Active participants (count of participants with at least one submission)
   - Total submissions (count of `submissions` rows for this contest)
   - Submissions per problem (grouped by `problem_id`)
   - Acceptance rate per problem (ratio of `ACCEPTED` to total submissions)
   - Average solve time per problem
   - Participant engagement timeline (submission count per 15-minute interval)
2. THE Frontend SHALL display the analytics on a dedicated "Analytics" tab within the Private_Contest management page.
3. THE Backend SHALL allow the Contest_Host to export analytics as a CSV file.
4. THE Backend SHALL cache analytics data for `ENDED` contests in Valkey with a TTL of 24 hours.

### Requirement 17: Contest host notification system

**User Story:** As a Contest_Host, I want to receive email notifications for important events in my Private_Contest lifecycle, so that I stay informed without constantly checking the dashboard.

#### Acceptance Criteria

1. WHEN a Private_Contest is successfully created, THE Backend SHALL send an email to the Contest_Host with the contest details and Invite_Link.
2. WHEN the first Participant joins a Private_Contest, THE Backend SHALL send an email to the Contest_Host confirming the first registration.
3. WHEN a Private_Contest transitions to `LIVE` status, THE Backend SHALL send an email to the Contest_Host with a link to the live dashboard.
4. WHEN a Private_Contest transitions to `ENDED` status, THE Backend SHALL send an email to the Contest_Host with a summary: total participants, total submissions, and a link to the Analytics_Dashboard.
5. THE Backend SHALL allow the Contest_Host to configure notification preferences (opt-out per notification type) via a settings page.

### Requirement 18: Contest cancellation by host

**User Story:** As a Contest_Host, I want to cancel my Private_Contest before it starts, so that I can adjust plans if circumstances change.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that allows the Contest_Host to cancel a Private_Contest with status `UPCOMING`.
2. WHEN a Contest_Host cancels a Private_Contest, THE Backend SHALL update the `private_contests` row with `cancelled: TRUE` and `cancelled_at` timestamp.
3. THE Backend SHALL send an email notification to all Participants informing them of the cancellation with an optional reason message from the Contest_Host.
4. THE Backend SHALL invalidate all Invite_Tokens for the cancelled Private_Contest.
5. THE Backend SHALL NOT allow cancellation after the Private_Contest transitions to `LIVE` status; instead, the Contest_Host must wait for the contest to end naturally or contact an Admin.
6. THE cancelled Private_Contest SHALL still count toward the Contest_Host's Monthly_Quota as defined in Requirement 3.

### Requirement 19: Admin oversight and moderation

**User Story:** As an Admin, I want to view all Private_Contests across all Contest_Hosts, suspend or delete contests that violate policies, and revoke hosting privileges, so that the platform remains compliant.

#### Acceptance Criteria

1. THE Backend SHALL provide an admin API endpoint `/api/admin/private-contests` that returns all Private_Contests with columns: `contest_id`, `host_username`, `name`, `status`, `participant_count`, `created_at`, `start_time`, `end_time`.
2. THE Backend SHALL allow Admins to view the full details of any Private_Contest, including participant list, problem list, submissions, and analytics, regardless of whether the Admin is a Participant.
3. THE Backend SHALL provide an API endpoint that allows an Admin to delete a Private_Contest, which SHALL cascade-delete all related rows in `private_contests`, `private_contest_participants`, `private_contest_invitations`, and optionally the `contests` row if it has no submissions.
4. THE Backend SHALL provide an API endpoint that allows an Admin to revoke a Contest_Host's hosting privileges by updating their `contest_hosting_requests` row status from `APPROVED` to `REVOKED`.
5. WHEN a Contest_Host's privileges are revoked, THE Backend SHALL prevent them from creating new Private_Contests and SHALL send them an email notification with the reason.
6. THE Backend SHALL NOT automatically cancel existing Private_Contests when a Contest_Host is revoked; Admins must delete those contests separately if needed.

### Requirement 20: Database schema isolation

**User Story:** As a Database administrator, I want Private_Contest tables to coexist with existing contest tables without schema conflicts, so that migrations are safe and rollback is possible.

#### Acceptance Criteria

1. THE Database SHALL add a new table `contest_hosting_requests` with columns: `id`, `user_id`, `status`, `reason`, `intended_use_case`, `submitted_at`, `reviewed_by`, `reviewed_at`, `admin_notes`.
2. THE Database SHALL add a new table `private_contests` with columns: `id`, `contest_id` (unique FK to `contests.id`), `host_user_id` (FK to `users.id`), `created_at`, `cancelled`, `cancelled_at`, `enable_proctoring`.
3. THE Database SHALL add a new table `private_contest_invitations` with columns: `id`, `contest_id`, `token`, `created_at`, `expires_at`, `invalidated`.
4. THE Database SHALL add a new table `private_contest_participants` with columns: `id`, `contest_id`, `user_id`, `joined_at`, and unique constraint on `(contest_id, user_id)`.
5. THE Database SHALL NOT modify the existing `contests`, `problems`, `submissions`, `contest_problems`, or `proctored_contests` tables.
6. THE Database SHALL define foreign key constraints with `ON DELETE CASCADE` for `private_contests.contest_id`, `private_contest_invitations.contest_id`, and `private_contest_participants.contest_id`.

### Requirement 21: Cache key prefixing for isolation

**User Story:** As the System, I want Private_Contest cache keys to use a distinct prefix from Public_Contest cache keys, so that cache invalidation and TTL management do not conflict.

#### Acceptance Criteria

1. THE Backend SHALL use the cache key prefix `private:contest:{contestId}:*` for all Private_Contest hot state.
2. THE Backend SHALL use the cache key prefix `private:leaderboard:{contestId}` for Private_Contest leaderboards.
3. THE Backend SHALL use the cache key prefix `private:submission:queue` for the Private_Contest judge queue.
4. THE Backend SHALL use the cache key prefix `private:rate-limit:{userId}:{resource}` for Contest_Host rate limits (e.g., AI problem generation).
5. THE Backend SHALL document all cache key patterns in the design phase with example keys and TTL values.

### Requirement 22: Judge worker VM resource allocation

**User Story:** As the System, I want the dedicated Judge_Worker VM to process both public and private contest submissions fairly, so that neither contest type experiences starvation.

#### Acceptance Criteria

1. THE Judge_Worker SHALL drain jobs from both `submission:queue` (public) and `private:submission:queue` (private) using a weighted round-robin or priority strategy defined in the design phase.
2. THE Judge_Worker SHALL log the queue it is draining for each job (public vs. private) for observability.
3. THE Backend SHALL provide a monitoring endpoint `/api/admin/judge-stats` that returns: queue lengths for public and private queues, average processing time per queue, and worker idle vs. busy ratio.
4. THE Judge_Worker SHALL respect the same resource limits (CPU, memory, time) for private contest submissions as for public contest submissions.

### Requirement 23: Authentication and authorization

**User Story:** As the System, I want all Private_Contest API endpoints to enforce JWT authentication and role-based access control, so that only authorized users can create, join, or manage contests.

#### Acceptance Criteria

1. THE Backend SHALL require a valid JWT token for all API endpoints under `/api/contests/private/*`.
2. THE Backend SHALL verify that the JWT `user_id` matches the `host_user_id` of a Private_Contest for all Contest_Host-only operations (create, edit, cancel, view participants, view analytics).
3. THE Backend SHALL verify that the JWT `user_id` exists in `private_contest_participants` for a given `contest_id` before allowing contest access, problem viewing, or submission.
4. THE Backend SHALL allow Admins (users with `ROLE_ADMIN`) to access all Private_Contest endpoints regardless of ownership or participation.
5. IF a JWT is expired or invalid, THEN THE Backend SHALL return HTTP `401 Unauthorized`.
6. IF a User attempts an operation they are not authorized for, THEN THE Backend SHALL return HTTP `403 Forbidden` with a descriptive message.

### Requirement 24: Rate limiting and abuse prevention

**User Story:** As an Operator, I want rate limits on contest creation, AI problem generation, and invite link regeneration, so that malicious or buggy clients cannot degrade service.

#### Acceptance Criteria

1. THE Backend SHALL apply a rate limit of 5 contest creation requests per Contest_Host per hour, enforced via a Cache-backed sliding window counter.
2. THE Backend SHALL apply a rate limit of 5 AI problem generation requests per Contest_Host per day as defined in Requirement 9.
3. THE Backend SHALL apply a rate limit of 10 invite link regenerations per Private_Contest per hour.
4. THE Backend SHALL apply a rate limit of 100 invite link acceptances per Private_Contest per hour to prevent automated bot registrations.
5. WHEN a rate limit is exceeded, THE Backend SHALL return HTTP `429 Too Many Requests` with a `Retry-After` header indicating when the limit resets.
6. THE Backend SHALL log rate limit violations to a monitoring service for abuse detection.

### Requirement 25: Scalability and performance targets

**User Story:** As an Operator, I want the Private_Contest feature to scale to the MVP target concurrency without requiring additional VMs or database upgrades, so that deployment costs remain predictable.

#### Acceptance Criteria

1. THE Backend SHALL support 50 concurrent Private_Contests with an average of 30 Participants each (1,500 concurrent users across private contests) on the existing VM1 infrastructure.
2. THE Judge_Worker VM SHALL process submissions from both public and private queues with an average latency of less than 5 seconds per submission under normal load.
3. THE Backend SHALL cache Private_Contest metadata (name, description, start/end times, host info) in Valkey with a TTL of 10 minutes to reduce Database load.
4. THE Backend SHALL use database connection pooling (HikariCP) with a maximum pool size of 20 connections, shared between public and private contest queries.
5. THE design SHALL document the scaling path to 200 concurrent Private_Contests, including cache eviction policies, database indexing strategies, and judge worker autoscaling.

### Requirement 26: Invite token expiration and cleanup

**User Story:** As the System, I want expired Invite_Tokens to be automatically cleaned up, so that the Database does not accumulate stale rows.

#### Acceptance Criteria

1. THE Scheduler SHALL run a daily cleanup job at 02:00 UTC that deletes all rows from `private_contest_invitations` where `expires_at < current_timestamp` and `invalidated: TRUE`.
2. THE Scheduler SHALL log the count of deleted tokens for observability.
3. WHEN a User attempts to use an expired Invite_Token, THE Backend SHALL return HTTP `410 Gone` with message "This invitation link has expired".

### Requirement 27: Email notification templates

**User Story:** As a Contest_Host and Participant, I want to receive well-formatted, branded email notifications for contest events, so that I recognize official communications from CodeCoder.

#### Acceptance Criteria

1. THE Backend SHALL use the existing email service (`MailConfig`, `noreplyMailSender`) for all Private_Contest notifications.
2. THE Backend SHALL provide HTML email templates for:
   - Contest_Host: Hosting request approved
   - Contest_Host: Hosting request rejected (with reason)
   - Contest_Host: Private_Contest created successfully (with Invite_Link)
   - Contest_Host: First participant joined
   - Contest_Host: Contest started (with live dashboard link)
   - Contest_Host: Contest ended (with analytics link)
   - Participant: Contest cancelled (with reason from host)
   - Admin: New hosting request submitted (with approval dashboard link)
3. THE email templates SHALL include the CodeCoder logo, sender name "CodeCoder Team", and footer with unsubscribe link.
4. THE Backend SHALL send emails asynchronously using `@Async` to avoid blocking API responses.

### Requirement 28: Frontend UI/UX for private contests

**User Story:** As a Contest_Host, I want a dedicated section in the CodeCoder frontend for managing my Private_Contests, so that I can create, configure, and monitor contests without navigating through admin panels.

#### Acceptance Criteria

1. THE Frontend SHALL add a new navigation menu item "My Private Contests" visible only to approved Contest_Hosts (users with an approved Hosting_Request).
2. THE Frontend SHALL provide a "Request Hosting" button on the user profile page for Verified_Users who have not yet submitted a Hosting_Request.
3. THE Frontend SHALL display a "Create Private Contest" button on the "My Private Contests" page, showing the current Monthly_Quota usage (e.g., "1/2 contests used this month").
4. THE Frontend SHALL provide a Private_Contest creation wizard with steps: (1) Basic Info (name, description, times), (2) Problem Selection (browse or generate), (3) Settings (proctoring, invite link), (4) Review & Create.
5. THE Frontend SHALL display a Private_Contest management dashboard with tabs: Overview, Participants, Problems, Leaderboard, Analytics, Settings.
6. THE Frontend SHALL provide a shareable Invite_Link with a "Copy" button and QR code generator for easy distribution.
7. THE Frontend SHALL display a countdown timer on the Private_Contest detail page showing time until contest starts (for `UPCOMING`) or time remaining (for `LIVE`).

### Requirement 29: Audit logging for compliance

**User Story:** As an Admin, I want all critical actions in the Private_Contest lifecycle to be logged for compliance and debugging, so that I can investigate disputes or abuse.

#### Acceptance Criteria

1. THE Backend SHALL log the following events to an `audit_logs` table with columns `(id, user_id, action, resource_type, resource_id, timestamp, ip_address, user_agent, details_json)`:
   - Hosting request submitted
   - Hosting request approved/rejected by admin
   - Private_Contest created
   - Private_Contest cancelled by host
   - Private_Contest deleted by admin
   - Participant joined via invite link
   - Participant removed by host
   - Problem added/removed from contest
   - Invite link regenerated
   - Contest_Host privileges revoked by admin
2. THE Backend SHALL retain audit logs for a minimum of 90 days.
3. THE Backend SHALL provide an admin API endpoint `/api/admin/audit-logs` with filters for `user_id`, `action`, `resource_type`, and date range.

### Requirement 30: Problem visibility and reuse policies

**User Story:** As a Contest_Host, I want to understand which problems I can reuse in my Private_Contests and which problems are restricted, so that I comply with content licensing.

#### Acceptance Criteria

1. THE Database SHALL add a `visibility` column to the `problems` table with enum values: `PUBLIC`, `PRIVATE_AVAILABLE`, `PRIVATE_OWNED`, `ADMIN_ONLY`.
2. THE Backend SHALL allow Contest_Hosts to select problems with `visibility: PUBLIC` or `visibility: PRIVATE_AVAILABLE` when browsing the problem bank.
3. THE Backend SHALL NOT allow Contest_Hosts to select problems with `visibility: ADMIN_ONLY`.
4. WHEN a Contest_Host generates a problem using the AI_Problem_Generator, THE Backend SHALL set the problem's `visibility: PRIVATE_OWNED` and `created_by: {Contest_Host user_id}`.
5. THE Backend SHALL allow Contest_Hosts to edit or delete only problems where `created_by` equals their `user_id`.
6. THE Backend SHALL allow Admins to change any problem's `visibility` value via the admin problem management panel.

### Requirement 31: Participant notifications

**User Story:** As a Participant, I want to receive email reminders before a Private_Contest starts and notifications when the contest ends, so that I don't miss important deadlines.

#### Acceptance Criteria

1. THE Scheduler SHALL send an email reminder to all Participants of a Private_Contest 24 hours before the `start_time`.
2. THE Scheduler SHALL send an email reminder to all Participants of a Private_Contest 1 hour before the `start_time`.
3. WHEN a Private_Contest transitions to `LIVE`, THE Backend SHALL send an email to all Participants with a link to the contest page.
4. WHEN a Private_Contest transitions to `ENDED`, THE Backend SHALL send an email to all Participants with their final rank and score.
5. THE Backend SHALL allow Participants to opt out of reminder emails via their user profile settings, but SHALL always send the contest start and end notifications.

### Requirement 32: Contest host dashboard metrics

**User Story:** As a Contest_Host, I want to see real-time metrics during a live Private_Contest, so that I can monitor engagement and intervene if needed.

#### Acceptance Criteria

1. THE Backend SHALL provide a WebSocket endpoint for Contest_Hosts that streams real-time updates for their Private_Contest, including:
   - Participant count (joined vs. active)
   - Submission count (total and per problem)
   - Current leaderboard top 10
   - Recent submissions (last 20)
2. THE Frontend SHALL display these metrics on the "Overview" tab of the Private_Contest management dashboard.
3. THE Frontend SHALL auto-update metrics every 5 seconds while the contest is `LIVE`.
4. THE Backend SHALL enforce that only the Contest_Host or an Admin can connect to the WebSocket endpoint for a given Private_Contest.

### Requirement 33: Contest cloning for reuse

**User Story:** As a Contest_Host, I want to clone an existing Private_Contest to reuse the same problem set and settings for a future contest, so that I don't have to reconfigure everything manually.

#### Acceptance Criteria

1. THE Backend SHALL provide an API endpoint that allows a Contest_Host to clone one of their own ended Private_Contests.
2. WHEN a Contest_Host clones a Private_Contest, THE Backend SHALL create a new `contests` row and `private_contests` row with the same name (appended with " (Copy)"), description, problem list, and proctoring settings.
3. THE Backend SHALL NOT copy the participant list, submissions, or leaderboard from the original contest.
4. THE Backend SHALL generate a new Invite_Token for the cloned contest.
5. THE cloned contest SHALL have status `UPCOMING` and SHALL require the Contest_Host to set new `start_time` and `end_time` values before saving.
6. THE cloned contest SHALL count toward the Contest_Host's Monthly_Quota.

### Requirement 34: Contest template library

**User Story:** As a Contest_Host, I want to browse pre-made contest templates (e.g., "Beginner Algorithms", "Advanced Data Structures") curated by Admins, so that I can quickly create a contest without selecting problems manually.

#### Acceptance Criteria

1. THE Database SHALL add a new table `contest_templates` with columns: `id`, `name`, `description`, `difficulty`, `problem_ids` (JSON array), `estimated_duration_minutes`, `created_by_admin_id`.
2. THE Backend SHALL provide an API endpoint `/api/contests/private/templates` that returns all published contest templates.
3. THE Frontend SHALL display contest templates in the Private_Contest creation wizard with a "Use Template" button.
4. WHEN a Contest_Host selects a template, THE Frontend SHALL pre-populate the problem selection and duration fields with the template values.
5. THE Backend SHALL allow Admins to create, edit, and delete contest templates via the admin panel.

### Requirement 35: Integration with existing user points system

**User Story:** As a Participant, I want to earn points for solving problems in Private_Contests, so that my profile reflects my achievements across all contest types.

#### Acceptance Criteria

1. WHEN a Participant submits an accepted solution to a problem in a Private_Contest, THE Backend SHALL increment the Participant's `total_points` field in the `users` table by the problem's point value.
2. THE Backend SHALL use the same point calculation logic for Private_Contests as for Public_Contests (points per problem based on difficulty, no penalty deduction from total_points).
3. THE Backend SHALL record the points earned in the `submissions` table or a separate `user_points_history` table for audit purposes.
4. THE Backend SHALL display the Participant's updated total points on their user profile and contest leaderboard.

### Requirement 36: Support for team-based private contests (future extension placeholder)

**User Story:** As a Contest_Host, I want to create team-based Private_Contests in a future version, so that I can organize collaborative coding challenges.

#### Acceptance Criteria

1. THE Database schema for `private_contests` and `private_contest_participants` SHALL be designed to allow future addition of a `team_id` foreign key without breaking existing contests.
2. THE Backend API design SHALL reserve a `team_mode` field (boolean or enum) in the Private_Contest creation request for future use, but SHALL reject requests with `team_mode: TRUE` in the MVP with HTTP `501 Not Implemented`.
3. THE design document SHALL outline the extension path for team support, including team creation, team invitations, shared leaderboard scoring, and submission attribution.

### Requirement 37: Multi-language support for contest UI

**User Story:** As a Contest_Host or Participant, I want the Private_Contest UI to be available in my preferred language, so that non-English speakers can use the platform.

#### Acceptance Criteria

1. THE Frontend SHALL support internationalization (i18n) for Private_Contest UI text using the existing `react-i18next` library (or equivalent).
2. THE Frontend SHALL provide translations for at least English and one additional language (e.g., Hindi, Spanish) in the MVP.
3. THE Backend SHALL store contest `name` and `description` in a `translations` JSON column to support multiple languages per contest.
4. THE Backend SHALL return contest text in the language specified by the `Accept-Language` header or a query parameter `lang`.

### Requirement 38: Backup and disaster recovery

**User Story:** As an Operator, I want Private_Contest data to be included in regular database backups, so that contest data can be restored in case of failure.

#### Acceptance Criteria

1. THE Database backup strategy SHALL include all new tables: `contest_hosting_requests`, `private_contests`, `private_contest_invitations`, `private_contest_participants`, `contest_templates`, and `audit_logs`.
2. THE Backend SHALL log a warning if a backup is older than 24 hours, visible in the monitoring dashboard.
3. THE Operator documentation SHALL include a disaster recovery runbook for restoring Private_Contest data from backups.

### Requirement 39: Performance monitoring and alerting

**User Story:** As an Operator, I want to monitor Private_Contest performance metrics and receive alerts for anomalies, so that I can respond to issues before users are impacted.

#### Acceptance Criteria

1. THE Backend SHALL expose Prometheus metrics for:
   - Private_Contest creation rate (per hour)
   - Invite link acceptance rate (per hour)
   - Private submission queue length
   - Average judge latency for private submissions
   - Active Private_Contests count
   - Active Participants count (across all Private_Contests)
2. THE Backend SHALL integrate with the existing monitoring stack (if any) or provide a `/metrics` endpoint for Prometheus scraping.
3. THE Operator SHALL configure alerts for:
   - Private submission queue length > 100 for more than 5 minutes
   - Average judge latency > 10 seconds for more than 5 minutes
   - Contest creation failure rate > 10% over 1 hour

### Requirement 40: Documentation and help resources

**User Story:** As a Contest_Host, I want comprehensive documentation on how to create and manage Private_Contests, so that I can use the feature effectively without support.

#### Acceptance Criteria

1. THE Platform SHALL provide a "Help" section in the Frontend with articles on:
   - How to request hosting privileges
   - How to create a Private_Contest
   - How to generate and share Invite_Links
   - How to select problems and use the AI_Problem_Generator
   - How to enable proctoring
   - How to interpret the Analytics_Dashboard
2. THE Backend SHALL provide API documentation (Swagger/OpenAPI) for all Private_Contest endpoints.
3. THE Platform SHALL provide video tutorials or a guided tour for first-time Contest_Hosts.
