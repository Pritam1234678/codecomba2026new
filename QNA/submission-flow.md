# CodeCombat Submission Engine — Complete Architecture

---

## Ek Line Me Samjho

> User code likhta hai → Submit/Run dabata hai → Backend Valkey queue me daalta hai → Worker execute karta hai (bwrap sandbox) → Result SSE/polling se wapas aata hai

---

## Architecture Overview (Single VM)

```
USER (Browser)                          Backend (VM)                Valkey              PostgreSQL
     │                                       │                         │                     │
     │── POST /api/submissions ─────────────►│                         │                     │
     │     or /api/practice/run              │── Save PENDING ─────────────────────────►│
     │                                       │── LPUSH job ────────────►│                │
     │◄─ 202 Accepted (subId) ───────────────│                         │                     │
     │                                       │                         │                     │
     │  (SSE open / polling)                 │  [Worker thread]        │                     │
     │                                       │── LMOVE: claim job ────►│                     │
     │                                       │── Execute (bwrap) ──────┤                    │
     │                                       │── Write verdict ─────────────────────────►│
     │                                       │── ZINCRBY leaderboard ──►│                     │
     │                                       │── GitHub push ──────────►│                     │
     │                                       │── Streak update ─────────┤                    │
     │◄─ SSE "verdict" / polling ────────────│                         │                     │
```

---

## Submission Types

| Type | Endpoint | DB Table | Leaderboard | GitHub Push | Streak |
|------|----------|----------|-------------|-------------|--------|
| Contest | `POST /api/submissions` | `submissions` | Yes | No | Yes |
| Practice | `POST /api/practice/run` | `practice_submissions` | No | Yes (on AC) | Yes |
| Duel | `POST /api/duel/{id}/submit` | `submissions` | No (duel adjudication) | No | Yes |
| Test Run | Same endpoints (isTestRun=true) | Same tables | No | No | No |
| Web Contest | `POST /api/web-contest/submit` | `submissions` | Yes (own ZSET) | No | Yes |

**ALL types go through the same Valkey queue (`submission:queue`) and same `SubmissionWorkerPool`.**

---

## STEP 1 — User Submit/Run Karta Hai

### Contest Submission
```
POST /api/submissions
Body: { problemId, code, language }
→ SubmissionController.submitCode()
→ SubmissionService.submitCodeAsync()
```

### Practice Run
```
POST /api/practice/run
Body: { problemId, code, language }
→ PracticeController.run()
→ PracticeService.enqueuePractice()
```

### Duel Submission
```
POST /api/duel/{matchId}/submit
→ DuelController.submitCode()
→ pushes to submission:queue with duelId set
```

**All three eventually call `redis.opsForList().leftPush("submission:queue", jobJson)`**

---

## STEP 2 — SubmissionJob Structure

```java
SubmissionJob {
    Long submissionId;       // DB row ID (null for test runs)
    Long userId;
    Long problemId;
    Long contestId;          // null for practice
    String code;
    String language;         // JAVA, CPP, PYTHON, JAVASCRIPT, C
    Double timeLimit;        // seconds
    Integer memoryLimit;     // MB
    boolean testRun;         // true = "Run" button, not saved
    UUID duelId;             // null unless duel match
    Long proctoringSessionId;
    boolean practice;        // true = practice mode
}
```

---

## STEP 3 — Worker Pool (SubmissionWorkerPool)

**2 workers** on single VM (configurable via `JUDGE_WORKERS=2`):

```
Worker Thread (judge-worker-1, 2):

  Loop forever:
    LMOVE submission:queue → submission:processing:{host}:{idx}
    (blocking wait, processes from RIGHT = FIFO)

    If job mila:
      → Update DB status → JUDGING
      → Fetch harness from cache (Valkey or DB)
      → Inject user code between USER_CODE markers
      → DockerJudgeService.execute()  ← bwrap sandbox
      → parseOutput() — parse TC: lines from stdout
      → finalizeAndNotify() — update DB, leaderboard, SSE, streak, GitHub
    If timeout (no job):
      → loop again
```

**LMOVE vs BRPOP:** LMOVE is atomic — job moves from `submission:queue` to `submission:processing:{host}:{idx}`. If worker crashes, job stays in processing list. Janitor reclaims stuck jobs after 5 minutes. ZERO job loss.

