# CodeCoder Architecture - Comprehensive Analysis

## Summary
This document captures a complete analysis of the existing CodeCoder architecture to inform the Private Contest Hosting feature design.

---

## 1. Entity Layer (JPA/Hibernate)

### Core Entities
- **User**: `id`, `username`, `email`, `password`, `enabled`, `totalPoints`, roles (M:N with Role), social fields
- **Role**: `id`, `name` (enum: ROLE_USER, ROLE_ADMIN)
- **Contest**: `id`, `name`, `description`, `startTime`, `endTime`, `status` (enum: UPCOMING, LIVE, ENDED), `active`, M:N with Problem
- **Problem**: `id`, `title`, `description`, `inputFormat`, `outputFormat`, `constraints`, `timeLimit`, `memoryLimit`, `level`, `active`, legacy `contest_id` (being migrated), M:N with Contest via ContestProblem
- **ContestProblem**: Explicit M:N junction with `contestId`, `problemId`, `displayOrder`, `addedAt`
- **Submission**: `id`, user, problem, contest, `code`, `language`, `status` (PENDING, JUDGING, AC, WA, TLE, RE, CE, MLE), `testRun` flag, execution stats
- **ContestRegistration**: M:N junction for user-contest participation with unique constraint on (contest_id, user_id)
- **CodeSnippet**: Language-specific code templates (starter code + solution template harness) per problem
- **UserProblemSolved**: Practice mode progress tracking
- **DuelMatch**, **DuelSubmission**: 1v1 coding battle entities
- **WebContestTemplate**: "Into the Web" full-stack challenges

### Proctoring Entities
- **ProctoredContest**: 1:1 extension marker on Contest, includes `consentVersion`
- **ProctoringSession**: Per-candidate session with `startedAt`, `endedAt`, `endReason`, risk scores
- **ProctoringEvent**: WebSocket event stream (tab switch, face detection, fullscreen exit)
- **ProctoringScreenshot**: Random screenshots with JPEG blobs
- **ProctoringConsentAck**: Consent acknowledgment records
- **ProctoringAdminAudit**: Admin actions on proctoring data

### Entity Patterns
- **@PrePersist/@PreUpdate**: Automatic timestamp/status calculation (Contest.calculateStatus(), Submission.onCreate())
- **Lazy Fetch**: All relations use LAZY to avoid N+1
- **@JsonIgnore**: Bidirectional relations ignored in JSON to prevent cycles
- **Composite Keys**: ContestProblemId for M:N junction
- **Indexes**: Strategic indexes on foreign keys and query columns

---

## 2. Repository Layer (Spring Data JPA)

### Query Patterns
- **Derived queries**: `findByUsername`, `findByActiveTrue`, `existsByContestIdAndUserId`
- **@Query JPQL**: Custom joins and projections
- **@Query native SQL**: Complex queries with PostgreSQL-specific features
- **@Modifying**: Bulk updates from judge workers (`updateStatus`, `updateResult`)
- **Pageable**: Limit result sets for performance

### Key Repositories
- **ContestRepository**: Status-based queries, active contest filtering
- **ProblemRepository**: M:N junction reads via ContestProblem, available problems query
- **SubmissionRepository**: User/contest/problem queries, time-windowed queries for proctoring
- **UserRepository**: Username/email lookups, non-admin user search
- **ContestRegistrationRepository**: Registration checks and counts
- **ContestProblemRepository**: Junction CRUD with display order

### Repository Pattern
- All extend `JpaRepository<Entity, Long>`
- @Repository annotation (though Spring Data makes it optional)
- Method naming conventions for auto-implementation
- Custom queries when derived queries insufficient

---

## 3. Service Layer

### Service Responsibilities
- **Business logic enforcement** (validation, authorization)
- **Cache management** (read-through, write-through, eviction)
- **Transaction boundaries** (@Transactional)
- **External system integration** (email, judge queue)
- **Cross-cutting concerns** (rate limiting, audit logging)

### Key Services

#### ContestService
- CRUD operations with cache eviction
- Cache: `contests:active` (30s TTL), `contest:{id}` (2min TTL)
- Uses ObjectMapper for JSON serialization

#### ProblemService
- Problem CRUD with per-problem and per-contest caching
- Cache: `problem:{id}` (5min), `problems:contest:{contestId}` (30s)
- Coordinates with ContestProblemService for M:N operations

