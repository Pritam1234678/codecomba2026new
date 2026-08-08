# CodeCombat 2026 — SQL Judge Engine (Complete Guide)

A production-ready SQL Judge that runs candidate SQL queries against **six external Neon PostgreSQL pools** as **read-only** execution nodes. The app's own database never executes candidate SQL — it only routes, normalizes and compares results.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Files Created (New)](#2-files-created-new)
3. [Files Modified](#3-files-modified)
4. [How a Query Actually Runs (End-to-End Flow)](#4-how-a-query-actually-runs-end-to-end-flow)
5. [What a User Can Do](#5-what-a-user-can-do)
6. [What an Admin Can Do](#6-what-an-admin-can-do)
7. [Provisioning a New Problem (Step by Step)](#7-provisioning-a-new-problem-step-by-step)
8. [Security Model](#8-security-model)
9. [Configuration & Environment Variables](#9-configuration--environment-variables)
10. [Rate Limiting & Queueing](#10-rate-limiting--queueing)
11. [API Reference](#11-api-reference)
12. [Test Coverage](#12-test-coverage)

---

## 1. Architecture Overview

```
                    ┌──────────────────────────────────────────────┐
                    │         CodeCoder Spring Boot App           │
                    │                                              │
  User (JWT) ─────► │  SqlJudgeController ──► SqlSubmissionService  │
                    │         │                                     │
                    │         ▼                                     │
                    │   Valkey Queue  (sqljudge:queue)             │
                    │         │                                     │
                    │   SqlJudgeWorkerPool (8 workers)             │
                    │         │                                     │
                    │         ▼                                     │
                    │   SqlQueryValidator  (string-level checks)    │
                    │         │                                     │
                    │   SqlExecutionRouter ──► NeonNodeRegistry     │
                    │         │              (6 Neon pools)         │
                    └─────────┼─────────────────────────────────────┘
                              ▼
              ┌───────────────────────────────────────────┐
              │  Neon PostgreSQL (x6, read-only roles)     │
              │  each has schema q_<id> + role q_<id>_ro  │
              └───────────────────────────────────────────┘
```

**Key design principles:**

- **Read-only boundary at the database level** — every query runs inside a read-only transaction under a least-privilege role (`q_<id>_ro`) that only has `USAGE` + `SELECT` on its own problem schema. Even if a query is maliciously crafted, PostgreSQL itself refuses writes.
- **Fail-safe = never block a worker** — a strict (string/pattern-based) validator runs *before* the DB round-trip as a fast pre-filter. It is secondary; the DB read-only role is the primary boundary.
- **Single consistent source of truth** — the official solution is executed ONCE per problem, on the first healthy node, and the expected result is cached in Redis (`sql:problem:{id}:expected`, TTL 24h). Every later submission is compared against this cached expected result.

---

## 2. Files Created (New)

### 2.1 Backend — SQL Judge module

All under `src/main/java/com/example/codecombat2026/sqljudge/`:

| File | Purpose |
|---|---|
| `entity/SqlProblem.java` | JPA entity for a SQL problem (title, description, setupSql, officialSolutionSql, comparisonMode, timeLimitMs, maxResultRows, schemaName, provisioned count, enabled). |
| `entity/SqlSubmission.java` | JPA entity for a user submission (problem, user, sql, status, verdict, executionTimeMs, node used, sanitized preview, error). |
| `repository/SqlProblemRepository.java` | Spring Data JPA repo for problems (with paging for admin). |
| `repository/SqlSubmissionRepository.java` | Spring Data JPA repo for submissions, including the `@Modifying` finalize/update queries (each annotated `@Transactional`). |
| `config/SqlJudgeProperties.java` | `@ConfigurationProperties(prefix = "sql.judge")` binding for all tunables (enabled, workers, timeouts, max rows, the six Neon connection URLs/users/passwords, breaker thresholds). |
| `dto/SqlJob.java` | Internal queue job DTO (problem id, SQL, userId, run-vs-submit flag, submission id). |
| `dto/SqlExecutionResult.java` | Raw query result (column list + row matrix + duration + optional error). |
| `dto/SqlResult.java` | Normalized result ready for comparison. |
| `dto/SqlProblemRequest.java` | Admin request body for creating a problem. |
| `dto/SqlProblemView.java` | Safe view of a problem for admin; user view never exposes officialSolutionSql. |
| `dto/SqlSubmissionRequest.java` | User request body for RUN / SUBMIT. |
| `dto/SqlSubmissionStatusResponse.java` | Status/verdict/preview returned to the client when polling. |
| `dto/SqlVerdictEvent.java` | SSE event payload pushed to the user on completion. |
| `router/NeonNode.java` | One Neon node: url, user, password, healthy flag, activeQueries, lastLatency, consecutiveFailures, breaker state, semaphore. |
| `router/NeonNodeRegistry.java` | Builds the 6 nodes from config, runs the `SELECT 1` sweep every 30s, auto-recovers nodes. |
| `router/SqlExecutionRouter.java` | Chooses the healthiest node (lowest activeQueries then latency, skip unhealthy/saturated), retries one alternate node on failure. |
| `validator/SqlQueryValidator.java` | Static validation: only `SELECT`/`WITH` allowed, blocks INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/GRANT/CREATE (incl. via `WITH ... INSERT`), blocks forbidden keywords, limits SQL length, blocks cross-schema references (`.`, `"`, `\`). |
| `executor/SqlQueryExecutor.java` | Runs a query on a single Neon node with the read-only session sequence (see §8). |
| `normalizer/SqlResultNormalizer.java` | Normalizes cell values so equivalent results compare equal: `10.00` == `10`, ISO dates, byte[] → hex, NULL sentinel. |
| `comparator/SqlResultComparator.java` | Compares candidate vs expected: UNORDERED (sorted flattened rows) or ORDERED (positional); column count must match, column names ignored. |
| `service/SqlSubmissionService.java` | Accepts RUN/SUBMIT requests, applies rate limits, enqueues jobs, returns submission id for polling. |
| `service/SqlExpectedResultCache.java` | Redis cache for the official expected result (`sql:problem:{id}:expected`, TTL 24h). |
| `service/SqlJudgeHealthService.java` | Aggregates cluster status for the admin panel (node health, queue depth, active jobs, worker count). |
| `service/SqlProblemProvisioningService.java` | Provisions a problem on ALL 6 Neon nodes: create schema `q_<id>`, run setupSql, create read-only role, grant SELECT, verify official solution, compute expected result once. |
| `worker/SqlJudgeWorkerPool.java` | 8 background workers that drain `sqljudge:queue`, execute via router/executor, persist result + preview, then finalize status and emit SSE verdict. |
| `controller/SqlJudgeController.java` | **User-facing** REST + SSE endpoints (see §5, §11). |
| `controller/AdminSqlJudgeController.java` | **Admin-only** REST endpoints (see §6, §11). |

### 2.2 Database migrations

| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V23__sql_judge.sql` | Creates `sql_problems` and `sql_submissions` tables. |
| `src/main/resources/db/migration/V24__sql_judge_schema_name_nullable.sql` | `ALTER TABLE sql_problems ALTER COLUMN schema_name DROP NOT NULL` — schema is assigned at provision time as `q_<id>`, not at creation. |

### 2.3 Tests

All under `src/test/java/com/example/codecombat2026/sqljudge/` (49 tests total):

| Test file | Tests | What it covers |
|---|---|---|
| `validator/SqlQueryValidatorTest.java` | 20 | Allowed SELECT, blocked DML/DDL, `WITH ... INSERT`, cross-schema, length caps. |
| `normalizer/SqlResultNormalizerTest.java` | 7 | Number/date/byte[]/NULL normalization. |
| `comparator/SqlResultComparatorTest.java` | 7 | ORDERED vs UNORDERED, column count mismatch. |
| `router/SqlExecutionRouterTest.java` | 6 | Node selection, unhealthy/saturation skip, retry logic. |
| `service/SqlSubmissionServiceTest.java` | 4 | Rate limits, queue enqueue, job payload. |
| `executor/SqlQueryExecutorTest.java` | 5 | Read-only session sequence, statement timeout, maxRows cap. |

### 2.4 Frontend

| File | Purpose |
|---|---|
| `frontend/src/pages/SqlJudge.jsx` | **User page** — problem list (`/sql-judge`) + solve view (`/sql-judge/:id`) with SQL editor, RUN, SUBMIT, live result preview, SSE live verdict, submission history. |
| `frontend/src/pages/AdminSqlJudge.jsx` | **Admin page** (`/admin/sql-judge`) — problem CRUD table, create form (title/description/setupSql/official solution/comparison/timeout/maxRows), provision + enable/disable buttons, per-problem node status, 6-node cluster health strip (healthy / activeQueries / latency / failures / queueDepth / activeJobs / workers). |

---

## 3. Files Modified

| File | What was changed |
|---|---|
| `frontend/src/App.jsx` | Added lazy-loaded routes: `/sql-judge` and `/sql-judge/:id` (under `UserRoute`), `/admin/sql-judge` (under `AdminRoute`). |
| `frontend/src/components/AppSidebar.jsx` | Added two separate sidebar entries: **Admin** `SQL Judge` → `/admin/sql-judge`, **User** `SQL Judge` → `/sql-judge`. |
| `frontend/src/services/api.js` | Added `sqlJudgeApi` export — all user (`listProblems`, `getProblem`, `run`, `submit`, `submissionStatus`, `mySubmissions`, `issueSseTicket`) and admin (`adminListProblems`, `adminGetProblem`, `adminCreateProblem`, `adminProvision`, `adminSetEnabled`, `adminStatus`) calls. |
| `src/main/java/com/example/codecombat2026/security/SecurityConfig.java` | Added `"/api/sql/stream"` to `permitAll()` — same single-use-ticket pattern as `/api/submissions/stream`. The ticket-mint endpoint stays JWT-gated; the ticket consume inside `streamVerdicts` is the real auth gate (survives async dispatch). |
| `src/main/java/com/example/codecombat2026/service/RateLimiterService.java` | Added a generic `allow(String key, int max, int windowSeconds)` method so the SQL judge can apply different ceilings for RUN vs SUBMIT (also used by any future bounded endpoint). Primary path: atomic Valkey INCR + TTL; fallback: per-JVM window. |
| `src/main/resources/application.properties` | Added the full `sql.judge.*` configuration block (see §9). |
| `sqljudge/worker/SqlJudgeWorkerPool.java` | *(fix 8589846)* The RUN preview is now persisted **before** the job is finalized, so the SSE verdict carries the preview. |
| `sqljudge/entity/SqlProblem.java` | *(fix 06d7da0)* `schema_name` column made nullable (was NOT NULL but only assigned at provision time). |
| `sqljudge/repository/SqlSubmissionRepository.java` | *(fix 4622369)* Added `@Transactional` to all `@Modifying` queries (`updateStatus`, `updateFinalized`, `updateFinalizedPreview`) — they ran on worker threads and failed with "Executing an update/delete query". |
| `frontend/src/pages/SqlJudge.jsx` | *(fix 08ef7d7)* Imported `Link` (problem list crashed on render — "Link is not defined"). *(fix 2055352)* Added a `loadingProblem` guard so `/sql-judge/:id` shows a spinner instead of crashing blank on `null.title`. |

---

## 4. How a Query Actually Runs (End-to-End Flow)

### 4.1 RUN (preview, no comparison)

1. **User** clicks **Run** in `/sql-judge/:id` → `POST /api/sql/problems/{id}/run` with `{ sql }`.
2. `SqlJudgeController` loads the problem, checks it exists and is `enabled`.
3. `RateLimiterService.allow("sqljudge:rate:run:" + userId, MAX_TEST_RUNS, window)` — rejects 429 if exceeded.
4. `SqlSubmissionService` creates a `SqlSubmission` (status `QUEUED`, kind `RUN`) and pushes a `SqlJob` to Valkey key `sqljudge:queue`.
5. A free worker in `SqlJudgeWorkerPool` (8 threads, semaphore) takes the job (claims via `sqljudge:processing:{id}` / `sqljudge:claim:{id}` with 10-min TTL).
6. `SqlQueryValidator.validate()` runs first — rejects with `SECURITY_VIOLATION` if the SQL isn't read-only SELECT.
7. `SqlExecutionRouter` picks the healthiest Neon node.
8. `SqlQueryExecutor` runs the query on that node inside the read-only session (see §8).
9. The worker sanitizes the result into a **preview** (max `sql.judge.preview-max-rows`), persists it to the submission, **then finalizes** the status to `SUCCEEDED` / `ERROR`.
10. An SSE event is pushed to the user's open stream (via the single-use ticket): verdict `RUN_SUCCESS` + preview rows.
11. The user sees the live result table in the UI without polling.

### 4.2 SUBMIT (execution + comparison → verdict)

Same as RUN through step 8, then:

9. The worker fetches the **cached expected result** for the problem from Redis (`sql:problem:{id}:expected`). If the cache is cold, it re-runs the official solution on the current node (via `SqlExpectedResultCache`).
10. `SqlResultNormalizer` normalizes both the candidate rows and the expected rows (numbers `10.00`→`10`, ISO dates, byte[]→hex, NULL sentinel).
11. `SqlResultComparator` compares:
    - **UNORDERED** mode: flatten each row to a string with a separator, sort both sets, compare line-by-line.
    - **ORDERED** mode: compare row-by-row positionally.
    - Column count must match; column names are ignored.
12. Verdicts: `ACCEPTED` (match), `WRONG_ANSWER` (mismatch), `ERROR` (DB/execution error), `SECURITY_VIOLATION` (rejected earlier), `TIME_LIMIT_EXCEEDED` (statement_timeout / queryTimeout), `RATE_LIMITED`.
13. The worker persists the final verdict + preview, then finalizes and emits the SSE verdict event. The submission history endpoint can also be polled.

### 4.3 Queue janitor

A periodic sweep requeues any job stuck in `processing` for more than 5 minutes (crashed worker recovery).

---

## 5. What a User Can Do

- **Browse problems** — `GET /api/sql/problems` lists only enabled questions (internal fields like `officialSolutionSql` are never exposed).
- **Open a problem** — `GET /api/sql/problems/{id}` gives title + description (safe view).
- **Run a query** — writes SQL in the editor and hits **Run** to get a live result preview (no comparison, fast feedback, max `preview-max-rows` rows).
- **Submit a solution** — hits **Submit** to get a real verdict (`ACCEPTED` / `WRONG_ANSWER` / `TIME_LIMIT_EXCEEDED` / `ERROR` / `SECURITY_VIOLATION`) with execution time and the node used.
- **See live verdicts** — opens an SSE stream (`POST /api/sql/sse-ticket` for a single-use ticket, then `GET /api/sql/stream?ticket=...`) and gets verdicts pushed in real time, no polling needed.
- **View submission history** — `GET /api/sql/submissions?limit=N` returns recent submissions with status/verdict.
- **Rate limited by role** — RUN and SUBMIT have separate ceilings; violations return `RATE_LIMITED` / HTTP 429.

---

## 6. What an Admin Can Do

All admin endpoints are `@PreAuthorize("hasRole('ROLE_ADMIN')")`:

- **List problems** — `GET /api/admin/sql/problems?page=0&size=100` (paged, includes provision status `x/6` nodes).
- **Create a problem** — `POST /api/admin/sql/problems` with `{ title, description, setupSql, officialSolutionSql, comparisonMode, timeLimitMs, maxResultRows }`. Returns `201` with `provisioned: true` once all 6 Neon nodes are ready.
- **Provision / re-provision** — `POST /api/admin/sql/problems/{id}/provision` (idempotent: DROP SCHEMA CASCADE → recreate → apply setup → role + grants → verify). Retries any node that failed during creation.
- **Enable / disable** — `PATCH /api/admin/sql/problems/{id}/enabled` with `{ enabled: true|false }` (disabled problems vanish from the user list).
- **Cluster status** — `GET /api/admin/sql/status` → all 6 nodes (healthy, activeQueries, lastLatency, consecutiveFailures), queue depth, active jobs, worker count, and whether `sql.judge.enabled` is on.
- **UI** — the `/admin/sql-judge` page in the app gives the same CRUD + a live health strip, so an admin can add a question without touching the API.

---

## 7. Provisioning a New Problem (Step by Step)

When an admin creates a problem, `SqlProblemProvisioningService` does this on **every one of the 6 Neon nodes** (per-node, idempotent):

1. `DROP SCHEMA IF EXISTS q_<id> CASCADE` — wipe any previous copy.
2. `CREATE SCHEMA q_<id>`.
3. Run the problem's `setupSql` with `SET search_path TO q_<id>` (creates tables + seed rows).
4. `CREATE ROLE q_<id>_ro NOLOGIN` (guarded with a `DO $$ ... $$` block so it's safe to re-run).
5. `GRANT USAGE ON SCHEMA q_<id> TO q_<id>_ro` and `GRANT SELECT ON ALL TABLES IN SCHEMA q_<id> TO q_<id>_ro`.
6. Add `q_<id>_ro` to the pool user (`neondb_owner`) so the app's connection can `SET ROLE q_<id>_ro`.
7. Verify the official solution runs on that node.
8. On the **first verified node**, run the official solution once, compute the expected result (capped at `max-result-rows`), and cache it in Redis for 24h.
9. Only when **all 6 nodes verify** does the problem become `enabled` (`enabled` is set false until provision completes).

Any node that fails is skipped and marked; `POST .../provision` retries it later. The user-facing `GET /api/sql/problems` only returns `enabled` problems, so a partially-provisioned question never leaks to users.

**Live example (problem #2 "Total Salary by Department"):**
`CREATE TABLE employees(id, name, department, salary)` + 6 seeded rows (HR 2, IT 2, Finance 2). Official solution: `SELECT department, SUM(salary) AS total_salary FROM employees GROUP BY department`. Verified on all 6 nodes: schema `q_2`, role `q_2_ro`, 6 rows each.

---

## 8. Security Model

Candidate SQL is **never** executed with elevated rights. The defense is layered:

1. **DB-level read-only boundary (primary, non-bypassable):** every query runs via `SqlQueryExecutor` with this session sequence on the target Neon node:
   ```
   SET ROLE NONE
   SET default_transaction_read_only = on
   SET ROLE q_<id>_ro            -- least-privilege, only USAGE+SELECT on q_<id>
   SET search_path TO q_<id>
   SET statement_timeout = {timeLimitMs}
   ```
   plus JDBC `setQueryTimeout(...)` and `setMaxRows(maxResultRows + 1)`. Even a crafted query that slips past the validator is refused writes by PostgreSQL itself.
2. **App-level validator (fast pre-filter):** `SqlQueryValidator` rejects anything that isn't `SELECT`/`WITH`, blocks DML/DDL keywords (`INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `GRANT`, `CREATE`, ...), blocks `WITH ... INSERT`, blocks cross-schema references (`.`/`"`/`` ` ``), and caps SQL length.
3. **Schema isolation:** each problem lives in its own `q_<id>` schema with its own role — one problem can never read another problem's data.
4. **External nodes:** execution happens on Neon, not the app's own `codecombat` database. A malicious query cannot touch the platform's data.
5. **Rate limits** on RUN/SUBMIT prevent abuse/DoS of the worker pool.
6. **No secrets exposed:** user-facing problem views never include the official solution.

Live-verified: `INSERT` and `WITH x AS (...) INSERT` both returned `SECURITY_VIOLATION`, and the Neon data was untouched (6 rows intact, `id=99` never inserted).

---

## 9. Configuration & Environment Variables

`.env` (app) / `application.properties` block:

```properties
sql.judge.enabled=${SQL_JUDGE_ENABLED:false}              # master switch (true on VM)
sql.judge.default-timeout-ms=${SQL_JUDGE_DEFAULT_TIMEOUT_MS:2000}
sql.judge.max-result-rows=${SQL_JUDGE_MAX_RESULT_ROWS:500}
sql.judge.preview-max-rows=${SQL_JUDGE_PREVIEW_MAX_ROWS:100}
sql.judge.workers=${SQL_JUDGE_WORKERS:8}
sql.judge.max-inflight-queries=${SQL_JUDGE_MAX_INFLIGHT_QUERIES:120}
sql.judge.max-queue-wait-seconds=${SQL_JUDGE_MAX_QUEUE_WAIT_SECONDS:30}
sql.judge.failure-threshold=${SQL_JUDGE_FAILURE_THRESHOLD:3}   # circuit-breaker
```

Neon node credentials — six endpoints, one URL/USER/PASS each (loaded from env, never committed):

```
NEON_DB_1_URL=ep-...-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb
NEON_DB_1_USER=neondb_owner
NEON_DB_1_PASS=...
... (NEON_DB_2..NEON_DB_6)
```

Note: the local `.env` sources these from the `Test1..Test6` variables.

---

## 10. Rate Limiting & Queueing

**Rate limits** (via `RateLimiterService.allow(key, max, window)`):
- `sqljudge:rate:run:{userId}` — RUN previews per window.
- `sqljudge:rate:submit:{userId}` — SUBMITS per window (lower ceiling).
- Backed by atomic Valkey INCR + TTL; falls back to a per-JVM window if Redis is down.

**Queue (Valkey / Redis):**
- `sqljudge:queue` — pending jobs (LPUSH/BRPOP).
- `sqljudge:processing:{id}` + `sqljudge:claim:{id}` — claimed job + 10-min TTL claim.
- Janitor requeues jobs stuck > 5 min.
- `sqljudge:rate:*` — rate-limit counters.

**Workers:** fixed pool of 8 (`sql.judge.workers`), a semaphore per node caps concurrent queries on that node (`max-inflight-queries`), and the router prefers the node with the fewest active queries.

---

## 11. API Reference

### User endpoints (`/api/sql`, authenticated, user role)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/sql/problems` | List enabled problems (safe view) |
| GET | `/api/sql/problems/{id}` | Problem detail (no official solution) |
| POST | `/api/sql/problems/{id}/run` | Execute + preview, no comparison |
| POST | `/api/sql/problems/{id}/submit` | Execute + compare → verdict |
| GET | `/api/sql/submissions/{id}` | Poll a submission status/verdict |
| GET | `/api/sql/submissions?limit=N` | Recent submissions (history) |
| POST | `/api/sql/sse-ticket` | Mint a single-use SSE ticket |
| GET | `/api/sql/stream?ticket=...` | SSE stream for live verdicts (permitAll at filter; ticket consume is the auth gate) |

### Admin endpoints (`/api/admin/sql`, ROLE_ADMIN only)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/sql/problems?page&size` | Paged problem list w/ provision status |
| GET | `/api/admin/sql/problems/{id}` | Full problem incl. official solution |
| POST | `/api/admin/sql/problems` | Create + auto-provision on all 6 nodes |
| POST | `/api/admin/sql/problems/{id}/provision` | (Re)provision a problem |
| PATCH | `/api/admin/sql/problems/{id}/enabled` | `{ enabled: bool }` toggle |
| GET | `/api/admin/sql/status` | Cluster health + queue metrics |

---

## 12. Test Coverage

Run with:

```bash
./mvnw -q test -Dtest='sqljudge.*'  # or the full suite
```

| Test class | Count |
|---|---|
| SqlQueryValidatorTest | 20 |
| SqlResultNormalizerTest | 7 |
| SqlResultComparatorTest | 7 |
| SqlExecutionRouterTest | 6 |
| SqlSubmissionServiceTest | 4 |
| SqlQueryExecutorTest | 5 |
| **Total** | **49** |

Known environment limitation: `@SpringBootTest`-based tests (e.g. the pre-existing `SseAuthInvariantTest`) fail to start the context in this environment — unrelated to the SQL judge work (fails on a clean tree too).