---

## STEP 4 — Sandbox Execution (bwrap + prlimit)

**File:** `DockerJudgeService.java` (named "Docker" but uses bwrap, not Docker)

### Per-Language Execution

```
JAVA:
  1. javac -cp . Solution.java    (compile, 30s limit)
  2. java -Xmx{mem}m -cp . Main   (run, inside bwrap)

CPP:
  1. g++ -O2 -o binary source.cpp  (compile)
  2. ./binary                       (run)

PYTHON:
  1. python3 solution.py            (run directly)

JAVASCRIPT:
  1. node --max-old-space-size={mem} solution.js  (run)

C:
  1. gcc -O2 -o binary source.c    (compile)
  2. ./binary                       (run)
```

### Sandbox Layers

```
Layer 1: bwrap (bubblewrap)
  • New PID namespace (host processes invisible)
  • New network namespace (no internet)
  • Filesystem: /usr, /lib, /bin, /etc → read-only
  • /tmp, /run → tmpfs (per-job temp files)
  • UID=65534 (nobody, no privileges)
  • --die-with-parent (kill all if parent dies)

Layer 2: prlimit
  • RLIMIT_AS: memory limit (virtual address space)
  • RLIMIT_CPU: CPU seconds limit
  • RLIMIT_NPROC: max 64 processes
  • RLIMIT_FSIZE: max 16MB files
  • RLIMIT_NOFILE: max 64 open files
```

### Execution Timing (fixed July 2026)

```
startMs = System.currentTimeMillis();    ← AFTER pb.start() + stdin write
process.waitFor(timeLimit + 5, SECONDS);
elapsed = System.currentTimeMillis() - startMs;

Previously: startMs was BEFORE pb.start(), including Docker/bwrap
startup overhead (1-5s). Now only measures actual execution time.
```

---

## STEP 5 — Output Parsing

Harness prints one line per test case to stdout:

```
TC:1:PASS                     ← visible, passed
TC:2:FAIL:input=[4,2,0]:expected=4:got=0  ← visible, failed
TC:3:PASS:hidden              ← hidden, passed
TC:4:FAIL:hidden              ← hidden, failed (no debug info)
```

### try-catch in Harness (Added July 2026)

Every `test()` call in `main()` is wrapped in try-catch:

```java
try { test(arr, exp, 1, false); }
catch (Exception e) { System.out.println("TC:1:FAIL:...got=ERR"); }
```

This prevents a crash in one test case from killing the entire process. Previously, if test 3 crashed, tests 4-6 would never run — making WA/RE appear faster than AC.

### Verdict Resolution

| Exit Code | Meaning | Verdict |
|-----------|---------|---------|
| 0 | Normal | Check TC lines → AC or WA |
| 1 | Compile error | CE (stderr shown to user) |
| 137 (SIGKILL) | Killed by OOM | MLE |
| 139/152 (SIGXCPU) | CPU limit | TLE |
| Other | Runtime crash | RE |
| Timed out | process.waitFor returned false | TLE |

---

## STEP 6 — finalizeAndNotify()

**Called with all verdict data:**

```
1. DB Update
   → UPDATE submissions SET status=?, score=?, timeConsumed=?, testCaseDetails=?
   → UPDATE practice_submissions SET ... (for practice)
   → Only updates if current status is PENDING/JUDGING (idempotent)

2. Leaderboard (contest only, non-duel, non-test-run)
   → ZINCRBY leaderboard:contest:{contestId} {userId} {score}
   → Per-problem score tracked via Valkey key (delta approach)
   → Example: WA 50 → AC 100 = delta +50

3. Cache Eviction
   → DEL submissions:user:{userId}
   → DEL submission:status:{submissionId}
   → DEL submission:user:problem:{userId}:{problemId}

4. SSE Push
   → sseRegistry.sendVerdict(userId, VerdictEvent)
   → Browser receives real-time verdict
   → VerdictEvent includes: status, score, testCasesPassed, timeConsumedMs,
     testCaseDetails, testRun flag, practice flag, pointsAwarded

5. GitHub Auto-Push (practice AC only)
   → githubService.pushSolution(userId, submissionId, problemTitle, language, code)
   → Creates/updates file in user's CodeCoder GitHub repo
   → Path: {problem-slug}/{lang}{n}Solution.{ext}

6. Streak Update (all real submissions)
   → streakService.updateStreak(userId)
   → Consecutive day → increment current streak
   → Gap → reset to 1, max = MAX(max, current)

7. Job Cleanup
   → LREM from processing list (job acknowledged)
```

