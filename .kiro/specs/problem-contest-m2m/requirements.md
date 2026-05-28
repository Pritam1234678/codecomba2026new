# Requirements Document

## Introduction

The current platform binds every Problem to exactly one Contest through a
not-null `problems.contest_id` foreign key. Admins can only create a problem
from inside a contest, and a problem cannot be reused across events. This
spec captures the requirements to evolve that relationship into a true
many-to-many: problems become first-class entities admins can create and
manage independently, and a contest is treated as a curated selection of
existing problems. The change ships as a production-safe, online migration
with backfill, dual-write transition window for backward compatibility, and
a planned future drop of the legacy column.

## Glossary

- **System** — The CodeCombat 2026 web application as a whole (Spring Boot
  backend + React frontend + PostgreSQL).
- **Backend** — The Spring Boot 3.5.14 application running on Java 21.
- **Frontend** — The React admin SPA in `frontend/`.
- **Database** — The PostgreSQL 18 instance managed by Flyway.
- **Migration_V5** — The Flyway migration file
  `V5__contest_problems_m2m.sql`.
- **Junction_Table** — The new `contest_problems` table introduced by
  Migration_V5, with primary key `(contest_id, problem_id)` and payload
  columns `display_order` and `added_at`.
- **Legacy_Column** — The pre-existing `problems.contest_id` column, made
  nullable by Migration_V5 and treated as deprecated by the application.
- **Standalone_Problem** — A problem whose Legacy_Column is NULL and which
  has zero rows in the Junction_Table at the moment of inspection.
- **Admin** — An authenticated user with `ROLE_ADMIN`.
- **Contest_Problem_Service** — The Spring `@Service` that owns attach,
  detach, and dual-write logic.
- **Attach** — Inserting a row into the Junction_Table for a given
  `(contest_id, problem_id)` pair.
- **Detach** — Deleting a row from the Junction_Table for a given
  `(contest_id, problem_id)` pair, without deleting the problem itself.
- **Dual_Write** — During the transition window, every Attach also updates
  the Legacy_Column when it is currently NULL, so legacy code paths
  continue to observe a non-null value.
- **Browse_Existing_Modal** — The new UI element in
  `ManageContestProblems.jsx` that lets admins multi-select problems from
  the standalone pool to attach to the current contest.

## Requirements

### Requirement 1: Junction table schema

**User Story:** As a Database administrator, I want the contest-problem
relationship represented in a dedicated junction table, so that one problem
can belong to multiple contests at once.

#### Acceptance Criteria

1. WHEN Migration_V5 runs against a Database that does not yet contain the
   Junction_Table, THE Database SHALL create a table named
   `contest_problems` with columns `contest_id BIGINT NOT NULL`,
   `problem_id BIGINT NOT NULL`, `display_order INTEGER NOT NULL DEFAULT 0`,
   and `added_at TIMESTAMP NOT NULL DEFAULT NOW()`.
2. WHEN Migration_V5 runs, THE Database SHALL define the primary key of
   `contest_problems` as the composite `(contest_id, problem_id)`.
3. WHEN Migration_V5 runs, THE Database SHALL create a foreign key from
   `contest_problems.contest_id` to `contests(id)` with `ON DELETE
   CASCADE`.
4. WHEN Migration_V5 runs, THE Database SHALL create a foreign key from
   `contest_problems.problem_id` to `problems(id)` with `ON DELETE
   CASCADE`.
5. WHEN Migration_V5 runs, THE Database SHALL create an index named
   `idx_contest_problems_problem_id` on `contest_problems(problem_id)`.
6. WHEN Migration_V5 runs against a Database that already contains the
   Junction_Table from a previous partial run, THE Database SHALL complete
   without error and leave the existing table unchanged.

### Requirement 2: Backfill of existing memberships

**User Story:** As a Platform operator, I want every existing
problem-contest association preserved when Migration_V5 runs, so that no
contest loses problems during the upgrade.

#### Acceptance Criteria

1. WHEN Migration_V5 runs, THE Database SHALL insert one row into
   `contest_problems` for every row in `problems` where `contest_id IS NOT
   NULL`, using the existing `contest_id` as `contest_problems.contest_id`,
   the existing `id` as `contest_problems.problem_id`, `0` as
   `display_order`, and the current transaction timestamp as `added_at`.
2. WHILE the backfill runs, THE Database SHALL skip any
   `(contest_id, problem_id)` pair that already exists in
   `contest_problems` rather than failing the migration.
3. WHEN the backfill completes, THE Database SHALL contain at least as
   many rows in `contest_problems` as the count of `problems` rows with
   non-null `contest_id` immediately before the migration.

### Requirement 3: Legacy column made nullable

**User Story:** As a Backend developer, I want the `problems.contest_id`
column to allow NULL, so that standalone problems can be persisted while
older code that still reads the column does not crash.

