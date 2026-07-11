# Contest Submission Flow — Complete Architecture Guide

---

## Ek Line Me Samjho

> User code likhta hai → Submit dabata hai → Backend queue me daalta hai → Worker execute karta hai → Result wapas aata hai SSE se

---

## Poora Flow — Big Picture

```
USER (Browser)                  VM1/VM2 Backend                    Valkey          PostgreSQL
     │                                │                               │                 │
     │── POST /api/submissions ──────►│                               │                 │
     │                                │── Save PENDING row ──────────────────────────►│
     │                                │── LPUSH job ─────────────────►│                 │
     │◄─ 202 Accepted (subId) ────────│                               │                 │
     │                                │                               │                 │
     │  (user waits, SSE open)        │  [Worker background thread]   │                 │
     │                                │── LMOVE queue → processing ──►│                 │
     │                                │── Execute code (sandbox) ─────────              │
     │                                │── Write verdict ─────────────────────────────►│
     │                                │── ZINCRBY leaderboard ────────►│                 │
     │◄─ SSE "verdict" event ─────────│                               │                 │
     │                                │                               │                 │
```

---

## STEP 1 — User Submit Karta Hai

**File:** `SubmissionController.java`

```
User browser se POST request aati hai:
  URL:  POST /api/submissions
  Body: { problemId: 42, code: "...", language: "JAVA" }
  Header: Authorization: Bearer <jwt_token>
```

**Controller kya karta hai:**

```
1. JWT check → valid hai? (AuthTokenFilter already verify kar chuka)
2. Rate limit check:
     rateLimiter.allowSubmission(userId)
     → Agar zyada submissions kiye → 429 Too Many Requests return
3. Code validation:
   - Code null/blank nahi hona chahiye
   - Code length 50,000 characters se zyada nahi hona chahiye
3. SubmissionService.submitCodeAsync() call karta hai
4. 202 Accepted return karta hai TURANT
   (< 10ms response — user ko wait nahi karna)
```

> **Key Point:** Controller ka kaam sirf request accept karna hai. Execution woh nahi karta.

---

## STEP 2 — Service DB Me Save Karta Hai

**File:** `SubmissionService.java → submitCodeAsync()`

```
1. User aur Problem DB se fetch karo

2. Upsert logic (smart row management):
   ┌─────────────────────────────────────────────────────┐
   │  Latest submission fetch karo (us user ka, us problem ka) │
   │                                                     │
   │  Case A: Row hai aur verdict final hai (AC/WA/CE/RE)│
   │    → Same row reuse karo (update)                   │
   │                                                     │
   │  Case B: Row hai lekin PENDING/JUDGING (in-flight)  │
   │    → Nayi row banao (overwrite mat karo!)            │
   │                                                     │
   │  Case C: Koi row nahi                               │
   │    → Nayi row banao                                 │
   └─────────────────────────────────────────────────────┘

3. Submission row save karo with status = PENDING

4. Valkey cache evict karo:
      DEL submissions:user:{userId}
      DEL submission:user:problem:{userId}:{problemId}
```

---

## STEP 3 — Job Valkey Queue Me Push Hota Hai

**File:** `SubmissionService.java`

```java
SubmissionJob job = new SubmissionJob(
    submissionId,    // DB row ID
    userId,          // kaun submit kar raha hai
    problemId,       // kaunsa problem
    contestId,       // kaunse contest ka
    code,            // user ka code
    language,        // JAVA / CPP / PYTHON / etc
    timeLimit,       // seconds
    memoryLimit,     // MB
    isTestRun=false, // real submission
    duelId=null,     // contest submission, not duel
    proctoringSessionId  // if proctored contest
);

String jobJson = objectMapper.writeValueAsString(job);
redis.opsForList().leftPush("submission:queue", jobJson);
```

**Valkey queue structure:**
```
submission:queue → [job3_json, job2_json, job1_json]
                      ↑ new jobs    ↑ old jobs
                    (LPUSH = left)  (LMOVE = right se uthao)
```

---

## STEP 4 — Worker Job Uthata Hai

**File:** `SubmissionWorkerPool.java → workerLoop()`

Do VMs pe total **8 workers** chal rahe hain (VM1=2, VM2=6):