---

## STEP 7 — User Ko Result Milta Hai

### Primary: SSE (Server-Sent Events)

```
Frontend:
  1. POST /api/submissions/sse-ticket → ticket
  2. new EventSource("/submissions/stream?ticket=...")
  3. es.addEventListener("verdict", handler)
  4. Auto-reconnect on error (3s delay, new ticket)

Backend:
  1. Ticket validated (Valkey GETDEL — single use)
  2. SseEmitter registered per userId
  3. On verdict → push to emitter
  4. Connection dropped → emitter auto-expires
```

### Fallback: Polling

```
Frontend polls every 2-3s:
  GET /api/submissions/{submissionId}/status

Backend:
  Checks BOTH submissions AND practice_submissions tables
  Returns verdict if status is final (not PENDING/JUDGING)
```

---

## Run vs Submit

| | Run (Test) | Submit (Real) |
|---|---|---|
| `isTestRun` | true | false |
| Visible test cases | First 2-3 only | All |
| DB row | Saved (for polling) | Saved (real) |
| Leaderboard | No | Yes (contest) |
| GitHub push | No | Yes (practice AC) |
| Streak | No | Yes |
| Score | Shown but not stored | Stored in DB |

---

## Crash Recovery — Janitor

```
@Scheduled every 60 seconds:

1. Scan all submission:processing:* keys
2. For each job in processing:
   - Check DB: is submission still PENDING/JUDGING?
   - Check age: has claim been held > 5 minutes?
   - If stuck → LREM from processing list → LPUSH back to queue
3. Log: "Reclaimed X stuck job(s)"
```

Jobs are NEVER permanently lost — at worst, 5-minute delay before re-processing.

---

## Practice Submission Flow

Practice uses the SAME queue and worker pool as contest submissions:

```
User clicks "Run" → POST /api/practice/run
  → PracticeController.run()
    → PracticeService.enqueuePractice()
      → Sets job.practice = true, job.testRun = false
      → LPUSH submission:queue

Worker processes normally:
  → If practice + AC + !testRun + updated > 0:
    → practiceService.awardPointsIfFirstSolve()  (5/7/10 pts by difficulty)
    → githubService.pushSolution()               (auto-sync to GitHub)
    → streakService.updateStreak()               (daily streak)
  → DB: practice_submissions table (not submissions)
  → Leaderboard: SKIPPED (no contest)
```

---

## Duel Submission Flow

```
User submits in duel → POST /api/duel/{matchId}/submit
  → DuelController.submitCode()
    → Sets job.duelId = match UUID
    → LPUSH submission:queue

Worker:
  → duelService.onDuelVerdict() handles adjudication
  → NO leaderboard update (Per Property 13)
  → NO GitHub push
  → streakService.updateStreak() still runs
```

---

## Web Contest Flow (separate pool)

```
Web contest uses WebContestWorkerPool (4 workers)
  → Same LMOVE pattern
  → Own leaderboard ZSET per contest
  → Maven-based test execution (not single harness)
  → streakService.updateStreak() runs
```

---

## Possible Verdicts

| Verdict | Meaning | Kab aata hai |
|---------|---------|-------------|
| AC | Accepted | All test cases passed |
| WA | Wrong Answer | Output doesn't match expected |
| CE | Compile Error | Code failed to compile |
| RE | Runtime Error | Code crashed during execution |
| TLE | Time Limit Exceeded | Execution exceeded time limit |
| MLE | Memory Limit Exceeded | Memory usage exceeded limit |
| PENDING | Queued | Waiting in Valkey queue |
| JUDGING | Running | Worker is executing |

---

## Key Design Decisions

### LMOVE over BRPOP
LMOVE is atomic claim-and-move. BRPOP is fire-and-forget. If worker crashes after BRPOP, job is lost forever. With LMOVE, job stays in processing list → Janitor reclaims.

### Lock-Free Architecture
No Java `synchronized` or `Lock` anywhere. PostgreSQL MVCC + Valkey atomic commands (LMOVE, INCR, ZINCRBY) handle all concurrency. Single-threaded Valkey command execution makes LMOVE inherently safe across workers.