#### Acceptance Criteria

1. WHEN Migration_V5 runs, THE Database SHALL alter `problems.contest_id`
   to drop the `NOT NULL` constraint.
2. WHEN Migration_V5 runs, THE Database SHALL leave the existing foreign
   key from `problems.contest_id` to `contests(id)` in place.
3. WHEN Migration_V5 runs, THE Database SHALL NOT drop the
   `problems.contest_id` column.

### Requirement 4: ContestProblem JPA mapping

**User Story:** As a Backend developer, I want a JPA entity for the
junction so that the application can read and write rows through Spring
Data.

#### Acceptance Criteria

1. THE Backend SHALL define a `ContestProblem` JPA entity mapped to the
   `contest_problems` table with composite key `(contestId, problemId)`.
2. THE Backend SHALL define a `ContestProblemRepository` extending
   `JpaRepository<ContestProblem, ContestProblemId>` and exposing methods
   `findByContestIdOrderByDisplayOrderAscAddedAtAsc(Long)`,
   `findByProblemId(Long)`, `existsByContestIdAndProblemId(Long, Long)`,
   `deleteByContestIdAndProblemId(Long, Long)`, and
   `countByContestId(Long)`.
3. THE Backend SHALL declare an inverse `@ManyToMany(mappedBy = "contests")`
   collection on `Contest` and a `@ManyToMany` collection on `Problem`
   joined through `contest_problems`.

### Requirement 5: Junction-backed reads

**User Story:** As a Frontend user viewing a contest, I want the contest's
problem list to reflect the junction table, so that the M:N relationship is
the source of truth.

#### Acceptance Criteria

1. WHEN the Backend executes `ProblemRepository.findByContestId(contestId)`,
   THE Backend SHALL return only problems whose ids appear in
   `contest_problems` for the given `contestId`, ordered by
   `display_order ASC, added_at ASC`.
2. WHEN the Backend executes
   `ProblemService.getProblemsByContestId(contestId)`, THE Backend SHALL
   delegate to the junction-backed `findByContestId` for cache misses.
3. THE Backend SHALL NOT consult the Legacy_Column when serving requests
   for the problems of a contest.

### Requirement 6: Standalone problem creation

**User Story:** As an Admin, I want to create a problem without picking a
contest, so that I can build a reusable problem pool.

#### Acceptance Criteria

1. WHEN an Admin sends `POST /api/admin/problems` with a valid problem
   payload, THE Backend SHALL persist a new row in `problems` with
   `contest_id` set to NULL.
2. WHEN an Admin sends `POST /api/admin/problems` with a valid problem
   payload, THE Backend SHALL NOT insert any row in `contest_problems` for
   the new problem.
3. WHEN an Admin sends `POST /api/admin/problems` with a payload missing
   the required `title` or `description`, THE Backend SHALL respond with
   HTTP 400 and a descriptive error message.
4. WHEN an Admin sends `POST /api/admin/problems` while not authenticated
   as an Admin, THE Backend SHALL respond with HTTP 401 or 403.
5. WHEN an Admin opens the `AdminProblemManagement` page, THE Frontend
   SHALL display a "Create New Problem" button that navigates to a
   standalone problem creation form.
6. WHEN the standalone problem creation form is submitted successfully,
   THE Frontend SHALL navigate the Admin to `/admin/problems/{id}/edit`
   for the newly created problem.

### Requirement 7: Attach existing problem to contest

**User Story:** As an Admin, I want to attach an existing problem to a
contest, so that the same problem can be reused across multiple events.

#### Acceptance Criteria

1. WHEN an Admin sends `POST /api/admin/contests/{contestId}/problems/{problemId}`
   for a `(contestId, problemId)` pair that does not yet exist in
   `contest_problems`, THE Backend SHALL insert a new row with
   `display_order = 0` and `added_at = NOW()` and respond with HTTP 200
   and the persisted `ContestProblem`.
2. WHEN an Admin sends `POST /api/admin/contests/{contestId}/problems/{problemId}`
   for a pair that already exists in `contest_problems`, THE Backend SHALL
   respond with HTTP 200 and the existing `ContestProblem` without
   inserting a duplicate row.
3. IF the referenced contest does not exist, THEN THE Backend SHALL
   respond with HTTP 404 and a message identifying the contest.
4. IF the referenced problem does not exist, THEN THE Backend SHALL
   respond with HTTP 404 and a message identifying the problem.
5. WHEN an Admin sends a successful attach request, THE Contest_Problem_Service
   SHALL evict the cache key `problems:contest:{contestId}` and the per-problem
   cache for `problemId`.

### Requirement 8: Dual-write to legacy column