#### ContestProblemService
- **Owns M:N relationship management**
- Dual-write to legacy `problems.contest_id` during migration
- Idempotent attach/detach with cache eviction
- Available problems query with caching (no-search queries only)
- Cache: `problem:contests:{problemId}`, `contest:available:{contestId}:{level}`

#### SubmissionService
- **Async submission pattern**: Save PENDING → push to Valkey queue → return immediately
- Test run vs real submission distinction (`testRun` flag)
- Integration with proctoring lockout mechanism
- Upsert logic: Reuse finished submission rows, create new for in-flight

#### SubmissionWorkerPool
- **Background worker pool** draining `submission:queue`
- **LMOVE atomicity**: Claim job from queue → processing list → finalize → LREM
- **Janitor**: Scheduled reclamation of stuck jobs (5min timeout)
- **Leaderboard delta calculation**: Track per-problem scores to compute deltas
- **SSE verdict push**: SseEmitterRegistry integration
- **Duel branch**: Conditional leaderboard update (no leaderboard for duel submissions)

#### LeaderboardService & LeaderboardCacheService
- **Batch DB calculation**: Sum best-per-problem scores
- **Real-time ZSET**: Valkey sorted set with ZINCRBY for live updates
- **O(log N) operations**: Fast rank/score lookups
- Cache: `leaderboard:contest:{contestId}` (26h TTL)

#### CacheService
- **Cache-aside pattern**: Try cache → on miss read DB → populate cache
- Problem, snippet, contest caching
- Generic get/set/delete for custom keys

#### EmailService
- **Dual-relay routing**: noreply@codecoder.in (SendPulse), support@codecoder.in (Brevo)
- **HTML templates**: classpath:email-templates/ with {{TOKEN}} replacement
- **Retry with backoff**: 3 attempts, exponential backoff + jitter
- **Async sending**: @Async to avoid blocking API responses

#### RateLimiterService
- **Sliding window** with Valkey INCR + TTL
- **Local fallback**: ConcurrentHashMap per-JVM when Valkey unavailable
- Limits: 5 submissions/30s, 15 test runs/30s

#### ContestRegistrationService
- Idempotent registration
- Cache-first registration checks
- Cache: `contest:reg:{contestId}:{userId}` (10min TTL)

#### ProctoringSessionService
- Session lifecycle management (start, heartbeat, end)
- Lockout mechanism for terminal states
- Integration with submission flow

### Service Patterns
- **@Autowired field injection** (warnings suggest constructor injection)
- **@Lazy**: Break circular dependencies (e.g., DuelService ↔ SubmissionWorkerPool)
- **Try-catch around cache**: Never fail on cache errors
- **Eviction on write**: Invalidate caches after mutations
- **TTL strategy**: Shorter for hot data (30s contests), longer for cold (5min problems)

---

## 4. Controller Layer

### REST Conventions
- **Public routes**: `/api/auth`, `/api/contests` (GET list), `/api/health`
- **Authenticated routes**: `/api/contests/{id}`, `/api/submissions`
- **Admin routes**: `/api/admin/**` with `@PreAuthorize("hasRole('ADMIN')")`

### Security Annotations
- `@PreAuthorize("isAuthenticated()")`: Require any authenticated user
- `@PreAuthorize("hasRole('ADMIN')")`: Admin-only
- `@AuthenticationPrincipal UserDetailsImpl`: Get current user from JWT

### Response Patterns
- **DTO conversion**: Entity → DTO to control JSON exposure
- **Cache-Control headers**: `public, max-age=30` for cacheable endpoints
- **Parallel fetches**: CompletableFuture for independent operations
- **Batch queries**: Fetch related data in single query to avoid N+1

### Key Controllers
- **ContestController**: Public contest list, authenticated detail/register
- **AdminContestController**: CRUD with cache eviction, M:N problem management
- **SubmissionController**: Submit/test code, SSE verdict stream
- **LeaderboardController**: Real-time leaderboard from ZSET
- **ProctoringEntryController**: Pre-flight checks, consent, session start
- **ProctoringAdminController**: Admin dashboard, screenshot viewing, audit log

### Special Endpoints
- **SSE (Server-Sent Events)**: `/api/submissions/stream` for real-time verdicts
- **WebSocket**: `/api/proctoring/ws` for bi-directional proctoring events
- **Ticket-based auth**: SseTicketService for one-time use tokens (bypasses JWT for WebSocket/SSE)

---

## 5. Security Architecture