### Single Queue, Multiple Job Types
Contest, practice, duel, web contest all push to `submission:queue`. The `SubmissionJob` DTO carries flags (`practice`, `duelId`, `testRun`) that the worker uses to branch behavior. Clean separation — one queue, many paths.

### Practice vs Contest DB Tables
Practice uses `practice_submissions` table (separate from `submissions`). Keeps contest data clean. Practice submissions have `github_pushed` flag for tracking sync status.

---
### FQ1: Tumne LMOVE use kiya hai — BRPOP se kyu nahi?

**Answer:**

```
┌─── BRPOP Approach (Risky) ──────────────────────────┐
│                                                     │
│  Worker: BRPOP submission:queue 3                   │
│  → Job queue se permanently remove ho gaya          │
│                                                     │
│  Ab agar worker crash ho jaye (OOM, SIGKILL):       │
│  → Job kahi bhi nahi hai — LOST FOREVER ❌           │
│  → User ka submission permanent PENDING reh gaya     │
└─────────────────────────────────────────────────────┘

┌─── LMOVE Approach (Safe — hamara) ──────────────────┐
│                                                     │
│  Worker: LMOVE submission:queue →                    │
│          submission:processing:vm2:3                 │
│  → Job queue se hata ke processing list me gaya     │
│  Agar worker crash ho jaye:                         │
│  → Job abhi bhi processing list me pada hai ✓       │
│  → Janitor 5 min baad detect karega                 │
│  → LPUSH back to queue → koi aur worker uthayega    │
│  → ZERO JOB LOSS ✓                                  │
└─────────────────────────────────────────────────────┘
```

**One-liner:** BRPOP = fire-and-forget (risky). LMOVE = claim-and-acknowledge (durable). Production me kabhi BRPOP use mat karo for critical workloads.

---

### FQ2: Rate limiting kaise implement kiya hai? Kya hoga agar Valkey down ho?

**Answer:**

```
Primary layer: Valkey INCR + EXPIRE
─────────────────────────────────────
  Key:    sub:rl:{userId}
  Logic:  INCR key → count
          If count == 1: EXPIRE key 10s
          If count > 3:  reject (429)
  Shared across VMs ✓

Fallback layer: ConcurrentHashMap (per JVM)
─────────────────────────────────────
  Map<String, LocalBucket>
  LocalBucket = { windowStartMs, count, windowSec }
  Not shared (each VM has its own) ✗
  But still protects against abuse ✓
```

**Valkey down scenario:**
```
1. redis.increment() throws Exception
2. Catch block → allowLocally(key, max, windowSec)
4. ConcurrentHashMap se check karo local count
5. User ko zyada generous limit milega ( instead of global)
   But NEVER completely unprotected
```

**Follow-up to follow-up:** "Global limit 3/10s hai, Valkey down hone pe 3/10s PER VM ho jayega — matlab 6/10s total across single VM. Acceptable tradeoff — better than zero protection."

---

### FQ3: Agar dono VMs ka worker ek hi job uthane ki koshish kare to?

**Answer:**

**Ye possible hi nahi hai** kyunki LMOVE atomic hai:

```
LMOVE = single Redis command = atomic

Thread A (VM): LMOVE submission:queue → processing:vm1:0
Thread B (VM): LMOVE submission:queue → processing:vm2:3

Redis executes commands sequentially (single-threaded):
  → Thread A gets job1
  → Thread B gets job2 (next item)
  → NEVER same job to two workers
```

Redis/Valkey is single-threaded for command execution. Two `LMOVE` commands on the same list are serialized at the Valkey level. No locking needed on the application side.

---

### FQ4: SSE connection (single VM, no cross-VM issue) delivery problem — kaise solve kiya?

**Answer:**

