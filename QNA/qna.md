# CodeCombat 2026 — Technical Interview Q&A (25+ Questions)

> **Project**: CodeCombat 2026 — Competitive Coding Platform  
> **Tech Stack**: Spring Boot 3.5 (Java 21, WAR), React 19 + Vite, PostgreSQL 18, Valkey (Redis fork), nginx  
> **Deployment**: Dual Oracle Cloud VMs (ARM aarch64), Vercel (frontend)  
> **Key Features**: Live contests, practice mode, live 1v1 duels, AI problem generation, sandboxed code execution  

---

## Q1: Give a high-level architecture overview of your project.

**Answer:**

```
┌──────────────────────────────────────────────────────────────────┐
│                        INTERNET (Users)                           │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼───────────────────────────────────────┐
│              Vercel (React 19 + Vite frontend)                    │
│   • SPA with client-side routing                                 │
│   • Communicates with backend via REST + SSE                     │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTPS (api.codecoder.in)
┌──────────────────────────▼───────────────────────────────────────┐
│               VM1 — nginx reverse proxy (TLS termination)        │
│   • SSL via Let's Encrypt                                        │
│   • Load-balances /api/practice/* across VM1 + VM2               │
│   • Pins SSE, compiler WS, proctoring WS to VM1                 │
└───────┬──────────────────────────────┬───────────────────────────┘
        │ localhost:8080               │ 10.0.0.34:8080
┌───────▼──────────┐          ┌───────▼──────────────────┐
│  VM1 App (WAR)   │          │  VM2 App (WAR)           │
│  1 OCPU, 6GB     │          │  2 OCPU, 10GB            │
│  JUDGE_WORKERS=2 │          │  JUDGE_WORKERS=6         │
│  PRACTICE_WKR=2  │          │  PRACTICE_WKR=4          │
│  + Compiler svc  │          │  (no compiler needed)    │
│  + PostgreSQL 18 │          │                          │
│  + Valkey 7      │          │                          │
└──────────────────┘          └──────────────────────────┘
        ▲                              ▲
        │  Private Network 10.0.0.0/24 (0.4ms latency)  │
        └──────────────────────────────┘
         VM2 connects to VM1's Postgres + Valkey
```

**Key architectural decisions:**
- **Stateless app servers** — JWT auth, no server-side sessions. Both VMs can serve any request.
- **Shared Valkey queue** — Contest submissions use a durable Valkey list (`submission:queue`). Both VMs' workers drain it atomically via `LMOVE`.
- **Synchronous practice** — Practice runs execute in-process (no queue), nginx load-balances them across VMs.
- **SSE for real-time verdicts** — Server-Sent Events push verdicts instantly; polling fallback for cross-VM delivery.

**Follow-up Q: Why not Docker/Kubernetes?**
> Oracle free-tier ARM VMs have limited resources (1-2 OCPU). Docker adds ~200MB overhead + IO penalty on ARM. Local sandboxed execution via `bwrap` (bubblewrap) gives container-equivalent isolation at near-zero overhead. Kubernetes is overkill for 2 nodes.

**Follow-up Q: Why WAR instead of JAR?**
> WAR deployment is a legacy choice from early development. Functionally identical to JAR for embedded Tomcat. We use `java -jar app.war` which Spring Boot handles natively — it's effectively a fat JAR.

---

## Q2: How does the code execution sandbox work? Explain the security model.

**Answer:**

The sandbox uses a **two-layer defense** implemented in `SandboxRunner.java`:

```
┌─────────────────────────────────────────────────────┐
│             LAYER 1: bwrap (bubblewrap)              │
│                                                     │
│  • New PID namespace (can't see host processes)     │
│  • New NET namespace (no network access)            │
│  • New USER namespace (UID 65534 — nobody)          │
│  • New IPC/UTS namespace                            │
│  • Mount namespace:                                 │
│    - /usr, /lib, /etc → read-only bind             │
│    - /tmp, /run → fresh tmpfs                      │
│    - workDir → read-write bind (only this job)     │
│    - /proc, /dev → fresh (minimal devs)            │
│  • --die-with-parent (kill if JVM dies)            │
│  • --cap-drop ALL (no capabilities)               │
│  • --clearenv (no host env vars leaked)            │
│  • --new-session (detach from TTY)                 │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│             LAYER 2: prlimit (inside sandbox)        │
│                                                     │
│  • RLIMIT_AS    → virtual memory cap (256MB+pad)   │
│  • RLIMIT_CPU   → CPU seconds (kills with SIGXCPU) │
│  • RLIMIT_NPROC → max 64 processes/threads         │
│  • RLIMIT_FSIZE → max 16MB file writes             │
│  • RLIMIT_NOFILE → max 64 open file descriptors    │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│            USER CODE EXECUTES HERE                   │
│  (java/python3/g++/gcc/node)                        │
└─────────────────────────────────────────────────────┘
```

**Runtime memory padding** (`DockerJudgeService.runtimePaddingMB`):
- Java: +4096MB (JVM reserves 3+GB virtual address space at startup)
- JavaScript/Node: +1024MB (V8 reserves ~700MB virtual)
- Python: +256MB
- C/C++: +32MB

This padding is for RLIMIT_AS (virtual memory), not actual RAM. The real memory limit is enforced by `-Xmx` (Java) and `--max-old-space-size` (Node).

**Follow-up Q: What happens if user code does `fork()` infinitely?**
> `RLIMIT_NPROC=64` limits total processes. After 64 forks, the kernel returns EAGAIN. Additionally, bwrap's `--die-with-parent` ensures all children die when the parent sandbox dies.

**Follow-up Q: Can user code read environment variables or secrets?**
> No. `--clearenv` wipes all inherited env vars. Only PATH, HOME=/tmp, LANG, LC_ALL are set inside the sandbox.

---

## Q3: Explain the contest submission flow end-to-end.

**Answer:**