```
┌─────────────────────────────────────────────────┐
│  Worker Thread (judge-worker-1, 2, 3 ...)       │
│                                                 │
│  Loop forever:                                  │
│    LMOVE submission:queue                        │
│         → submission:processing:<host>:<idx>     │
│         (3 second blocking wait)                 │
│                                                 │
│    If job mila:                                  │
│      → processJob(job)                           │
│    If timeout (3s no job):                       │
│      → loop again (wait karo)                    │
└─────────────────────────────────────────────────┘
```

**LMOVE kyun?** (BRPOP se better)
```
BRPOP  → queue se hata do (job lost if worker crashes)
LMOVE  → queue se hata ke processing list me daalo
         → crash hone pe bhi job safe hai!
```

---

## STEP 5 — Job Process Hota Hai

**File:** `SubmissionWorkerPool.java → processJob()`

```
Step 5a: DB me status update karo → JUDGING

Step 5b: Harness fetch karo (CacheService)
  Check Valkey: snippet:{problemId}:{language}
    HIT → harness milgi (fast, ~0.5ms)
    MISS → PostgreSQL se fetch, Valkey me cache karo

Step 5c: User code inject karo harness me
  Harness has markers:
    // USER_CODE_START
    [user ka code yahan aayega]
    // USER_CODE_END

Step 5d: Execute karo → DockerJudgeService.execute()
  (details Step 6 me)

Step 5e: Output parse karo
  TC:1:PASS  → test 1 pass
  TC:2:FAIL:input=...:expected=...:got=...  → test 2 fail
  TC:3:PASS:hidden  → hidden test (user ko nahi dikhega)

Step 5f: finalizeAndNotify() call karo
```

---

## STEP 6 — Sandbox Me Code Execute Hota Hai

**Files:** `DockerJudgeService.java` + `SandboxRunner.java`

```
Language ke hisaab se:

JAVA:
  1. javac Solution.java  (compile, 30s limit)
  2. java -Xmx256m Solution  (run)

C++:
  1. g++ -O2 -o solution solution.cpp  (compile)
  2. ./solution  (run)

Python:
  1. python3 solution.py  (directly run, no compile)

JavaScript:
  1. node --max-old-space-size=256 solution.js  (run)

C:
  1. gcc -O2 -o solution solution.c  (compile)
  2. ./solution  (run)
```

**Sandbox security (2 layers):**
```
┌─── Layer 1: bwrap (bubblewrap) ─────────────────────┐
│  • Naya network namespace (internet nahi)            │
│  • Naya PID namespace (host processes nahi dikh rahe)│
│  • Filesystem readonly (sirf /tmp write kar sakte)   │
│  • UID = 65534 (nobody — no privileges)             │
│  • --die-with-parent (crash hone pe sab kuch band)   │
└─────────────────────────────────────────────────────┘
                       +
┌─── Layer 2: prlimit (limits) ───────────────────────┐
│  • Max memory (virtual): 256MB + runtime overhead   │
│  • Max CPU: problem time limit + 1 second           │
│  • Max processes: 64 (fork bomb prevent)            │
│  • Max file size: 16MB                              │
│  • Max open files: 64                               │
└─────────────────────────────────────────────────────┘
```

**Process management:**
```
1. ProcessBuilder se process start karo
2. stdin close karo immediately (Scanner hang prevent)
3. stdout + stderr reading → daemon threads (parallel)
4. waitFor(timeLimit + 5 seconds)
5. Agar time se nahi aaya → destroyForcibly() → TLE
6. Exit code 137 = SIGKILL = Memory Limit Exceeded
7. Exit code 152 = SIGXCPU = CPU Limit Exceeded
```

---

## STEP 7 — Result Save + Notifications

**File:** `SubmissionWorkerPool.java → finalizeAndNotify()`

```
┌─── 1. Database Update ──────────────────────────────┐
│  submission.status = AC / WA / CE / RE / TLE / MLE  │
│  submission.score = (passed/total) * 100             │
│  submission.testCaseDetails = JSON array             │
│  submission.timeConsumed = execution ms              │
└─────────────────────────────────────────────────────┘

┌─── 2. Leaderboard Update (only AC, non-test) ───────┐
│  ZINCRBY leaderboard:contest:{contestId}             │
│          score                                       │
│          {userId}                                    │
│  O(log N) — instant even with 500 users              │
└─────────────────────────────────────────────────────┘

┌─── 3. Cache Evict ──────────────────────────────────┐
│  DEL submissions:user:{userId}                       │
│  DEL submission:status:{submissionId}                │
│  DEL submission:user:problem:{userId}:{problemId}    │
└─────────────────────────────────────────────────────┘

┌─── 4. SSE Push ─────────────────────────────────────┐
│  sseRegistry.sendVerdict(userId, verdictEvent)       │
│  → Browser ko real-time notification                 │
└─────────────────────────────────────────────────────┘

┌─── 5. Job Cleanup ──────────────────────────────────┐
│  LREM submission:processing:<host>:<idx> 0 jobJson   │
│  → Job ACK — processing list se hata do              │
└─────────────────────────────────────────────────────┘
```