```
Problem:
  User ka SSE stream VM pe open hai
  User ka submission VM ke worker ne process kiya
  VM ka worker SSE push karta hai:
    sseRegistry.sendVerdict(userId=42, verdict)
  BUT: VM ke SseEmitterRegistry me userId=42 ka emitter nahi hai!
  → Push silently fails on VM

Solution: Frontend polling fallback
──────────────────────────────────
  Frontend code (ProblemSolve.jsx):
    pollVerdict(submissionId) runs every 3s:
      GET /submissions/{id}/status
      → reads from shared PostgreSQL (the VM same DB)
      → single VM don't matter, DB is shared

Verified behavior:
  Best case (same VM processed): <1 second (SSE instant)
  Worst case ((single VM, no cross-VM issue)): 3-5 seconds (polling catches it)

Better solution (future): Valkey Pub/Sub bridge — worker publishes verdict to `channel:verdict:{userId}`, all VMs subscribe and push to local SSE. But current polling approach works perfectly at our scale.
```

---

### FQ5: `submitCodeAsync()` me upsert logic kyun lagaya? Har baar nayi row kyun nahi?

**Answer:**

```
Problem agar har baar nayi row banate:
  1 user × 1 problem × 50 submits = 50 rows
  500 users × 5 problems × 50 submits = 125,000 rows per contest
  10 contests = 1.25 MILLION rows

Upsert approach:
  1 user × 1 problem = MAX 1 real submission row (reused)
  + test run rows (separate, but also bounded)
  500 users × 5 problems = 2,500 real submission rows per contest
  10 contests = 25,000 rows ← 50x less!
```

**Rule:**
```java
// Reuse row if latest verdict is FINAL (AC/WA/CE/RE/TLE/MLE)
// Create new row if latest is IN-FLIGHT (PENDING/JUDGING)
//   → because overwriting in-flight would corrupt running judge
// Create new row if latest is TEST RUN (isTestRun=true)
//   → because test and real are logically different
```

**Benefit:** DB stays small, queries fast, no unbounded growth.

---

### FQ6: Sandbox me TLE kaise detect karta hai? Double-kill mechanism kya hai?

**Answer:**

```
┌─── TLE Detection (3 layers) ───────────────────────┐
│                                                     │
│  Layer 1: prlimit RLIMIT_CPU                        │
│    → Kernel sends SIGXCPU after N CPU seconds       │
│    → Exit code 152 (128 + 24)                       │
│    → Catches CPU-bound infinite loops               │
│                                                     │
│  Layer 2: Process.waitFor(timeLimit + 5, SECONDS)   │
│    → JVM waits max (timeLimit + 5) wall-clock       │
│    → If exceeded → destroyForcibly()                │
│    → Catches I/O-bound hangs (sleep, network wait)  │
│                                                     │
│  Layer 3: Wall-clock elapsed check                  │
│    → Even if process exited code=0:                 │
│    → if elapsed > timeLimit*1000ms → TLE            │
│    → Safety net for edge cases                      │
└─────────────────────────────────────────────────────┘

┌─── Double-Kill (Process MUST die) ──────────────────┐
│                                                     │
│  destroyForcibly()  ← sends SIGKILL                 │
│  process.waitFor(2, SECONDS)  ← confirm death       │
│  if STILL alive:                                    │
│    log.error("survived destroyForcibly!")            │
│  process.descendants().forEach(destroyForcibly)     │
│    → kill all child processes too                    │
│  With bwrap: --die-with-parent                      │
│    → Killing bwrap parent = entire namespace dies   │
│    → All children auto-killed by kernel ✓            │
└─────────────────────────────────────────────────────┘
```

---

### FQ8: Score calculation kaise hota hai? Leaderboard update atomic hai?

**Answer:**

```
Score Formula:
  score = (passed_test_cases / total_test_cases) × 100
  Example: 3/4 passed → score = 75

Leaderboard Update (only on AC):
  redis.opsForValue().increment(
    "leaderboard:contest:" + contestId,
    userId.toString(),
    scoreToAdd  // 100 for full AC
  );

ZINCRBY is ATOMIC:
  → Even if 100 workers call simultaneously for different users
  → Each user's score correctly incremented
  → No race condition, no mutex needed
  → O(log N) — instant even with 10,000 users
```

**Important:** Score is INCREMENTED not SET. Agar user pehle WA (75 points) submit kiya, fir AC (100 points) — dono add hote hain. But upsert means only one real submission row per problem — so typically only one AC verdict per problem.

---

### FQ9: User code me `System.exit(0)` ya `os.kill()` call kare to kya hoga?

**Answer:**