### Authentication
- **JWT-based**: Bearer tokens in Authorization header
- **JwtUtils**: Generate, validate, extract user details
- **UserDetailsServiceImpl**: Load user by username for Spring Security
- **AuthTokenFilter**: Intercept requests, validate JWT, set SecurityContext

### Authorization
- **Role-based**: ROLE_USER, ROLE_ADMIN
- **Method security**: @PreAuthorize at controller methods
- **Ownership validation**: Services verify user owns resource (e.g., contest host checks)

### SecurityConfig
- **FilterChain**: JWT filter before UsernamePasswordAuthenticationFilter
- **CORS**: Explicit origin whitelist (no wildcards in prod)
- **CSP**: Content Security Policy headers
- **HSTS**: HTTP Strict Transport Security (manual writer for nginx TLS termination)
- **SSE/WebSocket permit**: Filter-level permitAll, auth in controllers
- **Public endpoints**: `/api/health`, `/api/auth/**`, GET `/api/contests`

### Security Patterns
- **Password encryption**: BCryptPasswordEncoder
- **Token blacklist**: JwtBlacklistService for logout
- **Rate limiting**: RateLimiterService for submissions
- **Input validation**: @Valid, @NotBlank, @Email on DTOs
- **SQL injection prevention**: Parameterized queries, JPA protections

---

## 6. Caching Strategy (Valkey/Redis)

### Cache Keys
```
# Contests
contests:active                     → List<Contest>  (30s TTL)
contest:{id}                        → Contest       (2min TTL)

# Problems
problem:{id}                        → Problem       (5min TTL)
problems:contest:{contestId}        → List<Problem> (30s TTL)
snippet:{problemId}:{language}      → String        (60min TTL)
contest:available:{contestId}:{level} → List<Problem> (30s TTL)
problem:contests:{problemId}        → List<Contest> (5min TTL)

# Submissions
submissions:user:{userId}           → List<Submission>
submission:user:problem:{userId}:{problemId}
submission:status:{submissionId}

# Leaderboard
leaderboard:contest:{contestId}     → ZSET (26h TTL)
contest:score:{contestId}:{userId}:{problemId} → String (26h TTL)

# Registration
contest:reg:{contestId}:{userId}    → "1"|"0" (10min TTL)

# Rate Limiting
ratelimit:submit:{userId}           → count (30s TTL)
ratelimit:testrun:{userId}          → count (30s TTL)

# Judge Queue
submission:queue                    → LIST (job queue)
private:submission:queue            → LIST (private contest queue)
submission:processing:{instanceId}:{workerId} → LIST (in-flight jobs)
submission:processing:registry      → SET (tracking processing lists)

# Proctoring
proctoring:ws:ticket:{token}        → sessionData (5min TTL)
proctoring:session:{sessionId}:*    → Various session state
```

### Cache Patterns
- **Read-through**: Try cache → on miss read DB → populate cache
- **Write-through**: Update DB → evict cache (not write cache)
- **TTL strategy**: Hot data short TTL, cold data long TTL
- **Eviction on mutation**: Delete affected keys after writes
- **Fail-safe**: Never throw on cache errors, log and continue

### Data Structures
- **String**: Serialized JSON entities, simple flags
- **ZSET (Sorted Set)**: Leaderboards with scores
- **LIST**: FIFO queues for judge jobs
- **SET**: Registry of worker processing lists

---

## 7. Queue Architecture (Valkey)

### Submission Queue Flow
```
1. Producer (SubmissionService)
   ↓ LPUSH
2. submission:queue (or private:submission:queue)
   ↓ LMOVE (atomic claim)
3. submission:processing:{instanceId}:{workerId}
   ↓ Execute in sandbox (bwrap + prlimit)
4. Write verdict to DB
   ↓ Update leaderboard ZSET
5. Push SSE to user
   ↓ LREM from processing list (ACK)
6. Done
```

### Durability Model
- **Atomic claim**: LMOVE from queue → processing list
- **Worker crash recovery**: Janitor reclaims jobs from processing lists after timeout
- **Processing lists per worker**: `{instanceId}:{workerId}` for parallel workers
- **Registry**: SET of all processing list keys for janitor scanning

### Worker Pool Configuration
- **VM1**: 2 contest workers + 2 practice workers
- **VM2**: 6 contest workers + 4 practice workers
- **VM3/4/5**: Web contest workers for "Into the Web" challenges
- **Environment variable**: `JUDGE_WORKERS` controls pool size

---