---

## STEP 8 — User Ko Result Milta Hai

**File:** `SseEmitterRegistry.java` + Frontend polling

```
Primary Path — SSE (Server-Sent Events):
┌──────────────────────────────────────────────────────┐
│  Frontend:                                           │
│  1. POST /api/submissions/sse-ticket → ticket mila   │
│  2. new EventSource("/submissions/stream?ticket=...") │
│  3. Event listener: es.addEventListener("verdict"...)│
│                                                      │
│  Backend:                                            │
│  1. Ticket validate (Valkey GETDEL — ek baar use)    │
│  2. userId ke liye SseEmitter register karo          │
│  3. Jab verdict aaye → sendVerdict(userId, data)     │
│  4. Browser ko event push hota hai (<1s)             │
└──────────────────────────────────────────────────────┘

Fallback Path — Polling (agar SSE drop ho):
┌──────────────────────────────────────────────────────┐
│  Frontend:                                           │
│  pollVerdict() function runs every 3-5 seconds:      │
│  GET /submissions/{submissionId}/status              │
│                                                      │
│  Backend:                                            │
│  1. Check Valkey cache (2s TTL)                      │
│  2. Miss → PostgreSQL se fetch                       │
│  3. Only cache final states (not PENDING/JUDGING)    │
└──────────────────────────────────────────────────────┘
```

---

## "Run" vs "Submit" — Kya Fark Hai?

| | Run (Test) | Submit (Real) |
|---|---|---|
| Test cases | Sirf 2 visible | Saare (visible + hidden) |
| DB row | `isTestRun = true` | Real submission |
| Leaderboard | Update nahi hota | AC pe update hota |
| Score | Count nahi hota | Count hota hai |
| Limit | 10 per problem | Rate limited per user |
| Purpose | Code check karo | Official result |

---

## Kya Hoga Agar Worker Crash Ho Jaye?

```
┌─── Problem ────────────────────────────────────────┐
│  Worker process aa raha tha                        │
│  Job processing list me tha                        │
│  JVM crash ho gaya                                 │
│  Job lost? ❌                                       │
└────────────────────────────────────────────────────┘

┌─── Solution: Janitor ─────────────────────────────┐
│  @Scheduled every 60 seconds:                      │
│                                                    │
│  1. SMEMBERS submission:processing:registry         │
│     (saare VMs ke processing keys)                 │
│                                                    │
│  2. Har key check karo:                            │
│     - Job DB me check karo (still PENDING?)        │
│     - Age check karo (5 minute se zyada purana?)   │
│                                                    │
│  3. Agar stuck hai:                                │
│     LREM → processing list se hata do              │
│     LPUSH → submission:queue me wapas daalo        │
│     Log: "Reclaimed stuck job"                     │
│                                                    │
│  Result: Koi bhi job kabhi permanently lost nahi  │
└────────────────────────────────────────────────────┘
```

---

## Complete Flow Diagram — Ek Nazar Me

```
[USER BROWSER]
      │
      │  POST /api/submissions
      │  {code, problemId, language}
      ▼
[SubmissionController]
  ├── JWT validate ✓
  ├── Rate limit check ✓
  └── Call SubmissionService
               │
               ▼
       [SubmissionService]
         ├── Save to PostgreSQL (PENDING)
         ├── LPUSH → Valkey queue
         └── Return submissionId
               │
               │  202 Accepted
               │  ◄─────────────────────────── User gets response
               │
               │  (Background - async)
               ▼
       [SubmissionWorkerPool]
         ├── LMOVE queue → processing (atomic)
         ├── DB → JUDGING
         ├── Fetch harness (Valkey cache)
         ├── Inject user code
         └── DockerJudgeService.execute()
                     │
                     ▼
             [SandboxRunner]
               ├── bwrap (namespace isolation)
               ├── prlimit (resource limits)
               └── ProcessBuilder (run code)
                     │
                     ▼  stdout:
                    TC:1:PASS
                    TC:2:FAIL:...
                    TC:3:PASS:hidden
                     │
                     ▼
       [finalizeAndNotify]
         ├── DB update (verdict + score)
         ├── ZINCRBY leaderboard (if AC)
         ├── Cache evict
         ├── SSE push to user
         └── LREM (job ACK)
               │
               │  SSE event: "verdict"
               ▼
       [USER BROWSER] → shows result
```