```
System.exit(0) / exit() / os.kill():
  → Process exits with code 0
  → stdout jo abhi tak print hua tha → captured
  → Likely: 0 TC lines parsed → RE verdict
  OR: partial TC lines → WA verdict

fork() infinite loop:
  → prlimit NPROC=64 → 64 processes ke baad EAGAIN error
  → Parent process still runs normally
  → JVM unaffected

while(true) {} (CPU loop):
  → RLIMIT_CPU exceeded → SIGXCPU
  → Exit 139 → TLE verdict

malloc(10GB) (memory bomb):
  → RLIMIT_AS = 256MB virtual → mmap returns ENOMEM
  → Java: -Xmx256m → OOM → exit 137 → MLE verdict

open("/etc/passwd"):
  → bwrap mounts /etc read-only from host
  → File is accessible (contains no secrets)
  → But /home, ~/.env, etc. NOT mounted → invisible

socket() / network:
  → --unshare-all → new empty network namespace
  → connect() fails with ENETUNREACH
  → No internet, no localhost, nothing
```

---

### FQ10: Harness cache invalidate kab hota hai? Stale harness problem.

**Answer:**

```
Cache Write:
  snippet:{problemId}:{language} → harness string (TTL: 60 minutes)

Cache Evict (explicit):
  Admin edits problem → CacheService.evictProblem(problemId)
    → DEL snippet:{id}:JAVA
    → DEL snippet:{id}:CPP
    → DEL snippet:{id}:PYTHON
    → DEL snippet:{id}:JAVASCRIPT
    → DEL snippet:{id}:C

Automatic (TTL expiry):
  → After 60 minutes, key expires
  → Next compile → fresh fetch from DB

Stale harness risk:
  If admin edits problem directly in DB (bypassing API):
    → Cache still has old harness for up to 60 minutes
    → Solution: redis-cli FLUSHDB (nuclear option)
    → Or wait 60 minutes (TTL expires naturally)
```

---

### FQ11: `javac` compile error output kahan jaata hai? CE verdict kaise banta hai?

**Answer:**

```
compile step:
  javac Solution.java  (stdout + stderr captured)

if (exitCode != 0):
  stderr = "Main.java:5: error: ';' expected\n..."
  → return ExecutionResult(
        stdout = "",
        stderr = captured_stderr,
        exitCode = 1,
        timeTaken = 0,
        memory = 0
      )

parseOutput(result, isTestRun):
  if (result.isCompilationError()):
    return ParsedResult(CE, result.getStderr(), 0, 0, 0, "[]")
```

User ko dikhai deta: **"Compilation Error"** + actual compiler error message.

---

### FQ12: `javac` compile time kya karte ho? `javac` timeout kaise handle karte ho?

**Answer:**

```
compile step:
  javac Solution.java
  timeout: 30 seconds (hardcoded in DockerJudgeService)

run step:
  java -Xmx256m -cp . Solution
  timeout: problem.timeLimit + 5 seconds (wall-clock)

Rationale:
  - 30s compile is generous (even huge Java files compile <5s)
  - Runtime timeout = problem limit + 5s buffer for JVM startup
  - Wall-clock timeout catches JVM startup overhead too
```

---

### FQ13: `synchronized` / `Lock` use kyun nahi kiya kahin bhi? Race conditions kaise handle ki?

**Answer:**

```
Philosophy: "Lock-free where possible, atomic where needed"

┌─── State Changes ─────────────────────────────────────┐
│  PostgreSQL:                                           │
│   - UPDATE ... WHERE status IN ('PENDING','JUDGING')   │
│   - Row-level locks automatic (MVCC)                  │
│   - No explicit locks needed                          │
│                                                       │
│  Valkey/Redis:                                        │
│   - LMOVE (atomic)                                    │
│   - INCR (atomic)                                     │
│   - ZINCRBY (atomic)                                  │
│   - MULTI/EXEC for compound ops                       │
│   - Lua scripts for multi-step atomicity              │
└───────────────────────────────────────────────────────┘

┌─── Why No Java Locks? ───────────────────────────────┐
│   - Multiple JVMs (single VM) → Java locks useless    │
│   - Distributed systems need distributed coordination │
│   - Valkey/Postgres ARE the coordination layer        │
└──────────────────────────────────────────────────────┘

Rule: "Don't synchronize in Java what the DB/Valkey already handles atomically."
```

---

### FQ14: `replay dedup` kaise kaam karta hai? `clientCorrelationId` ka kya role?