```
User clicks "Submit" in frontend
         │
         ▼
┌─── SubmissionController.submit() ───┐
│  1. Validate JWT (AuthTokenFilter)  │
│  2. Extract userId from Principal   │
│  3. Call SubmissionService           │
└──────────────┬──────────────────────┘
               │
               ▼
┌─── SubmissionService.submitCodeAsync() ───┐
│  1. Find/create Submission row (PENDING)  │
│  2. Upsert logic: reuse finished row      │
│     or create new if in-flight            │
│  3. Build SubmissionJob DTO               │
│  4. LPUSH → Valkey "submission:queue"     │
│  5. Return submission ID immediately      │
│     (< 10ms response time)               │
└──────────────┬────────────────────────────┘
               │
               ▼ (asynchronous)
┌─── SubmissionWorkerPool (both VMs) ───────┐
│  Worker loop:                              │
│  1. LMOVE queue → processing:<id> (atomic)│
│  2. Deserialize SubmissionJob              │
│  3. Update DB status → JUDGING            │
│  4. Fetch harness from CacheService       │
│  5. Inject user code into harness          │
│  6. DockerJudgeService.execute()           │
│     → SandboxRunner.wrap() + ProcessBuilder│
│  7. Parse TC:N:PASS/FAIL output            │
│  8. finalizeAndNotify():                   │
│     a. Update DB (verdict, score)          │
│     b. Update Valkey leaderboard (ZINCRBY) │
│     c. Push SSE verdict to user            │
│  9. LREM job from processing list          │
└────────────────────────────────────────────┘
```

**Durability guarantees:**
- If a worker crashes mid-processing, the job stays on the per-worker processing list
- A **janitor** (`@Scheduled` every 60s) scans processing lists and re-enqueues jobs older than 5 minutes
- Jobs are never lost because Valkey persists the queue

**Follow-up Q: What's the difference between "Run" and "Submit"?**
> "Run" (test run) only evaluates the first 2 visible (non-hidden) test cases. It creates a DB row with `isTestRun=true` and never updates the leaderboard. "Submit" runs ALL test cases (visible + hidden) and updates the leaderboard on AC.

**Follow-up Q: Why LMOVE instead of BRPOP?**
> `LMOVE` is atomic — it moves the job from the main queue to a per-worker processing list in one operation. If the worker crashes, the job is still in the processing list (not lost). `BRPOP` would remove the job with no way to recover it if the worker dies before writing the verdict.

---

## Q4: How does the real-time leaderboard work?

**Answer:**

The leaderboard uses **Valkey Sorted Sets (ZSET)** — implemented in `LeaderboardCacheService.java`:

```
Key:    leaderboard:contest:{contestId}
Member: userId (string)
Score:  total score (higher = better)
```

**Operations:**
- `ZINCRBY` on each AC submission — O(log N), atomic, safe for concurrent workers
- `ZREVRANGEWITHSCORES` for top-N — O(log N + K)
- `ZREVRANK` for user's rank — O(log N)

**Flow:**
```
Worker processes AC verdict
    │
    ▼
LeaderboardCacheService.updateScore(contestId, userId, score)
    │
    ▼
redis.opsForZSet().incrementScore(key, userId.toString(), scoreToAdd)
    │
    ▼
redis.expire(key, 26 hours)  // contest duration + buffer
```

**Cold start:** On first access, if the ZSET doesn't exist, we seed it from PostgreSQL via `seedFromDatabase()`.

**Why ZSET over SQL?**
- O(log N) rank queries vs O(N log N) SQL ORDER BY
- Atomic increments — no row locks, no race conditions
- Supports 500+ concurrent users at sub-millisecond latency

**Follow-up Q: What if Valkey crashes mid-contest?**
> The database has all submission verdicts. We re-seed the leaderboard from DB on next access. During the gap (seconds), the frontend shows stale ranks but no data is lost.

---

## Q5: Explain the dual Valkey connection pool architecture.

**Answer:**

Defined in `ValkeyConfig.java` — two separate Lettuce connection pools:

```
┌─────────────────────────────────────────┐
│  Pool 1: apiConnectionFactory (PRIMARY) │
│  • commandTimeout: 2s (fast fail)       │
│  • maxTotal: 20 connections             │
│  • Used by: all REST handlers,          │
│    cache reads, leaderboard, SSE        │
│  • Pattern: quick GET/SET/INCR          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Pool 2: workerConnectionFactory        │
│  • commandTimeout: 10s (allows LMOVE    │
│    to block for 3s without timeout)     │
│  • maxTotal: 16 connections             │
│  • Used by: SubmissionWorkerPool ONLY   │
│  • Pattern: blocking LMOVE (3s wait)    │
└─────────────────────────────────────────┘
```

**Why two pools?**
Previously, workers doing `LMOVE` with 3-second timeouts would block pool connections. API requests needing a quick `GET` would wait in the pool queue → 3-7 second latency spikes for users. Separate pools isolate blocking operations.

**Follow-up Q: Why Valkey instead of Redis?**
> Valkey is a Redis 7.2 fork maintained by the Linux Foundation after Redis changed its license (no longer open-source). It's wire-compatible, so Spring Data Redis works unchanged. We use it because it's truly open-source and Oracle Linux repos package it natively.

---

## Q6: How does JWT authentication work in your system?

**Answer:**

Implemented across `JwtUtils.java`, `AuthTokenFilter.java`, `JwtBlacklistService.java`:

```
┌─── Login Flow ──────────────────────────────────┐
│ 1. POST /api/auth/signin {username, password}   │
│ 2. AuthRateLimiterService.allowLogin(ip)        │
│    → 5 requests / 60s per IP                    │
│ 3. Account lockout check (5 failed → 15min lock)│
│ 4. BCrypt password verification                 │
│ 5. Generate JWT:                                │
│    • Algorithm: HS256                           │
│    • Claims: sub=username, jti=UUID, iat, exp   │
│    • Secret: BASE64-decoded env var             │
│    • Expiry: 24 hours                           │
│ 6. Return { accessToken, username, roles }      │
└─────────────────────────────────────────────────┘

┌─── Request Authentication ──────────────────────┐
│ AuthTokenFilter (runs before every request):    │
│ 1. Extract "Bearer <token>" from header         │
│ 2. Validate signature (HS256)                   │
│ 3. Check JTI not in blacklist (Valkey)          │
│ 4. Check iat > invalidate-before cutoff         │
│ 5. Load UserDetails from DB                     │
│ 6. Set SecurityContext                          │
└─────────────────────────────────────────────────┘
```

**Token revocation (two mechanisms):**
1. **JTI blacklist** — On logout, the token's `jti` is stored in Valkey with TTL = remaining token lifetime. Every request checks `jwt:blacklist:{jti}`.
2. **Invalidate-before** — On password change, we set `auth:invalidate-before:{userId}` = current timestamp. All tokens with `iat` before this are rejected.

**Follow-up Q: What if Valkey is down — can revoked tokens still work?**
> Yes — fail-open by design. Signature validation still gates auth. The alternative (fail-closed) would lock ALL users out during a cache outage. This is an acceptable tradeoff because token lifetime is 24h and password-change is rare.