## 8. Judge/Sandbox Execution

### Execution Flow
1. **Fetch harness**: CodeSnippet.solutionTemplate from cache
2. **Inject user code**: Replace USER_CODE_PLACEHOLDER markers
3. **Sandbox run**: bwrap + prlimit for isolation
4. **Parse output**: TC:number:PASS|FAIL lines
5. **Compute verdict**: AC/WA/TLE/MLE/RE/CE
6. **Update DB**: Submission status, score, test case details
7. **Update leaderboard**: Delta calculation for contest submissions
8. **Push SSE**: Real-time verdict to user's browser

### Sandbox (SandboxRunner)
- **bubblewrap (bwrap)**: Containerization without Docker
- **prlimit**: CPU time and memory limits
- **Environment isolation**: No network, restricted filesystem

### Test Case Format
```
TC:1:PASS:input=abc:expected=3:got=3
TC:2:FAIL:input=xyz:expected=6:got=5:hidden
```

---

## 9. Proctoring Architecture

### Components
- **ProctoredContest**: Marker entity enabling proctoring for a contest
- **ProctoringSession**: Per-candidate session tracking
- **WebSocket**: Bi-directional real-time event stream
- **Risk Scoring**: Aggregate events into risk bands (LOW, MEDIUM, HIGH, CRITICAL)
- **Admin SSE**: Live dashboard updates for admins

### Event Types
- Face detection (AI inference every 1s)
- Tab switch
- Fullscreen exit
- Screenshot capture (random intervals)
- Heartbeat (every 15s)

### Submission Integration
- **Lockout**: Terminal proctoring states (SELF_QUIT, ADMIN_FORCED, HEARTBEAT_TIMEOUT) block submissions
- **Session tagging**: SubmissionJob carries `proctoringSessionId` for correlation
- **Contest time gate**: No submissions after `endTime` regardless of proctoring

### Admin Features
- Live risk dashboard with SSE updates
- Screenshot viewing (served only to authenticated admins)
- Audit log of admin actions
- Session replay and timeline

---

## 10. Configuration

### Application Properties
- **Database**: HikariCP pool (max 20), PostgreSQL 18
- **Flyway**: Schema migrations, baseline-on-migrate for existing DB
- **Security**: JWT secret/expiration from env vars
- **Email**: Dual SMTP relays (SendPulse, Brevo)
- **Valkey**: Connection pool, SSL disabled
- **Proctoring**: 13 tunable parameters (thresholds, intervals, limits)

### Environment Variables
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `JUDGE_WORKERS`, `PRACTICE_WORKERS`, `WEB_CONTEST_WORKERS`
- `SENDPULSE_USERNAME`, `SENDPULSE_PASSWORD`
- `BREVO_USERNAME`, `BREVO_PASSWORD`
- `APP_ALLOWED_ORIGINS` (CORS whitelist)
- `PROCTORING_*` (13 proctoring parameters)

---

## 11. Deployment Architecture

### VM Distribution
- **VM1 (6GB)**: Main API server, PostgreSQL, Valkey, nginx, 2 contest workers + 2 practice workers
- **VM2 (10GB)**: Judge/practice execution, 6 contest workers + 4 practice workers, web contest workers
- **VM3 (6GB)**: Java "Into the Web" challenges, 8 web contest workers, code-server
- **VM4 (1GB)**: Node.js challenges, 2 web contest workers, code-server
- **VM5 (1GB Azure)**: Python challenges, 2 web contest workers, code-server

### Network
- **Private network**: VM1-4 on 10.0.0.0/24 (<1ms latency)
- **Public access**: VM5 (Azure) connects to VM1 via public IP
- **nginx**: TLS termination, reverse proxy, wildcard IDE subdomain routing
- **Valkey access**: iptables restrict to private network + VM5 IP

### Queue Sharing
- All VMs drain from shared Valkey queues on VM1
- `submission:queue`: Contest/practice submissions
- `web-contest:queue`: "Into the Web" challenges
- **Fair scheduling**: Workers drain both queues with round-robin or priority

---

## 12. Data Flow Patterns

### Contest Lifecycle
```
1. Admin creates contest (UPCOMING status)
2. Users register (ContestRegistration rows)
3. Scheduler transitions to LIVE at startTime
4. Participants submit code → queue → judge → verdict → leaderboard update
5. Scheduler transitions to ENDED at endTime
6. Leaderboard frozen, final rankings persisted
```