**Answer:**

```
Client sends:
  POST /api/submissions
  { ..., "clientCorrelationId": "uuid-v4-from-frontend" }

Server:
  1. submissionRepo.findFirstByClientCorrelationIdAndUserId(id, userId)
  2. If found:
        return 409 CONFLICT
        { "error": "DUPLICATE_SUBMISSION", "existingId": 123 }
  3. Else:
        Create new Submission with clientCorrelationId = that UUID
```

**Why needed?** Frontend double-click, network retry, page refresh → duplicate submissions. Idempotency key prevents duplicate DB rows.

---

### FQ15: Proctoring `WebSocket` vs `SSE` kaise kaam karta hai? Kyu WebSocket?

**Answer:**

```
SSE (Server-Sent Events):
  → One-way (Server → Client)
  → Used for: Verdict push, leaderboard updates
  → HTTP/1.1 compatible, auto-reconnect, simple

WebSocket:
  → Two-way (Client ↔ Server)
  → Used for: Proctoring (camera frames, heartbeat, warnings)
  → Duel mode: real-time interaction

Protocol choice:
  SSE: Server pushes, client listens → perfect for verdicts
  WebSocket: Bi-directional → needed for proctoring handshake
```

**Proctoring Flow:**
```
1. Contest starts → browser requests /api/proctoring/ws/{sessionId}
2. WebSocket handshake + JWT auth (ticket in query param)
3. Server registers session in ProctoringSessionRegistry
3. Client sends camera frames (binary WebSocket frames)
4. Server → face detection → risk scoring
5. Server → client: warnings (FOCUS_LOST, MULTIPLE_FACES)
6. Contest ends → WebSocket close
```

---

*Ye poora flow production me chal raha hai —*
*single VM dono shared Valkey queue se jobs uthate hain.*
*0.4ms private network latency ke saath.*
---

### FQ16: Practice submissions ka flow kya hai? Contest se different kaise?

**Answer:**

```
Practice submissions use the SAME worker pool and SAME Valkey queue:
  POST /api/practice/run → enqueuePractice()
    → job.practice = true, job.testRun = false
    → LPUSH submission:queue

Worker same flow, different destinations:
  → DB: practice_submissions table (NOT submissions)
  → Leaderboard: SKIPPED (practice has no leaderboard)
  → Points: awardPointsIfFirstSolve() → user_problem_solved + total_points
  → GitHub: auto-push on AC
  → Streak: updateStreak() runs

Why separate table? Contest submissions need leaderboard tracking,
practice needs GitHub sync + simple point system. Clean separation.
```

---

### FQ17: GitHub auto-push kaise kaam karta hai? Kab push hota hai?

**Answer:**

```
Push TRIGGER: Practice submission verdict is AC + isReal (not testRun)
  → In finalizeAndNotify(), after DB update + leaderboard skip:
    githubService.pushSolution(userId, submissionId, problemTitle, language, code)

GitHubService flow:
  1. Check user.githubToken exists (OAuth connected?)
  2. ensureRepo("CodeCoder") — create if not exists
  3. Create file path: {problem-slug}/{lang}{n}Solution.{ext}
     Example: two-sum/java3Solution.java
  4. If file exists → update (SHA based)
  5. If not → create new file
  6. Commit message: "✅ {problemTitle} — {language} solution #{n}"
  7. Update practice_submissions.github_pushed = true

Auto-numbering: counts existing files in that folder, increments.
First push: java1Solution.java, second: java2Solution.java...
```

---

### FQ18: Streak system kaise kaam karta hai? Reset condition kya hai?

**Answer:**

```
StreakService.updateStreak(userId) called on EVERY real submission:

  1. Get user's lastActiveDate + currentStreak + maxStreak
  2. Compare with today:
     - First activity ever → streak = 1
     - Same day → NO CHANGE (return)
     - Yesterday (consecutive) → streak++
     - Gap > 1 day → streak = 1 (RESET)
  3. maxStreak = MAX(maxStreak, currentStreak)
  4. Save to DB: users.current_streak, users.max_streak, users.last_active_date

Stored in DB (not just in-memory), survives restarts.
Dashboard shows: "Current: Xd" + "Best: Yd"
Socials page: Streak achievement posters (10d/50d/111d/222d/555d)
```

---