---

## Q7: How does the live duel mode work?

**Answer:**

Duel mode is a real-time 1v1 competitive coding feature. Core components:
- `MatchmakingService.java` — queue + pairing
- `DuelService.java` — match lifecycle
- `DuelSseEmitterRegistry.java` — real-time event streaming

```
┌─── Matchmaking Flow ───────────────────────────────────────────┐
│                                                                 │
│  User A: POST /api/duels/enqueue {difficulty: "MEDIUM"}         │
│  User B: POST /api/duels/enqueue {difficulty: "MEDIUM"}         │
│                                                                 │
│  Valkey queues (3 buckets):                                     │
│    duel:queue:EASY   → [userId, ...]                            │
│    duel:queue:MEDIUM → [userA, userB, ...]                      │
│    duel:queue:HARD   → [userId, ...]                            │
│                                                                 │
│  @Scheduled pairLoopTick() — runs every 250ms:                  │
│    1. For each difficulty bucket:                                │
│       a. LPOP two users atomically                              │
│       b. Acquire sorted-pair create-lock (SET NX EX 60)         │
│       c. Call duelService.pairAndStart(userA, userB, difficulty) │
│    2. Emit "matched" SSE event to both users                    │
│                                                                 │
│  Queue timeout: 120s — if no opponent found, emit queue_timeout │
└─────────────────────────────────────────────────────────────────┘

┌─── Match Lifecycle (State Machine) ────────────────────────────┐
│                                                                 │
│  WAITING → IN_PROGRESS → FINISHED                               │
│                                                                 │
│  IN_PROGRESS:                                                   │
│    • Draw timer: EASY=1200s, MEDIUM=2400s, HARD=3900s           │
│    • Each user can: Run (5 max) + Submit (2 max)                │
│    • First AC wins immediately                                  │
│    • Both AC → earlier submission wins                          │
│    • Timer expires → DRAW                                       │
│    • Disconnect → 120s grace period for reconnect               │
│    • Both disconnect → combined draw timer                      │
│    • Forfeit → opponent wins                                    │
│                                                                 │
│  Concurrency: ALL state transitions use conditional UPDATE:     │
│    UPDATE duel_matches SET ... WHERE match_id=? AND status=?    │
│    Returns rowCount=1 (won race) or 0 (lost, someone else did)  │
└─────────────────────────────────────────────────────────────────┘
```

**Win adjudication** (`WinAdjudicator.java`):
- Valkey win-claim flag: `SET duel:winner:{matchId} {userId} NX EX 7200` — first AC verdict to claim wins
- DB conditional update: only the row where `status=IN_PROGRESS` transitions to `FINISHED`
- Both layers agree → prevents double-win on concurrent AC verdicts

**Follow-up Q: How do you handle JVM restart mid-duel?**
> `@PostConstruct recoverInProgressMatches()` — on startup, scans `duel_matches WHERE status='IN_PROGRESS'`. For each match, computes remaining time. If time remains → reschedule draw timer. If expired → finalize as ABANDONED.

**Follow-up Q: Why not WebSocket for duels?**
> SSE is simpler (unidirectional server→client), works through all proxies/CDNs, auto-reconnects natively in browsers. User actions (submit/run) go through REST. Two-way WS would add complexity with no real benefit here.

---

## Q8: How does the AI problem generation work?

**Answer:**

Implemented in `AiProblemGeneratorController.java`:

```
Admin provides: topic, difficulty, constraints
         │
         ▼
┌─── Two-pass AI generation ─────────────────────────┐
│                                                     │
│  PASS 1: Problem Statement Generation              │
│    • System prompt: PASS1_SYSTEM                    │
│    • Generates: title, description, examples,       │
│      constraints, time/memory limits                │
│    • Model: DeepSeek API (NVIDIA endpoint)          │
│                                                     │
│  PASS 2: Code Harness Generation                   │
│    • System prompt: HARNESS_SYSTEM + PROBLEM_GUIDE  │
│    • Input: problem statement from Pass 1           │
│    • Generates: complete test harness for all 5     │
│      languages (Java, C++, C, Python, JavaScript)   │
│    • Includes: USER_CODE_START/END markers,         │
│      test driver, hidden test cases                 │
│                                                     │
│  Output format (per language):                     │
│    • LeetCode-style Solution class (Java/C++/Py)   │
│    • Top-level function (JS/C)                     │
│    • TC:N:PASS/FAIL output protocol                │
└─────────────────────────────────────────────────────┘
```

**Harness structure (Java example):**
```java
// === DATA STRUCTURES ===
// class ListNode { int val; ListNode next; ... }

// === SOLUTION CLASS ===
// USER_CODE_START
class Solution {
    public int[] twoSum(int[] nums, int target) {
        // user writes here
    }
}
// USER_CODE_END

// === TEST DRIVER ===
public class Main {
    public static void main(String[] args) {
        Solution sol = new Solution();
        // TC:1:PASS or TC:1:FAIL:input=...:expected=...:got=...
    }
}
```

**Follow-up Q: How do you validate AI-generated harnesses?**
> Currently manual review by admin before publishing. The PROBLEM_GUIDE.md provides strict formatting rules to the AI, and the harness is tested locally before saving.

---

## Q9: Explain the cache-aside pattern in your application.

**Answer:**

Implemented in `CacheService.java`:

```
┌─── Read Path ──────────────────────────────────┐
│                                                 │
│  Request: getProblem(id)                        │
│     │                                           │
│     ▼                                           │
│  Check Valkey: GET problem:{id}                 │
│     │                                           │
│     ├── HIT → deserialize JSON → return         │
│     │                                           │
│     └── MISS → query PostgreSQL                 │
│               → serialize to JSON               │
│               → SET problem:{id} (TTL: 30min)   │
│               → return                          │
└─────────────────────────────────────────────────┘

┌─── Write Path (Eviction) ──────────────────────┐
│                                                 │
│  Admin updates problem:                         │
│     │                                           │
│     ▼                                           │
│  evictProblem(id):                              │
│    • DEL problem:{id}                           │
│    • DEL snippet:{id}:JAVA                      │
│    • DEL snippet:{id}:CPP                       │
│    • DEL snippet:{id}:PYTHON                    │
│    • DEL snippet:{id}:JAVASCRIPT                │
│    • DEL snippet:{id}:C                         │
│                                                 │
│  Next read will repopulate from DB              │
└─────────────────────────────────────────────────┘
```