**User Story:** As a Backend developer, I want the Legacy_Column kept in
sync during the transition, so that any code path that has not yet been
migrated continues to observe a consistent value.

#### Acceptance Criteria

1. WHEN Contest_Problem_Service.attach succeeds for a problem whose
   Legacy_Column is currently NULL, THE Backend SHALL set
   `problems.contest_id` for that problem to the contest being attached.
2. WHEN Contest_Problem_Service.attach succeeds for a problem whose
   Legacy_Column is currently non-NULL, THE Backend SHALL leave the
   Legacy_Column unchanged.
3. WHEN the existing `POST /api/admin/problems/contest/{contestId}`
   endpoint creates a problem, THE Backend SHALL insert a corresponding
   row into `contest_problems` in the same transaction.

### Requirement 9: Detach problem from contest

**User Story:** As an Admin, I want to remove a problem from a contest
without deleting the problem, so that the problem stays available for
other contests.

#### Acceptance Criteria

1. WHEN an Admin sends `DELETE /api/admin/contests/{contestId}/problems/{problemId}`
   for a pair that exists in `contest_problems`, THE Backend SHALL delete
   that row and respond with HTTP 204.
2. WHEN an Admin sends `DELETE /api/admin/contests/{contestId}/problems/{problemId}`
   for a pair that does not exist in `contest_problems`, THE Backend SHALL
   respond with HTTP 204 without modifying the database.
3. WHEN Contest_Problem_Service.detach removes a row, THE Backend SHALL
   leave the corresponding `problems` row, its `code_snippets`, and its
   test cases intact.
4. WHEN Contest_Problem_Service.detach removes a row AND the Legacy_Column
   for the problem equals the detached `contestId`, THE Backend SHALL
   repoint the Legacy_Column to the smallest remaining `contest_id` for
   that problem in `contest_problems`, or to NULL if no rows remain.
5. WHEN Contest_Problem_Service.detach succeeds, THE Backend SHALL evict
   the cache key `problems:contest:{contestId}` and the per-problem cache
   for `problemId`.

### Requirement 10: Available-problems picker query

**User Story:** As an Admin assembling a contest, I want to see only
problems that are not already in this contest, with optional search and
level filters, so that I can pick the right additions quickly.

#### Acceptance Criteria

1. WHEN an Admin sends `GET /api/admin/contests/{contestId}/available-problems`,
   THE Backend SHALL return only problems whose ids do NOT appear in
   `contest_problems` for the given `contestId`.
2. WHERE the query parameter `search` is provided, THE Backend SHALL
   restrict the result to problems whose `title` contains `search` as a
   case-insensitive substring.
3. WHERE the query parameter `level` is provided and equals `EASY`,
   `MEDIUM`, or `HARD`, THE Backend SHALL restrict the result to problems
   whose `level` equals that value exactly.
4. THE Backend SHALL order the result by `id DESC` (newest problems
   first).

### Requirement 11: Reverse lookup for contests of a problem

**User Story:** As an Admin editing a problem, I want to see every contest
the problem currently belongs to, so that I can manage its associations
from the problem's own page.

#### Acceptance Criteria

1. WHEN an Admin sends `GET /api/admin/problems/{problemId}/contests`,
   THE Backend SHALL return the list of contests whose ids appear in
   `contest_problems` for the given `problemId`.
2. IF the referenced problem does not exist, THEN THE Backend SHALL
   respond with HTTP 404.
3. WHEN an Admin opens `EditProblem` for a problem, THE Frontend SHALL
   display a "Contest Associations" panel populated by the response of
   `GET /api/admin/problems/{problemId}/contests`.

### Requirement 12: Browse-existing modal in ManageContestProblems

**User Story:** As an Admin, I want a searchable picker to attach existing
problems to a contest, so that I can build contests from the standalone
pool without re-entering problem statements.

#### Acceptance Criteria

1. WHEN an Admin opens `ManageContestProblems` for a contest, THE Frontend
   SHALL display a "Browse Existing Problems" button alongside the
   existing "+ Add Problem" button.
2. WHEN an Admin clicks "Browse Existing Problems", THE Frontend SHALL
   open the Browse_Existing_Modal showing a search input, a level filter
   (`ALL`, `EASY`, `MEDIUM`, `HARD`), and the list returned by
   `GET /api/admin/contests/{contestId}/available-problems`.
3. WHEN an Admin types in the modal's search input, THE Frontend SHALL
   re-fetch the available list with the current `search` parameter
   debounced.
4. WHEN an Admin selects one or more problems and confirms, THE Frontend
   SHALL send `POST /api/admin/contests/{contestId}/problems/{problemId}`
   for each selected `problemId`.
5. WHEN all attach requests complete successfully, THE Frontend SHALL
   close the modal and reload the contest's problem roster.

### Requirement 13: Standalone-mode for AddProblem.jsx