### Submission Lifecycle
```
1. User submits code (SubmissionService)
2. DB row created (PENDING status)
3. Job pushed to Valkey queue
4. Worker claims job (LMOVE to processing list)
5. Status updated to JUDGING
6. Code executed in sandbox
7. Verdict computed
8. DB updated with result
9. Leaderboard ZSET incremented (if contest submission)
10. SSE pushed to user
11. Job ACK'd (LREM from processing list)
```

### Proctoring Session
```
1. User enters proctored contest
2. Consent screen + pre-flight checks
3. WebSocket connection established
4. ProctoringSession row created (ACTIVE)
5. Events streamed via WebSocket
6. Risk scoring in background
7. Admin dashboard shows live updates via SSE
8. Session ends → state finalized, screenshots retained 90 days
```

---

## 13. Key Design Principles

### Scalability
- **Stateless API servers**: Can scale horizontally
- **Valkey for shared state**: Cross-instance coordination
- **Worker pool pattern**: Add workers by increasing JUDGE_WORKERS env var
- **Queue-based async**: Decouple submission ingestion from execution

### Performance
- **Cache-first reads**: Reduce DB load
- **Batch queries**: Avoid N+1 with JOINs or findAllById
- **Parallel fetches**: CompletableFuture for independent operations
- **ZSET for leaderboards**: O(log N) rank lookups
- **LMOVE atomicity**: No double-processing without locks

### Reliability
- **Janitor for stuck jobs**: Automatic recovery from worker crashes
- **Retry with backoff**: Email sending, judge execution
- **Graceful degradation**: Local rate limiting if Valkey unavailable
- **Defensive cache eviction**: Always evict even if operation was no-op

### Maintainability
- **Service layer isolation**: Business logic not in controllers
- **Explicit M:N service**: ContestProblemService owns relationship
- **Migration-friendly**: Dual-write pattern for legacy column phaseout
- **Audit logging**: Track admin actions, proctoring events

---

## 14. Integration Points for Private Contest Hosting

### Entities to Extend
- New tables: `contest_hosting_requests`, `private_contests`, `private_contest_invitations`, `private_contest_participants`
- Reuse: `contests`, `problems`, `submissions`, `contest_problems`, `proctored_contests`, `users`

### Services to Integrate
- **ContestService**: Add private contest filtering
- **SubmissionService**: Route to `private:submission:queue`
- **SubmissionWorkerPool**: Drain both public and private queues
- **LeaderboardCacheService**: Use `private:leaderboard:{contestId}` prefix
- **ContestProblemService**: Reuse for private contest problem management
- **ProctoringSessionService**: Extend for private contest proctoring

### Controllers to Add
- **PrivateContestHostingController**: Request approval, create contest
- **PrivateContestManagementController**: Manage participants, problems, analytics
- **PrivateContestInviteController**: Accept invite, join contest
- **AdminHostingApprovalController**: Approve/reject requests, revoke privileges

### Security to Add
- **Host ownership checks**: Verify user is Contest_Host for private contest
- **Participant membership checks**: Verify user is in private_contest_participants
- **Admin oversight**: Allow admins to access all private contests

### Cache Keys to Add
```
private:leaderboard:{contestId}
private:submission:queue
private:contest:{contestId}
private:contests:host:{userId}
private:contests:joined:{userId}
private:invite:{token}
hosting:request:{userId}
hosting:approval:pending
```

---

## 15. Constraints and Trade-offs

### Current Constraints
- **Single PostgreSQL instance**: All data in one DB
- **Valkey single point**: All caching/queuing through one instance
- **Judge workers share resources**: Public + private contests compete for workers
- **No horizontal DB scaling**: Relies on caching to reduce DB load

### Trade-offs Made
- **Eviction over write-through**: Simpler, avoids cache/DB inconsistency
- **Dual-write during migration**: Temporary complexity for smooth transition
- **Per-JVM rate limiting fallback**: Weaker than cluster-wide but better than fail-open
- **Test runs in same table**: `testRun` flag instead of separate table (simpler schema)

### Scalability Limits (Current)
- **~50 concurrent contests**: Based on worker pool capacity
- **~500 active users per contest**: Leaderboard ZSET performs well
- **~100 submissions/sec**: Queue + worker pool throughput
- **26h leaderboard TTL**: Assumes contests don't exceed 24h

---

This architecture analysis provides the foundation for designing the Private Contest Hosting feature while ensuring seamless integration with existing systems.