**TTLs:**
- Problems: 30 minutes
- Code snippets/harnesses: 60 minutes
- Contest list: 30 seconds (changes frequently during active contests)

**Follow-up Q: Why not write-through instead of cache-aside?**
> Write-through adds complexity (must update cache on every DB write). Our writes are infrequent (admin actions), but reads are very frequent (every submission needs the harness). Cache-aside with eviction on write keeps the code simple and guarantees consistency.

---

## Q10: How does the practice mode differ from contest mode technically?

**Answer:**

| Aspect | Contest Mode | Practice Mode |
|--------|-------------|---------------|
| Execution | Async (Valkey queue → worker) | Synchronous (in-process pool) |
| Response | Polling + SSE push | Direct HTTP response |
| DB writes | submissions table | None (except user_problem_solved) |
| Scoring | Leaderboard ZINCRBY | user.total_points increment |
| Time limit | Problem-defined | Same, but 15s hard cap on HTTP |
| Queue | Shared `submission:queue` | ArrayBlockingQueue (in-JVM) |
| Workers | ThreadPoolExecutor (LinkedBlockingQueue) | ThreadPoolExecutor (bounded, AbortPolicy) |
| Load balance | Workers drain shared queue (both VMs) | nginx round-robin (both VMs) |

**Practice rejection policy:** If the bounded queue (1000 capacity) is full, `AbortPolicy` throws `RejectedExecutionException` → user sees "Server busy, try again in a few seconds." This prevents OOM under extreme load.

**Points system (practice):**
- EASY: 5 points
- MEDIUM: 7 points
- HARD: 10 points
- One-time only — uses unique constraint on `(user_id, problem_id)` in `user_problem_solved` table

**Follow-up Q: Why synchronous for practice but async for contests?**
> Contests need durability (job survives server crash) and fairness (FIFO queue). Practice is fire-and-forget — if the server crashes, the user just retries. Synchronous response gives better UX (instant verdict, no polling needed).

---

## Q11: Explain the SSE (Server-Sent Events) architecture.

**Answer:**

```
┌─── SSE Verdict Delivery ───────────────────────────────────────┐
│                                                                 │
│  Frontend:                                                      │
│    1. POST /api/submissions/sse-ticket → get single-use ticket  │
│    2. new EventSource("/submissions/stream?ticket=...")          │
│    3. Listen for "verdict" events                               │
│                                                                 │
│  Backend (SseEmitterRegistry):                                  │
│    • ConcurrentHashMap<userId, Map<subscriptionId, SseEmitter>> │
│    • Multi-tab safe: each tab gets unique subscriptionId        │
│    • 5-minute timeout with heartbeat every 25s                  │
│    • On verdict: fan-out to ALL subscriptions for that user     │
│                                                                 │
│  Security:                                                      │
│    • Single-use ticket (Valkey GETDEL, 60s TTL)                 │
│    • Browsers can't send Authorization header on EventSource    │
│    • Ticket minted only by authenticated user (JWT required)    │
│                                                                 │
│  Fallback:                                                      │
│    • Frontend polls GET /submissions/{id}/status every 3-5s     │
│    • Handles SSE drops, cross-VM delivery gaps                  │
└─────────────────────────────────────────────────────────────────┘
```

**Follow-up Q: SSE is per-JVM in-memory. How do verdicts reach the user if a different VM processes the job?**
> The contest submission queue is shared — any VM's worker can process any job. If VM2 processes a job but the user's SSE stream is on VM1, the SSE push fails silently. The frontend's **polling fallback** (reads from shared PostgreSQL) catches this within 3-5 seconds. Practice mode doesn't have this problem because it's synchronous — the response goes directly back on the same connection.

---

## Q12: How does rate limiting work across your system?

**Answer:**

Two separate rate limiters:

**1. Auth Rate Limiter** (`AuthRateLimiterService.java`):
```
Login:          5 requests / 60s per IP
Register:       3 requests / 3600s per IP
Password Reset: 3 requests / 3600s per email AND per IP
Forgot Username: 3 / 3600s per IP

Account Lockout:
  5 failed logins → 15-minute lock on that username
  Valkey keys: auth:fail:{username}, auth:locked:{username}
```

**2. General API Rate Limiter** (`RateLimiterService.java`):
```
Pattern: Valkey INCR + EXPIRE (sliding window approximation)
Fallback: ConcurrentHashMap per JVM (if Valkey down)
```

**3. Compiler Rate Limiter** (in `CompilerController`):
```
10 runs / 30s per IP (public endpoint, no auth required)
Valkey key: compiler:rl:{ip}
```

**Dual-layer resilience:**
```
Primary:  Valkey INCR + EXPIRE (shared across VMs)
Fallback: ConcurrentHashMap<key, LocalBucket> (per-JVM)

If Valkey is unreachable → local enforcement kicks in.
Neither layer is fail-open for rate limiting.
```

**Follow-up Q: What's the difference between rate limiting and account lockout?**
> Rate limiting is per-IP (prevents brute force from one network). Account lockout is per-username (prevents credential stuffing across distributed IPs). Both use Valkey with local fallback.

---

## Q13: How does the multi-VM deployment work for contest submissions?

**Answer:**

```
┌─── Dual-VM Queue Architecture ─────────────────────────────────┐
│                                                                 │
│  VM1 (2 workers)              VM2 (6 workers)                   │
│  ┌──────────────┐            ┌──────────────┐                   │
│  │ judge-worker-1│            │ judge-worker-1│                   │
│  │ judge-worker-2│            │ judge-worker-2│                   │
│  └──────┬───────┘            │ judge-worker-3│                   │
│         │                    │ judge-worker-4│                   │
│         │                    │ judge-worker-5│                   │
│         │                    │ judge-worker-6│                   │
│         │                    └──────┬───────┘                   │
│         │                           │                           │
│         └───────────┬───────────────┘                           │
│                     │                                           │
│                     ▼                                           │
│        ┌────────────────────────────┐                           │
│        │   Valkey "submission:queue" │ (on VM1, shared)          │
│        │   LMOVE (atomic claim)      │                           │
│        └────────────────────────────┘                           │
│                                                                 │
│  Each worker does:                                              │
│    LMOVE submission:queue → submission:processing:<host>:<idx>  │
│    (blocking wait: 3 seconds, then retry)                       │
│                                                                 │
│  On success: LREM from processing list (ACK)                    │
│  On crash: janitor re-enqueues after 5 minutes                  │
└─────────────────────────────────────────────────────────────────┘
```