---

## Possible Verdicts

| Verdict | Matlab | Kab aata hai |
|---------|--------|-------------|
| **AC** | All Correct | Saare test cases pass |
| **WA** | Wrong Answer | Kuch test cases fail |
| **CE** | Compile Error | Code compile nahi hua |
| **RE** | Runtime Error | Code crash ho gaya |
| **TLE** | Time Limit Exceeded | Time limit ke andar nahi aaya |
| **MLE** | Memory Limit Exceeded | Memory limit exceed ki |
| **PENDING** | Aaya nahi abhi | Queue me wait kar raha |
| **JUDGING** | Execute ho raha hai | Worker utha chuka hai |

---

## Follow-Up Questions & Answers (12 Deep-Dive)

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
5. User ko zyada generous limit milega (per-VM instead of global)
   But NEVER completely unprotected
```

**Follow-up to follow-up:** "Global limit 3/10s hai, Valkey down hone pe 3/10s PER VM ho jayega — matlab 6/10s total across 2 VMs. Acceptable tradeoff — better than zero protection."

---

### FQ3: Agar dono VMs ka worker ek hi job uthane ki koshish kare to?

**Answer:**

**Ye possible hi nahi hai** kyunki LMOVE atomic hai:

```
LMOVE = single Redis command = atomic

Thread A (VM1): LMOVE submission:queue → processing:vm1:0
Thread B (VM2): LMOVE submission:queue → processing:vm2:3

Redis executes commands sequentially (single-threaded):
  → Thread A gets job1
  → Thread B gets job2 (next item)
  → NEVER same job to two workers
```

Redis/Valkey is single-threaded for command execution. Two `LMOVE` commands on the same list are serialized at the Valkey level. No locking needed on the application side.

---

### FQ4: SSE connection cross-VM delivery problem — kaise solve kiya?

**Answer:**

```
Problem:
  User ka SSE stream VM1 pe open hai
  User ka submission VM2 ke worker ne process kiya
  VM2 ka worker SSE push karta hai:
    sseRegistry.sendVerdict(userId=42, verdict)
  BUT: VM2 ke SseEmitterRegistry me userId=42 ka emitter nahi hai!
  → Push silently fails on VM2

Solution: Frontend polling fallback
──────────────────────────────────
  Frontend code (ProblemSolve.jsx):
    pollVerdict(submissionId) runs every 3s:
      GET /submissions/{id}/status
      → reads from shared PostgreSQL (both VMs same DB)
      → VM1/VM2 don't matter, DB is shared

Verified behavior:
  Best case (same VM processed): <1 second (SSE instant)
  Worst case (cross-VM): 3-5 seconds (polling catches it)

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

### FQ11: Dual-VM setup me agar VM2 down ho to kya hoga?

**Answer:**

```
┌─── VM2 Down — Impact Analysis ─────────────────┐
│                                                     │
│  Contest Submissions:                               │
│    VM2 ke 6 workers band → queue drain slower       │
│    VM1 ke 2 workers still drain the queue ✓         │
│    Throughput: 96 jobs/min → 24 jobs/min (4x slower)│
│    NO DATA LOSS — jobs just wait longer             │
│                                                     │
│  Practice Runs:                                     │
│    nginx: proxy_next_upstream error timeout 502 503 │
│    → VM2 respond nahi karta                         │
│    → nginx automatically retries on VM1 ✓           │
│    → User ko koi error nahi dikhta!                 │
│    → Just slightly slower (VM1 handles all load)    │
│                                                     │
│  SSE/Compiler/API:                                  │
│    Already VM1-only → zero impact ✓                 │
└─────────────────────────────────────────────────────┘
```

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
│   - Multiple JVMs (VM1 + VM2) → Java locks useless    │
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
*VM1 + VM2 dono shared Valkey queue se jobs uthate hain.*
*0.4ms private network latency ke saath.*