**User Story:** As a Frontend developer, I want one Add Problem page that
handles both standalone and contest-bound creation, so that we do not
duplicate the form.

#### Acceptance Criteria

1. WHEN `AddProblem` is rendered at a route without a `contestId` route
   parameter, THE Frontend SHALL render the form in standalone mode and
   submit to `POST /api/admin/problems`.
2. WHEN `AddProblem` is rendered at a route with a `contestId` route
   parameter, THE Frontend SHALL preserve the existing behavior and
   submit to `POST /api/admin/problems/contest/{contestId}`.
3. WHEN `AddProblem` is in standalone mode, THE Frontend SHALL replace the
   "Contest CC-XXXX" label in the page header with a label indicating
   standalone creation.
4. WHEN the App router is registered, THE Frontend SHALL define the route
   `/admin/problems/new` mapped to `AddProblem` wrapped in `AdminRoute`
   inside a `p-8 flex-1` container, consistent with sibling admin pages.

### Requirement 14: Detach UX in ManageContestProblems

**User Story:** As an Admin, I want the "Remove" button on a contest's
roster to detach the problem rather than delete it, so that the problem
remains in the pool.

#### Acceptance Criteria

1. WHEN an Admin clicks the "Remove" button on a roster row in
   `ManageContestProblems` and confirms, THE Frontend SHALL send
   `DELETE /api/admin/contests/{contestId}/problems/{problemId}`.
2. WHEN the detach request returns 204, THE Frontend SHALL remove the
   problem from the displayed roster and show a success toast indicating
   the problem was detached, not deleted.
3. THE Frontend SHALL retain the existing
   `DELETE /api/admin/problems/{id}` action, exposed only on the
   `AdminProblemManagement` page, for fully deleting a problem.

### Requirement 15: Cascade behavior on delete

**User Story:** As a Backend developer, I want deletion of a contest or
problem to clean up junction rows automatically, so that the table never
contains orphans.

#### Acceptance Criteria

1. WHEN an Admin sends `DELETE /api/admin/contests/{id}` and the request
   succeeds, THE Database SHALL remove every row in `contest_problems`
   with `contest_id = id` via the FK cascade.
2. WHEN an Admin sends `DELETE /api/admin/problems/{id}` and the request
   succeeds, THE Database SHALL remove every row in `contest_problems`
   with `problem_id = id` via the FK cascade.
3. WHEN a contest is deleted, THE Database SHALL NOT delete any row in
   `problems`.

### Requirement 16: Authorization for new endpoints

**User Story:** As a Security reviewer, I want every new admin endpoint
gated by `ROLE_ADMIN`, so that the M:N feature does not introduce
privilege escalation paths.

#### Acceptance Criteria

1. WHILE a request to `POST /api/admin/problems`,
   `POST /api/admin/contests/{contestId}/problems/{problemId}`,
   `DELETE /api/admin/contests/{contestId}/problems/{problemId}`,
   `GET /api/admin/contests/{contestId}/available-problems`, or
   `GET /api/admin/problems/{problemId}/contests` lacks
   `ROLE_ADMIN`, THE Backend SHALL respond with HTTP 401 or 403 and SHALL
   NOT execute the underlying operation.

### Requirement 17: Idempotence and atomicity of attach/detach

**User Story:** As a Backend developer, I want attach and detach to behave
predictably under retries, so that flaky network conditions or duplicate
clicks do not corrupt state.

#### Acceptance Criteria

1. WHEN attach is invoked N times for the same `(contestId, problemId)`
   pair with N ≥ 1, THE Database SHALL contain exactly one row in
   `contest_problems` for that pair after the last invocation completes.
2. WHEN detach is invoked N times for the same `(contestId, problemId)`
   pair with N ≥ 1, THE Database SHALL contain zero rows in
   `contest_problems` for that pair after the last invocation completes.
3. WHEN Contest_Problem_Service.attach or detach throws an unchecked
   exception, THE Backend SHALL roll back the transaction so that neither
   `contest_problems` nor the Legacy_Column is left in a partially
   updated state.

### Requirement 18: Caching consistency

**User Story:** As an Admin, I want the contest detail page to reflect
attach and detach operations immediately, so that I can verify my changes
without waiting for cache TTLs.

#### Acceptance Criteria

1. WHEN attach succeeds for `contestId`, THE Backend SHALL invalidate the
   Redis key `problems:contest:{contestId}`.
2. WHEN detach succeeds for `contestId`, THE Backend SHALL invalidate the
   Redis key `problems:contest:{contestId}`.
3. WHEN the existing problem update or delete endpoints succeed, THE
   Backend SHALL continue to invalidate `problems:contest:{contestId}`
   for the contestId that the legacy `problems.contest_id` resolved to,
   preserving current behavior.