**Why this is safe for concurrent VMs:**
- `LMOVE` is atomic — two workers can never claim the same job
- Per-worker processing lists = exactly-once delivery guarantee
- `submission:processing:registry` (SET) tracks all active processing lists
- Janitor scans all registered lists every 60 seconds

**Follow-up Q: Total throughput after adding VM2?**
> VM1 (2 workers × ~5s/job) = 24 jobs/min. VM2 (6 workers × ~5s/job) = 72 jobs/min. Combined = ~96 jobs/min peak. 1000 concurrent users → all done in ~10 minutes (worst case all submit simultaneously).

---

## Q14: How do you handle database connection pooling?

**Answer:**

HikariCP configuration (`application.properties`):

```properties
spring.datasource.hikari.pool-name=CodeCombatPool
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000      # 5 min
spring.datasource.hikari.connection-timeout=5000   # 5s (fail fast)
spring.datasource.hikari.max-lifetime=1200000     # 20 min
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.keepalive-time=60000     # 60s
```

**Why these values?**
- `maximum-pool-size=20`: PostgreSQL defaults to 100 max connections. With 2 VMs × 20 = 40 connections total, well under limit.
- `connection-timeout=5000`: Fail fast — if pool is exhausted, return error in 5s (don't make user wait forever).
- `keepalive-time=60000`: Prevents connections from being killed by firewalls/NAT timeout between VM2 → VM1.
- `max-lifetime=1200000`: Rotate connections every 20 min to pick up DNS changes and avoid stale TCP.

**Follow-up Q: Both VMs share one PostgreSQL — any contention issues?**
> At our scale (few hundred concurrent users), 40 total connections is more than enough. PostgreSQL 18 handles this easily. For higher scale, we'd add PgBouncer as a connection multiplexer in front of Postgres.

---

## Q15: How does Flyway database migration work in a dual-VM setup?

**Answer:**

```
┌─── VM1 starts first (deploy script guarantees this) ───────────┐
│                                                                 │
│  Spring Boot → Flyway runs on startup:                          │
│    1. Check flyway_schema_history table                         │
│    2. Find pending migrations in classpath:db/migration/        │
│    3. Run V1, V2, ... V11 in order                             │
│    4. Record each in flyway_schema_history                      │
│                                                                 │
│  JPA: hibernate.ddl-auto=validate                               │
│    → If entity != DB schema → startup FAILS (safety net)        │
└─────────────────────────────────────────────────────────────────┘

┌─── VM2 starts after VM1 is healthy ───────────────────────────┐
│                                                                │
│  Same Flyway config, same migration files, shared DB:          │
│    1. Check flyway_schema_history → all migrations done ✓      │
│    2. Nothing to run → skip                                    │
│    3. JPA validate → entities match DB ✓                       │
│    4. Start normally                                           │
└────────────────────────────────────────────────────────────────┘
```

**Key settings:**
- `baseline-on-migrate=true` + `baseline-version=1`: Handles the transition from `ddl-auto=update` (legacy) to Flyway-managed migrations.
- `validate-on-migrate=true`: Ensures nobody tampered with already-applied migration files.

**Follow-up Q: What if both VMs start simultaneously?**
> Flyway uses a database-level lock (`SELECT ... FOR UPDATE` on `flyway_schema_history`). If both try to migrate simultaneously, one waits for the other to finish. No corruption possible.

---

## Q16: How does the Turnstile (CAPTCHA) integration work?

**Answer:**

Implemented in `TurnstileService.java`:

```
Frontend (login/register page):
  1. Cloudflare Turnstile widget renders invisible challenge
  2. On solve → returns a token string
  3. Sent with login/register request body

Backend:
  1. Extract turnstile token from request
  2. POST to Cloudflare: https://challenges.cloudflare.com/turnstile/v0/siteverify
     Body: { secret: TURNSTILE_SECRET, response: token, remoteip: clientIp }
  3. Response: { success: true/false }
  4. If TURNSTILE_ENABLED=false → skip validation (for testing)
```

**Follow-up Q: Why Turnstile instead of reCAPTCHA?**
> Turnstile is privacy-first (no tracking cookies), invisible by default (no "click all traffic lights"), and free for unlimited use. reCAPTCHA sends user data to Google and degrades UX.

---

## Q17: Explain the email system architecture.

**Answer:**

Dual SMTP relay configured in `MailConfig.java`:

```
┌─── Sender 1: noreply@codecoder.in ──────────────┐
│  Provider: SendPulse (smtp-pulse.com:587)        │
│  Use: Registration confirmation, password reset  │
│  TLS: STARTTLS required                          │
└──────────────────────────────────────────────────┘

┌─── Sender 2: support@codecoder.in ──────────────┐
│  Provider: Brevo (smtp-relay.brevo.com:587)      │
│  Use: Support ticket auto-replies                │
│  TLS: STARTTLS required                          │
└──────────────────────────────────────────────────┘
```

**Why two providers?**
- Sender reputation isolation: transactional emails (password reset) and support emails have different deliverability profiles
- If one provider is down, the other still works for its use case
- Different From addresses improve email categorization (users recognize noreply vs support)

---

## Q18: How does the compiler playground (public feature) work?

**Answer:**

`CompilerService.java` + `CompilerController.java` + `CompilerWebSocketConfig.java`:

```
┌─── REST Endpoint (anonymous) ──────────────────────────────────┐
│  POST /api/compiler/run {code, language, stdin}                  │
│  • Rate limited: 10/30s per IP (Valkey counter)                 │
│  • Separate thread pool: COMPILER_WORKERS=2                     │
│  • 5-second execution timeout, 256MB memory budget              │
│  • Returns: {stdout, stderr, exitCode, executionTime}           │
│  • Uses same sandbox as contest judge                           │
└─────────────────────────────────────────────────────────────────┘

┌─── WebSocket Endpoint (authenticated) ─────────────────────────┐
│  ws://api.codecoder.in/api/compiler/ws                          │
│  • Single-use ticket auth (same pattern as SSE)                 │
│  • Interactive: supports stdin streaming                        │
│  • Max sessions: COMPILER_WS_MAX_SESSIONS=10                    │
│  • Max runtime: 120 seconds per session                         │
│  • Full bwrap sandbox isolation                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Key design decision:** Compiler has its OWN thread pool, completely separate from the judge engine. A flood of compiler requests can never starve contest submissions.

**Follow-up Q: Why is the REST endpoint anonymous but WS needs auth?**
> REST is stateless — rate limiting by IP is sufficient. WS is long-lived and resource-intensive (holds a process open for up to 120s). Without auth, an attacker could open thousands of WS connections and exhaust server resources.

---

## Q19: How do you handle concurrent access to the same submission row?

**Answer:**

The `SubmissionService.submitCodeAsync()` uses an **upsert pattern**:

```java
// Look at latest submission for user+problem
List<Submission> existing = submissionRepository
    .findByUser_IdAndProblem_IdOrderBySubmittedAtDesc(userId, problemId, PageRequest.of(0,1));

// Reuse row ONLY if verdict is finalized (AC/WA/CE/RE/TLE/MLE)
// If PENDING/JUDGING → create new row (don't overwrite in-flight)
// If test run → create new row (don't mix test/real)
```

**Why this approach?**
- Prevents duplicate submission rows cluttering the database
- Never overwrites an in-flight submission (race condition safety)
- Test runs are always separate (never corrupt real submission history)

**Worker-side idempotency:**
```java
// finalizeAndNotify() uses submissionRepository.updateResult()
// This is a full overwrite of result fields — replaying is safe
// Leaderboard ZINCRBY is keyed off submission, not a counter
```

**Follow-up Q: What if two workers somehow process the same job?**
> Shouldn't happen (LMOVE is atomic). But if it did: the DB update overwrites with the same values (idempotent), and ZINCRBY would double-count. The janitor's reclaim logic prevents this by only re-enqueuing after checking the submission's status in DB.

---

## Q20: Explain the CORS and security headers configuration.

**Answer:**

`SecurityConfig.java` sets comprehensive security:

```
CORS:
  • Allowed origins: env-configured (no wildcards in production)
  • Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
  • Headers: Authorization, Content-Type, X-Requested-With
  • Credentials: true (needed for cookies if ever added)
  • Max age: 3600s (browser caches preflight for 1 hour)

Security Headers:
  • Content-Security-Policy: strict allowlist for scripts/styles/fonts
  • X-Frame-Options: DENY (clickjacking prevention)
  • Strict-Transport-Security: max-age=31536000; includeSubDomains
  • X-Content-Type-Options: nosniff
  • Referrer-Policy: strict-origin-when-cross-origin
  • Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

**HSTS special case:** Spring's built-in HSTS only fires on "secure" requests. Behind nginx TLS termination, the backend sees HTTP. So we use `StaticHeadersWriter` to emit it unconditionally.

**Follow-up Q: Why no CSRF protection?**
> CSRF is disabled because we use JWT bearer tokens (not cookies). CSRF attacks exploit cookie-based auth where the browser auto-attaches credentials. With `Authorization: Bearer <token>`, the browser never sends it automatically — the JavaScript must explicitly attach it.

---

## Q21: How does the connection warmup work and why is it needed?

**Answer:**

`ConnectionWarmupConfig.java`:

```java
@PostConstruct
public void warmup() {
    // 1. Warm PostgreSQL pool — execute SELECT 1
    //    Forces HikariCP to create minimum-idle connections at startup
    //    Instead of lazily creating them on first user request
    
    // 2. Warm Valkey pool — execute PING
    //    Forces Lettuce to establish connections
    //    First real request doesn't pay 50-100ms connection setup cost
}
```

**Why needed on ARM VMs?**
Oracle ARM instances have high first-connection latency (~200ms to PostgreSQL, ~50ms to Valkey). Without warmup, the first user request after a deploy would see 300ms+ response time. Warmup ensures all connections are pre-established during startup (while the health check is still returning unhealthy).

---

## Q22: Explain the contest lifecycle and scheduling.

**Answer:**

`ContestStatusScheduler.java` + `ContestService.java`:

```
Contest States:
  UPCOMING → ACTIVE → COMPLETED

┌─── Scheduler (@Scheduled every 5s) ───────────────────────────┐
│                                                                │
│  1. Find contests where startTime <= now AND status=UPCOMING   │
│     → Update status to ACTIVE                                  │
│                                                                │
│  2. Find contests where endTime <= now AND status=ACTIVE       │
│     → Update status to COMPLETED                               │
│     → Freeze leaderboard (no more ZINCRBY)                     │
│                                                                │
│  3. Evict contest cache on state change                        │
└────────────────────────────────────────────────────────────────┘
```

**Submission guard:** Even if the scheduler hasn't run yet, `SubmissionService` checks `contest.getEndTime().isBefore(now)` before accepting submissions. Belt-and-suspenders approach.

**Follow-up Q: What if the scheduler misses a transition?**
> The 5-second polling ensures transitions happen within 5s of the scheduled time. The submission guard provides a hard fence regardless of scheduler state. A few seconds delay in "ACTIVE→COMPLETED" UI label is acceptable.

---

## Q23: How does the code harness injection work?

**Answer:**

```
┌─── Harness Structure ──────────────────────────────────────────┐
│                                                                 │
│  Stored in code_snippets table per (problem_id, language):      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────┐       │
│  │ // Data structures (ListNode, TreeNode, etc.)       │       │
│  │                                                     │       │
│  │ // USER_CODE_START                                  │       │
│  │ class Solution {                                    │       │
│  │     public int[] twoSum(int[] nums, int target) {   │       │
│  │         // user writes code here                    │       │
│  │     }                                               │       │
│  │ }                                                   │       │
│  │ // USER_CODE_END                                    │       │
│  │                                                     │       │
│  │ // Test driver (Main class)                         │       │
│  │ //   - Instantiates Solution                        │       │
│  │ //   - Runs test cases                              │       │
│  │ //   - Prints TC:1:PASS or TC:1:FAIL:input=...     │       │
│  └─────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘

┌─── Injection Process (injectUserCode) ─────────────────────────┐
│                                                                 │
│  Input: harness template + user's code                          │
│                                                                 │
│  1. Find USER_CODE_START and USER_CODE_END markers              │
│  2. Replace everything between markers with user's code         │
│  3. Result = complete executable program                        │
│                                                                 │
│  Fallback (no markers): replace placeholder strings:            │
│    // USER_CODE_PLACEHOLDER                                     │
│    # USER_CODE_PLACEHOLDER                                      │
│    /* USER_CODE_PLACEHOLDER */                                   │
└─────────────────────────────────────────────────────────────────┘
```

**TC output protocol:**
```
TC:1:PASS                                    → test 1 passed
TC:2:FAIL:input=[1,2]:expected=3:got=5       → test 2 failed, show details
TC:3:PASS:hidden                             → test 3 passed (hidden, no details shown)
```

**Follow-up Q: What does the user actually see in the editor?**
> Only the code between USER_CODE_START and USER_CODE_END — the Solution class with method signature. The test driver is completely hidden. `CodeSnippetService.extractStarterCode()` strips the harness and shows only the editable portion.

---

## Q24: How does the startup recovery for in-flight submissions work?

**Answer:**

`StartupRecoveryConfig.java`:

```
┌─── On JVM Startup ─────────────────────────────────────────────┐
│                                                                 │
│  Problem: JVM was killed while jobs were in-flight.             │
│  Jobs are in submission:processing:<old_pid> lists but          │
│  no worker is alive to finish them.                             │
│                                                                 │
│  Solution: Janitor runs every 60s, but first run catches        │
│  stale jobs from previous JVM immediately:                      │
│                                                                 │
│  1. SMEMBERS submission:processing:registry                     │
│     → list of all known processing keys (including dead ones)   │
│                                                                 │
│  2. For each processing list:                                   │
│     a. LRANGE → get all jobs                                    │
│     b. For each job:                                            │
│        - Check DB status (PENDING/JUDGING = still needs work)   │
│        - Check age (submittedAt vs now)                         │
│        - If age > 5 minutes:                                    │
│          • LREM from processing list                            │
│          • LPUSH back to submission:queue (retry)               │
│          • Log: "Reclaimed stuck job"                           │
│        - If already has final verdict (AC/WA/etc):              │
│          • Just LREM (clean up)                                 │
│                                                                 │
│  Result: No submission is ever permanently lost.                │
│  Worst case: 5 minute delay before retry.                       │
└─────────────────────────────────────────────────────────────────┘
```

**Follow-up Q: Can a job be processed twice?**
> In theory, if the janitor re-enqueues a job that was actually being processed by a slow worker on another VM. In practice, 5-minute timeout is generous (worst execution is ~15s). Even if it happens, `updateResult()` is idempotent (overwrite with same data). Leaderboard ZINCRBY would double-count, but this edge case hasn't occurred in production.

---

## Q25: How does the nginx load balancing work for practice runs?

**Answer:**

```nginx
upstream practice_pool {
    server 127.0.0.1:8080;       # VM1 (local)
    server 10.0.0.34:8080;       # VM2 (private network)
    keepalive 16;                 # persistent connections
}

location /api/practice/ {
    proxy_pass http://practice_pool;
    proxy_next_upstream error timeout http_502 http_503 http_504;
    proxy_next_upstream_tries 2;
    proxy_read_timeout 60s;
}
```

**Why only practice is load-balanced:**

| Route | Strategy | Reason |
|-------|----------|--------|
| `/api/practice/*` | Round-robin (VM1+VM2) | Synchronous, CPU-bound |
| `/api/submissions/*` | VM1 only | Workers drain shared queue anyway |
| `/api/compiler/*` | VM1 only | Stateful WS, stays on one VM |
| `/api/submissions/stream` | VM1 only | SSE is in-memory per-JVM |
| Everything else | VM1 only | Stateless but low CPU |

**`proxy_next_upstream`**: If VM2 returns 502/503/504 or times out, nginx transparently retries on VM1. User never sees an error if one VM is restarting.

**Follow-up Q: What's `keepalive 16`?**
> nginx maintains 16 persistent TCP connections to each upstream. Without keepalive, every request does a TCP handshake (1ms extra per request on private network). With keepalive, connections are reused — zero handshake overhead after the first 16 requests.

---

## Q26: How does the dual-VM deploy process work?

**Answer:**

`deploy_all.sh`:

```bash
# Deployment order matters:
# 1. VM1 first → Flyway migrations land on shared DB
# 2. Health check VM1 → confirm migrations ran
# 3. VM2 second → validates schema (no new migrations)
# 4. Health check VM2 → confirm it connected to VM1's DB/Valkey

deploy_vm1() {
    ssh VM1 'cd ~/codecombat && git pull origin main'
    ssh VM1 './mvnw -q -DskipTests clean package'
    ssh VM1 'cp target/*.war ~/app.war'
    ssh VM1 'sudo systemctl restart codecombat'
}

deploy_vm2() {
    ssh VM2 'cd ~/codecombat && git pull origin main'
    ssh VM2 './mvnw -q -DskipTests clean package'
    ssh VM2 'sudo cp target/*.war /opt/codecombat/app.war'
    ssh VM2 'sudo restorecon -v /opt/codecombat/app.war'  # SELinux
    ssh VM2 'sudo systemctl restart codecombat'
}

health_check() {
    # Poll localhost:8080/api/health every 5s for up to 60s
    # Spring Boot returns {"status":"UP"} when ready
}
```

**SELinux note (VM2 — Oracle Linux):** SELinux enforcing mode requires proper file contexts. WAR lives in `/opt/codecombat/` with `usr_t` context. `restorecon` ensures systemd can read it after copy.

**Follow-up Q: What happens during rolling deploy — do users see errors?**
> During the ~30s restart window per VM, nginx's `proxy_next_upstream` handles it for practice routes. For non-load-balanced routes, there's a brief 502 window. In production, we'd add graceful shutdown (already configured: `server.shutdown=graceful`, `timeout-per-shutdown-phase=30s`) so in-flight requests complete before the old JVM dies.

---

## Q27: How does the Valkey security work in production?

**Answer:**

```
┌─── Before (insecure) ──────────────────────────────────────────┐
│  bind 127.0.0.1                                                 │
│  protected-mode yes                                             │
│  requirepass (none)                                             │
│  → Only localhost could connect, no auth needed                 │
└─────────────────────────────────────────────────────────────────┘

┌─── After (production, dual-VM) ────────────────────────────────┐
│  bind 127.0.0.1 10.0.0.221                                     │
│  protected-mode no (password replaces it)                       │
│  requirepass e8e61dca6443e01f63c82d8403dabfa4eaeba6bccd2946a7   │
│                                                                 │
│  iptables:                                                      │
│    ACCEPT tcp 10.0.0.0/24 → port 6379 (private subnet only)    │
│    REJECT all others before port 6379                           │
│                                                                 │
│  → VM2 connects with password over private network              │
│  → Public internet cannot reach port 6379                       │
└─────────────────────────────────────────────────────────────────┘
```

**Layers of protection:**
1. Valkey binds only localhost + private IP (not 0.0.0.0)
2. Requires strong 48-character hex password
3. iptables only allows `10.0.0.0/24` subnet on port 6379
4. OCI Security List (cloud firewall) — additional layer
5. VM2's ENV stores the password in `/opt/codecombat/codecombat.env` with chmod 600

---

## Q28: Explain the WebSocket compiler session lifecycle.

**Answer:**

`CompilerSessionHandler.java` + `CompilerWebSocketConfig.java`:

```
┌─── WebSocket Lifecycle ────────────────────────────────────────┐
│                                                                 │
│  1. Client: POST /api/compiler/ws-ticket (JWT auth)             │
│     → Returns single-use ticket (Valkey, 60s TTL)               │
│                                                                 │
│  2. Client: ws://api.codecoder.in/api/compiler/ws?ticket=...    │
│     → Handshake interceptor:                                    │
│       a. GETDEL ticket from Valkey (atomic consume)             │
│       b. If invalid → reject upgrade                            │
│       c. Extract userId, store in session attributes            │
│                                                                 │
│  3. Client sends: {type:"run", code:"...", lang:"PYTHON",       │
│                    stdin:"hello"}                                │
│     → Handler:                                                  │
│       a. Spawn sandbox process (SandboxRunner)                  │
│       b. Pipe stdin to process                                  │
│       c. Stream stdout/stderr back as WS frames                 │
│       d. On process exit → send {type:"exit", code:0}           │
│                                                                 │
│  4. Limits:                                                     │
│     • Max concurrent sessions: 10                               │
│     • Max runtime per session: 120 seconds                      │
│     • If exceeded → force-kill process, close WS                │
│                                                                 │
│  5. Client disconnect → kill process immediately                │
└─────────────────────────────────────────────────────────────────┘
```

**Follow-up Q: Why single-use tickets instead of JWT in WS URL?**
> Browsers cannot set `Authorization` header on WebSocket upgrade requests. Putting JWT in the URL query string would leak it in server logs, proxy logs, and browser history. Single-use tickets (GETDEL in Valkey) are consumed atomically on first use — even if logged, they're already invalid.

---

## Q29: How do you handle the "Test Run" vs "Submit" distinction in contests?

**Answer:**

```
┌─── "Run" (Test Run) ───────────────────────────────────────────┐
│  • Only runs first 2 VISIBLE (non-hidden) test cases           │
│  • Creates DB row with isTestRun=true                           │
│  • NEVER updates leaderboard                                    │
│  • NEVER counts as official submission                          │
│  • Purpose: let user verify their approach before committing    │
│  • Frontend shows: "2/2 passed" or "1/2 — check your logic"    │
└─────────────────────────────────────────────────────────────────┘

┌─── "Submit" (Real Submission) ─────────────────────────────────┐
│  • Runs ALL test cases (visible + hidden)                       │
│  • Creates/updates real submission row                          │
│  • On AC → ZINCRBY leaderboard                                 │
│  • Score = (passed / total) * 100                               │
│  • This is the official verdict for ranking                     │
└─────────────────────────────────────────────────────────────────┘
```

**Implementation in `parseOutput()`:**
```java
if (isTestRun) {
    // Only take first SAMPLE_TC_LIMIT visible TCs
    for (TcLine tc : allLines) {
        if (!tc.hidden) {
            lines.add(tc);
            if (lines.size() >= SAMPLE_TC_LIMIT) break;  // SAMPLE_TC_LIMIT = 2
        }
    }
}
```

**Follow-up Q: Why limit test runs to 2 test cases?**
> Prevents users from reverse-engineering hidden test cases through repeated test runs. If we showed all test case results, users could binary-search for edge cases without actually solving the problem.

---

## Q30: What monitoring and observability do you have?

**Answer:**

```
┌─── Health Endpoint ────────────────────────────────────────────┐
│  GET /api/health → {"status":"UP"}                              │
│  • Configured to skip Redis/DB/disk health checks               │
│  • Only ping probe enabled (instant response)                   │
│  • Why skip checks? Each adds 500ms-3s on ARM VM               │
│  • Used by: nginx health checks, deploy script, systemd         │
└─────────────────────────────────────────────────────────────────┘

┌─── Queue Status Endpoint ──────────────────────────────────────┐
│  GET /api/queue-status → {                                      │
│    "activeJobs": 3,                                             │
│    "estimatedWaitSeconds": 15,                                  │
│    "totalProcessed": 1847,                                      │
│    "queueDepth": 12                                             │
│  }                                                              │
│  • Real-time visibility into judge worker load                  │
│  • Used by admin dashboard                                      │
└─────────────────────────────────────────────────────────────────┘

┌─── Logging ────────────────────────────────────────────────────┐
│  • Production: LOG_LEVEL_APP=WARN (minimal noise)              │
│  • Incident triage: LOG_LEVEL_APP=DEBUG (temporary)            │
│  • journalctl -u codecombat -f (live tailing)                  │
│  • Worker logs: every job → status, passed/total, elapsed ms    │
└─────────────────────────────────────────────────────────────────┘
```

**Follow-up Q: Would you add metrics (Prometheus/Grafana) at scale?**
> Yes. I'd expose Micrometer metrics via Actuator (`/actuator/prometheus`), track: request latency histograms, queue depth gauge, worker pool active/idle counts, cache hit ratio, error rates. Grafana dashboards for visualization. At current scale (200-500 users), journalctl + queue-status is sufficient.

---

## Bonus Q31: What would you change for 10x scale (5000 concurrent users)?

**Answer:**

```
Current (500 users):
  • 2 VMs, 3 OCPU total, shared Postgres/Valkey on VM1

10x scale changes:
  1. Dedicated DB VM (Postgres on its own 4 OCPU machine)
  2. Dedicated Valkey VM (or Valkey Cluster for sharding)
  3. 3-5 execution VMs (each 2-4 OCPU, draining shared queue)
  4. PgBouncer connection pooler (100+ app connections → 20 PG conns)
  5. Read replicas for heavy-read queries (problem listing, user profiles)
  6. CDN for static assets (currently Vercel handles this)
  7. Practice mode → queue-based (like contests) to handle backpressure
  8. SSE → Valkey Pub/Sub bridge for cross-VM verdict delivery
  9. Kubernetes for auto-scaling execution pods during contest peaks
  10. Object storage (S3/OCI) for screenshots instead of DB BLOB
```

**Follow-up Q: What's the biggest bottleneck right now?**
> CPU on execution. Each code execution takes 1-5 seconds of CPU. With 8 total workers, max throughput is ~96 jobs/minute. Adding more execution VMs is linear scaling — the queue architecture already supports it with zero code changes.

---

*Prepared for technical interview — all answers based on actual production code in the CodeCombat 2026 repository.*
