# CodeCombat Contest Submission Engine — Deep Dive

---

## Overview — Ek Nazar Me

Jab user contest me koi problem submit karta hai, to backend us code ko ek sandbox environment me execute karta hai, output verify karta hai, aur real-time verdict browser ko bhejta hai. Poora flow async hai — user ko turant response milta hai (202 Accepted), background me execution hota hai, aur SSE ke through verdict push hota hai.

**Architecture:** Single VM running Spring Boot backend. 2 judge worker threads continuously drain a shared Valkey (Redis-compatible) queue. PostgreSQL stores all submission data. bwrap + prlimit provide secure sandboxed execution.

```
USER BROWSER                         BACKEND (VM)                  VALKEY              POSTGRESQL
     │                                    │                          │                      │
     │── POST /api/submissions ──────────►│                          │                      │
     │  {problemId, code, language}       │── INSERT PENDING ───────────────────────────►│
     │                                    │── LPUSH job ────────────►│                    │
     │◄─ 202 {submissionId} ──────────────│                          │                      │
     │                                    │                          │                      │
     │  [User waits — SSE open]           │  [Worker background]     │                      │
     │                                    │── LMOVE claim job ──────►│                    │
     │                                    │── UPDATE → JUDGING ───────────────────────────►│
     │                                    │── Fetch harness (cache) ─┤                    │
     │                                    │── Execute (bwrap sandbox)                      │
     │                                    │── Parse TC: lines                             │
     │                                    │── UPDATE verdict ─────────────────────────────►│
     │                                    │── ZINCRBY leaderboard ──►│                      │
     │◄─ SSE "verdict" ───────────────────│                          │                      │
```

---

## Step 1: User Submit Karta Hai — Controller Layer

**File:** `SubmissionController.java`

### Frontend se Request

User code likhkar "Submit" button dabata hai. Frontend POST request bhejta hai:

```
POST /api/submissions
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "problemId": 42,
  "code": "class Solution { public int[] twoSum(...) { ... } }",
  "language": "JAVA"
}
```

### Controller Kya Karta Hai — Step by Step

**1. Authentication:** JWT token `AuthTokenFilter` already verify kar chuka hota hai. Har request ke saath token aata hai, filter usse validate karta hai aur `SecurityContext` me user set kar deta hai. Agar token invalid/expired → 401 Unauthorized.

**2. Rate Limiting:** Har user ke liye ek sliding window counter maintain hota hai:
```
Key: sub:rl:{userId}
Logic: INCR → agar count == 1 to EXPIRE 10s
       Agar count > 5 → 429 Too Many Requests
```
Ye Valkey me stored hota hai — shared counter across all requests. Fallback: Valkey down hone pe ConcurrentHashMap-based local counter per JVM.

**3. Basic Validation:**
- `problemId` null nahi hona chahiye
- `code` blank nahi hona chahiye  
- `code.length < 50000` characters
- `language` supported list me hona chahiye (JAVA, CPP, PYTHON, JAVASCRIPT, C)

**4. Delegate to Service:** `SubmissionService.submitCodeAsync()` call karta hai. Controller ka kaam yahi tak — bas validate karo aur aage bhejo.

**5. Response:** Turant `202 Accepted` return karta hai with `submissionId`. Response time: <10ms. User ko turant pata chal jata hai ki submission accept ho gaya, queue me hai.

### Theory: Controller Ka Role

Controller ka kaam sirf "request accept karna + validate karna + delegate karna" hai. Wo execution nahi karta. Ye architecture pattern "Async Request Processing" kehlata hai — user ko fast response milta hai, heavy work background me hota hai.

---

## Step 2: Service Layer — DB Me Save + Queue Me Push

**File:** `SubmissionService.java` → `submitCodeAsync()`

### Row Management (Smart Upsert)

Har user ke liye per-problem ek hi submission row rakhne ki koshish hoti hai. Ye DB growth control karta hai:

```java
// Latest submission fetch karo (is user, is problem)
Submission latest = submissionRepo.findTopByUserIdAndProblemIdOrderBySubmittedAtDesc(userId, problemId);

if (latest == null || latest.isTestRun()) {
    // Case A: Koi row nahi hai, ya sirf test run rows hain
    // → NAYI row banao (PENDING status)
    submission = new Submission();
} else if (isFinalVerdict(latest.getStatus())) {
    // Case B: Row hai aur uska verdict final hai (AC/WA/CE/RE/TLE/MLE)
    // → USSI row ko REUSE karo (update code, reset to PENDING)
    // Benefit: DB me row count nahi badhta
    submission = latest;
    submission.setCode(code);
    submission.setLanguage(lang);
} else {
    // Case C: Row hai lekin PENDING/JUDGING (abhi process ho rahi hai)
    // → NAYI row banao (in-flight row ko disturb mat karo)
    // Karan: agar hum update kar de to current worker ka judgment corrupt ho jayega
    submission = new Submission();
}
```

### Theory: Kyun Upsert?

Agar har submit pe nayi row banaye:
- 1 user × 1 problem × 20 submits = 20 rows
- 500 users × 5 problems × 20 submits = 50,000 rows per contest
- 10 contests = 500,000 rows → query slow

Upsert ke saath:
- 1 user × 1 problem = MAX 1 row (reused)
- 500 users × 5 problems = 2,500 rows per contest
- DB lean and fast

### Save + Cache Evict

```java
submission.setStatus(PENDING);
submission.setSubmittedAt(TimeUtil.now());
submissionRepo.save(submission);

// Cache invalidate — next dashboard load will fetch fresh data
redis.delete("submissions:user:" + userId);
redis.delete("submission:user:problem:" + userId + ":" + problemId);
```

Cache delete isliye kyunki user ke dashboard me "Recent Submissions" cached hoti hai. Nayi submission add hone ke baad stale cache dikhegi, isliye turant evict karte hain.

---

## Step 3: Job Valkey Queue Me Push

**File:** `SubmissionService.java`

### SubmissionJob — Queue Item

```java
SubmissionJob job = new SubmissionJob();
job.setSubmissionId(submission.getId());   // DB row ID (verdict wahi save hoga)
job.setUserId(userId);                     // kaunsa user
job.setProblemId(problemId);               // kaunsa problem
job.setContestId(contestId);               // kaunse contest ka (leaderboard ke liye)
job.setCode(code);                         // user ka actual code
job.setLanguage(language);                 // JAVA/CPP/PYTHON/JAVASCRIPT/C
job.setTimeLimit(problem.getTimeLimit());  // seconds me time limit
job.setMemoryLimit(problem.getMemoryLimit()); // MB me memory limit
job.setTestRun(false);                     // REAL submission hai, test run nahi
job.setDuelId(null);                       // contest submission hai, duel nahi
```

### Push to Queue

```java
String jobJson = objectMapper.writeValueAsString(job);
redis.opsForList().leftPush("submission:queue", jobJson);
```

### Queue Structure

```
submission:queue = [job3_json, job2_json, job1_json]
                     ↑ LPUSH (left)  ↑ LMOVE (right side se uthate hain)
```

`leftPush` se naye jobs left side pe add hote hain. Worker `LMOVE` se RIGHT side se uthata hai — means **FIFO** (First In First Out). Jo pehle aaya, wo pehle process hoga.

### Theory: Queue Kyun?

Direct execution na karke queue use karne ke 3 fayde:
1. **Back-pressure handling:** Agar 50 users ek saath submit kare, to queue buffer karta hai. Workers apni speed se process karte hain, server overload nahi hota.
2. **Crash safety:** Job Valkey me stored hai. Agar worker crash ho jaye, job queue me wapas aa jati hai (LMOVE + Janitor se).
    3. **Priority:** Future me high-priority submissions ko pehle process kar sakte hain — queue architecture allows this.

---

## Step 4: Worker Job Uthata Hai

**File:** `SubmissionWorkerPool.java` → `workerLoop()`

### Worker Threads

2 worker threads continuously run karte hain (configurable via `JUDGE_WORKERS=2`):

```java
@PostConstruct
public void init() {
    for (int i = 0; i < workerCount; i++) {
        Thread worker = new Thread(this::workerLoop, "judge-worker-" + (i + 1));
        worker.setDaemon(true);
        worker.start();
    }
}
```

### Worker Loop

```java
void workerLoop() {
    while (running) {
        try {
            // LMOVE: atomically move one job from queue → processing list
            String jobJson = redis.opsForList()
                .rightPopAndLeftPush("submission:queue", 
                    "submission:processing:" + hostname + ":" + workerIdx, 
                    3, TimeUnit.SECONDS);
            
            if (jobJson != null) {
                SubmissionJob job = objectMapper.readValue(jobJson, SubmissionJob.class);
                processJob(job);
                // After successful processing, remove from processing list
                redis.opsForList().remove("submission:processing:...", 0, jobJson);
            }
            // If null → 3 second timeout with no job → loop again
        } catch (Exception e) {
            log.error("Worker error: {}", e.getMessage());
        }
    }
}
```

### LMOVE — Safer than BRPOP

```
BRPOP: queue se POP karta hai → job permanently removed
       Agar worker crash → JOB LOST FOREVER

LMOVE: queue se MOVE karta hai → job processing list me rehta hai
       Atomic operation (single Valkey command)
       Agar worker crash → job processing list me safe hai
       Janitor detect karega → wapas queue me daal dega
       ZERO JOB LOSS
```

### Theory: Atomic LMOVE

Valkey single-threaded hai for command execution. Do workers same queue se LMOVE kare to Valkey serialize karta hai — kabhi ek job do workers ko nahi milegi. Bina kisi Java lock ke perfect synchronization.

### Claim Timestamp

Job claim karte hi Valkey me timestamp store hota hai:
```java
redis.opsForValue().set("submission:claim:" + job.getSubmissionId(), 
    String.valueOf(System.currentTimeMillis()));
```

Ye timestamp Janitor use karta hai stuck jobs detect karne ke liye. Agar koi job 5 minute se zyada processing list me padi hai, wo "stuck" maani jayegi.

---

## Step 5: Job Process Hota Hai

**File:** `SubmissionWorkerPool.java` → `processJob()`

### 5a: DB Status → JUDGING

```java
submissionRepo.updateStatus(submissionId, JUDGING);
```

User ke dashboard pe ab "JUDGING" status dikhega instead of "PENDING".

### 5b: Harness Fetch (Cache-Aside Pattern)

```java
String harness = cacheService.getSnippetHarness(problemId, language);
```

```
Valkey Key: snippet:{problemId}:{language}
Example: snippet:42:JAVA → "import java.util.*;\n// USER_CODE_START\n..."

CACHE HIT  (~0.5ms Valkey) → harness turant milti hai
CACHE MISS → PostgreSQL se fetch, Valkey me cache (TTL: 60 min)
```

Harness cache 60 minute TTL ke saath Valkey me stored rehti hai. Admin problem edit kare to cache evict hoti hai.

### Harness Structure

Harness ek complete runnable Java/C++/Python/JS/C file hai jisme:
- Test cases hardcoded hain (no stdin reading)
- `// USER_CODE_START` aur `// USER_CODE_END` markers hain
- User ka code in dono markers ke beech inject hota hai
- Harness ka driver code OUTSIDE markers hai (user nahi dekh sakta)

```java
import java.util.*;

// USER_CODE_START
class Solution {
    public int[] twoSum(int[] nums, int target) {
        // ← USER KA CODE YAHAN INJECT HOGA
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int target, int[] expected, int tc, boolean hidden) {
        int[] got = new Solution().twoSum(nums, target);
        // compare and print TC: lines...
    }
    public static void main(String[] a) {
        try { test(new int[]{2,7,11,15}, 9, new int[]{0,1}, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:...:got=ERR"); }
        // ... more test cases
    }
}
```

### 5c: User Code Inject Karna

```java
String executableCode = injectUserCode(harness, userCode, language);
```

`USER_CODE_START` aur `USER_CODE_END` markers ke beech ka content replace hota hai user ke code se. Python ke liye `# USER_CODE_START` / `# USER_CODE_END` markers hain.

### 5d: Execute — Sandbox

```java
ExecutionResult result = judgeService.execute(executableCode, language, timeLimit, memoryLimit);
```

---

## Step 6: Sandbox Execution — Deep Dive

**Files:** `DockerJudgeService.java` + `SandboxRunner.java`  
(Note: "Docker" naam misleading hai — actually bwrap use hota hai, Docker nahi)

### Language-Specific Execution

```
JAVA:
  Step 1: javac Solution.java     (compile, 30 second timeout)
  Step 2: java -Xmx256m Main      (execute inside bwrap)

CPP:
  Step 1: g++ -O2 -o binary solution.cpp   (compile)
  Step 2: ./binary                          (execute)

PYTHON:
  python3 solution.py              (directly run, no compile step)

JAVASCRIPT:
  node --max-old-space-size=256 solution.js  (run)

C:
  Step 1: gcc -O2 -o binary solution.c     (compile)
  Step 2: ./binary                          (execute)
```

Java/C++/C me COMPILE time + RUN time alag-alag measure hote hain. Compilation ka time execution time me include nahi hota — sirf run step ka time count hota hai.

### Execution Timing

```java
Process process = pb.start();  // bwrap process start
// stdin write (prevent Scanner hang)
process.getOutputStream().write(stdin.getBytes());
process.getOutputStream().close();

long startMs = System.currentTimeMillis();  // ← TIMER STARTS AFTER PROCESS START
boolean finished = process.waitFor(timeLimit + 5, TimeUnit.SECONDS);
long elapsed = System.currentTimeMillis() - startMs;  // ← TIMER STOPS
```

**Important (July 2026 fix):** `startMs` AB `pb.start()` ke BAAD liya jata hai. Pehle pehle liya jata tha — to bwrap startup ka 1-5s ka variable overhead bhi execution time me add ho jata tha. Ab sirf actual code execution time count hota hai.

### Sandbox Security — 2 Layers

**Layer 1: bwrap (bubblewrap)** — Linux namespace isolation:

```
bwrap \
  --unshare-all \              # Saare namespaces naye (PID, NET, IPC, UTS, USER)
  --uid 65534 --gid 65534 \    # nobody user (zero privileges)  
  --ro-bind /usr /usr \        # System libraries read-only
  --ro-bind /lib /lib \
  --ro-bind /etc /etc \
  --tmpfs /tmp \               # Temporary write space (per-job, volatile)
  --proc /proc --dev /dev \    # Fresh /proc, minimal /dev
  --die-with-parent \          # Agar parent (JVM) mare to sab kuch kill
  --cap-drop ALL \             # Saare Linux capabilities drop
  --clearenv \                 # Environment variables clear
  --hostname sandbox \
  -- <user command>
```

Isolation kya karta hai:
- **Process isolation:** Host ke processes nahi dikhte. `ps aux` khaali aayega
- **Network isolation:** Koi network access nahi. `curl google.com` → network unreachable
- **Filesystem isolation:** Sirf `/usr`, `/lib`, `/etc` read-only mount hain. `/home`, `/root` invisible
- **User isolation:** UID 65534 = nobody. Root escalation impossible
- **Auto-cleanup:** JVM crash → `--die-with-parent` → kernel sab kuch kill karta hai

**Layer 2: prlimit** — Resource limits:

```
prlimit \
  --as=268435456 \       # Virtual memory: 256MB
  --cpu={timeLimit+1} \  # CPU seconds: problem limit + 1
  --nproc=64 \           # Max processes: 64 (fork bomb prevent)
  --fsize=16777216 \     # Max file size: 16MB
  --nofile=64 \          # Max open files: 64
  -- <command>
```

### TLE Detection — 3 Layers

Code time limit se zyada time le raha hai to kaise pakadte hain:

```
Layer 1 (Kernel): prlimit RLIMIT_CPU
  → CPU seconds exceed hone pe kernel SIGXCPU bhejta hai
  → Process exit code: 152 (128 + 24)
  → Perfect for CPU loops: while(true) {}

Layer 2 (JVM): Process.waitFor(timeLimit + 5, SECONDS)
  → Wall-clock timeout
  → Agar process 5 extra seconds me bhi exit nahi hua
  → destroyForcibly() → SIGKILL
  → Catches I/O hangs, sleep(), network waits

Layer 3 (Safety net): Wall-clock elapsed check
  → Process exit code 0 bhi aaya to:
  → elapsed > timeLimit * 1000ms → STILL TLE
  → Edge cases ke liye
```

### MLE Detection

Memory limit exceed hone pe:
```
RLIMIT_AS (virtual memory) exceeded
  → mmap() returns ENOMEM
  → Java: OutOfMemoryError → exit 137 (128 + 9 = SIGKILL from OOM killer)
  → C/C++: malloc returns NULL → likely segfault → exit 139 (SIGSEGV)
```

### Process Cleanup — Double Kill

```
destroyForcibly()         → SIGKILL
process.waitFor(2, SEC)   → confirm death
agar STILL alive:
  process.descendants().forEach(Process::destroyForcibly)
  → saare child processes bhi kill
bwrap --die-with-parent:
  → parent kill = entire namespace destroyed by kernel
  → 100% guaranteed cleanup
```

---

## Step 7: Output Parse Karna

**File:** `SubmissionWorkerPool.java` → `parseOutput()`

### Harness Output Format

Harness stdout pe har test case ke liye exactly ek line print karta hai:

```
TC:1:PASS                                              ← pass, visible test
TC:2:FAIL:input=[4,2,0,3]:expected=4:got=0            ← fail, visible (user ko input/expected/got dikhega)
TC:3:PASS:hidden                                       ← pass, hidden test (user sirf PASS dekhega)
TC:4:FAIL:hidden                                       ← fail, hidden test (user sirf FAIL dekhega, no debug info)
```

### Parsing Logic

```java
for (String line : stdout.split("\n")) {
    if (!line.startsWith("TC:")) continue;  // ignore non-TC lines
    
    // Parse: TC:1:PASS  or  TC:2:FAIL:input=...:expected=...:got=...
    String[] parts = line.split(":");
    int tcNum = Integer.parseInt(parts[1]);
    boolean passed = parts[2].equals("PASS");
    boolean hidden = line.contains(":hidden");
    
    // For failed visible tests, extract debug info
    String input = null, expected = null, got = null;
    if (!passed && !hidden) {
        // Parse input=, expected=, got= from the line
    }
}
```

### try-catch in Harness (July 2026 Feature)

Harness me har `test()` call `try-catch` me wrapped hoti hai:

```java
try { test(arr, expected, 1, false); }
catch (Exception e) { System.out.println("TC:1:FAIL:input=...:got=ERR"); }
```

**Kyun zaroori hai:** Pehle bina try-catch ke, agar test case 3 me user code exception throw karta tha (NPE, ArrayIndexOutOfBounds), to poori process die ho jati thi. Test cases 4-6 kabhi execute hi nahi hote the. Result me **WA ka execution time AC se KAM** dikhta tha — jo misleading tha.

Ab try-catch se: crash hone pe bhi FAIL print hota hai, process continue karta hai, saare test cases execute hote hain. Execution time accurate rehta hai.

### Verdict Determination

```java
if (result.isCompilationError()) → CE (stderr user ko dikhega)
if (result.isTimeLimitExceeded()) → TLE
if (result.isMemoryLimitExceeded()) → MLE
if (lines.isEmpty() && exitCode != 0) → RE (runtime error, stderr dikhega)
if (passed == total) → AC (Accepted!)
else → WA (Wrong Answer)

score = (passed / total) * 100
```

---

## Step 8: finalizeAndNotify() — Sab Kuch Final

**File:** `SubmissionWorkerPool.java` → `finalizeAndNotify()`

Ye method verdict ko har jagah save + push karta hai. Har operation try-catch me wrapped hai — ek fail hone se doosre affect nahi hote.

### 8a: Database Update

```sql
UPDATE submissions 
SET status = ?, score = ?, error_message = ?, 
    test_cases_passed = ?, total_test_cases = ?,
    time_consumed = ?, test_case_details = ?
WHERE id = ? 
  AND status IN ('PENDING', 'JUDGING')  -- idempotent: sirf in-flight rows update
```

`AND status IN ('PENDING','JUDGING')` clause ensure karta hai ki agar kisi reason se worker ne dobara same job process kiya (janitor reclaim ke baad), to already-finalized row dobara update nahi hogi.

### 8b: Leaderboard Update (Contest Only)

```java
if (!job.isTestRun() && job.getContestId() != null && updated > 0) {
    // Per-problem score tracking via Valkey
    String problemScoreKey = "contest:score:" + contestId + ":" + userId + ":" + problemId;
    String prevStr = redis.opsForValue().get(problemScoreKey);
    int prevScore = (prevStr != null) ? Integer.parseInt(prevStr) : 0;
    int delta = score - prevScore;
    
    if (delta != 0) {
        redis.opsForValue().set(problemScoreKey, String.valueOf(score), Duration.ofDays(30));
        leaderboard.updateScore(contestId, userId, delta);
    }
}
```

**Delta Approach:** Pehle WA 50 score tha, ab AC 100 score hai → delta = +50 hi leaderboard me add hoga. Full score +100 nahi — warna dobara count ho jata.

**Atomic:** `ZINCRBY` Valkey me atomic hai — ek saath 100 workers call kare to bhi har user ka score sahi increment hoga.

### 8c: Cache Eviction

```java
redis.delete("submissions:user:" + userId);
redis.delete("submission:status:" + submissionId);
redis.delete("submission:user:problem:" + userId + ":" + problemId);
```

Teeno caches clear karte hain taki next dashboard load fresh data dikhaye. Agar eviction fail bhi ho jaye, TTL ke hisaab se cache apne aap expire ho jayegi.

### 8d: SSE Push

```java
VerdictEvent event = new VerdictEvent(
    submissionId, status.name(), passed, total, score,
    timeMs, errorMessage, details,
    job.isTestRun(), job.isPractice(), practiceAwarded, practiceSolved
);
sseRegistry.sendVerdict(userId, event);
```

`SseEmitterRegistry` me har SSE-connected user ka `SseEmitter` stored hai. `sendVerdict` us emitter pe event push karta hai → browser ko <1 second me verdict mil jata hai.

### 8e: Job Cleanup

```java
redis.opsForList().remove(processingKey, 0, jobJson);
```

Processing list se job remove karte hain — "acknowledge" ki job successfully complete ho gayi.

---

## Step 9: User Ko Result Milta Hai

### Primary Path: SSE (Server-Sent Events)

```
Frontend:
  1. POST /api/submissions/sse-ticket → {ticket: "uuid"}
  2. const es = new EventSource("/submissions/stream?ticket=...")
  3. es.addEventListener("verdict", (e) => {
       const verdict = JSON.parse(e.data);
       showResult(verdict);  // AC/WA/CE etc
     })

Backend:
  1. Ticket validate → Valkey GETDEL (single-use, consumed on read)
  2. SseEmitter register for userId
  3. On verdict → emitter.send(verdictJson)
  4. Connection drops → auto-cleanup

SSE auto-reconnect (July 2026):
  onerror → close dead EventSource → delay 3s → new ticket → reconnect
```

### Fallback Path: Polling

```
Frontend: pollVerdict(submissionId) every 2-3 seconds
  GET /api/submissions/{id}/status

Backend:
  Check cache → hits PostgreSQL if not cached
  Return verdict if status is final (not PENDING/JUDGING)
  Only cache final states (2s TTL)
```

---

## Step 10: Crash Recovery — Janitor

**File:** `SubmissionWorkerPool.java` → `janitorLoop()`

```
Every 60 seconds:

1. Find all processing keys: submission:processing:*
2. For each key → LRANGE get all jobs
3. For each job:
   a. Check DB: is submission status still PENDING/JUDGING?
      → If already finalized (AC/WA/CE/...) → safe to remove from processing list
   b. Check age: has claim timestamp > 5 minutes?
      → If yes, job is STUCK
   c. Remove from processing list → LPUSH back to submission:queue
4. Log: "Reclaimed X stuck job(s)"
```

With this, **koi bhi job permanently lost nahi hoti**. Worst case: worker crash → janitor 5 min baad reclaim karega → koi aur worker uthayega → user ko 5-6 min me result milega instead of <1 min.

---

## Run vs Submit — Difference

| Feature | Run Button | Submit Button |
|---------|-----------|---------------|
| `isTestRun` flag | true | false |
| Test cases evaluated | First 2-3 visible only | All (visible + hidden) |
| DB row | Yes (for polling access) | Yes (permanent record) |
| Leaderboard update | No | Yes (contest) |
| Score stored | No | Yes |
| Limit | 10 per problem | Rate limited per user |
| Purpose | Code testing/debugging | Official contest result |

---

## Possible Verdicts

| Verdict | Full Name | Kab Aata Hai |
|---------|-----------|-------------|
| **AC** | Accepted | All test cases passed ✓ |
| **WA** | Wrong Answer | Output doesn't match expected |
| **CE** | Compile Error | Code failed to compile |
| **RE** | Runtime Error | Code crashed during execution |
| **TLE** | Time Limit Exceeded | Execution exceeded time limit |
| **MLE** | Memory Limit Exceeded | Memory usage exceeded limit |
| **PENDING** | — | Waiting in Valkey queue |
| **JUDGING** | — | Worker is currently executing |

---

## Complete Flow — Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│ USER BROWSER                                                           │
│   │                                                                     │
│   │  POST /api/submissions                                              │
│   │  {code, problemId, language}                                        │
│   ▼                                                                     │
│ SubmissionController                                                    │
│   ├── JWT validate ✓                                                    │
│   ├── Rate limit check (Valkey INCR) ✓                                  │
│   ├── Basic validation ✓                                                │
│   └── → SubmissionService.submitCodeAsync()                            │
│          │                                                               │
│          ▼                                                               │
│   SubmissionService                                                     │
│     ├── Upsert: latest row reuse ya nayi                                │
│     ├── Save PENDING row → PostgreSQL                                   │
│     ├── Cache evict                                                     │
│     └── LPUSH job → Valkey submission:queue                             │
│          │                                                               │
│          ▼  202 Accepted                                                 │
│   User gets: {submissionId: 12345}                                      │
│                                                                          │
│   ═══════════ BACKGROUND (Worker Thread) ═══════════════                 │
│                                                                          │
│   SubmissionWorkerPool (judge-worker-1)                                  │
│     ├── LMOVE: submission:queue → processing (atomic)                   │
│     ├── DB: UPDATE → JUDGING                                            │
│     ├── Fetch harness (Valkey cache → PostgreSQL fallback)              │
│     ├── Inject user code between markers                                │
│     └── DockerJudgeService.execute()                                    │
│              │                                                           │
│              ▼                                                           │
│       SandboxRunner                                                      │
│         ├── bwrap (namespace isolation: PID,NET,USER,FS)                │
│         ├── prlimit (resource limits: CPU,MEM,NPROC,FSIZE)              │
│         └── ProcessBuilder → javac + java / g++ + ./a.out / python3    │
│              │                                                           │
│              ▼  stdout: TC:1:PASS, TC:2:FAIL:..., TC:3:PASS:hidden      │
│                                                                          │
│   parseOutput() → extract TC results                                    │
│     passed=2, total=3, score=66, status=WA                              │
│                                                                          │
│   finalizeAndNotify()                                                    │
│     ├── DB UPDATE: verdict, score, time, details                        │
│     ├── ZINCRBY leaderboard (if contest + AC)                           │
│     ├── Cache eviction                                                  │
│     ├── SSE push → user browser                                         │
│     └── LREM → processing list (job done)                               │
│                                                                          │
│   ════════════════════════════════════════════════════                    │
│                                                                          │
│   ▼  SSE: "verdict" event                                               │
│ USER BROWSER                                                            │
│   shows: WA — 2/3 test cases passed                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

### Single Queue, Clean Design

Ek hi `submission:queue` se sab submissions handle hote hain. `SubmissionJob` simple POJO hai jisme submission ke saare attributes hain — koi complex routing nahi, koi multiple queues nahi. Clean, simple, maintainable.

### Lock-Free Architecture
Java me kahin bhi `synchronized` ya `Lock` use nahi hota. Saara coordination Valkey ke atomic commands (LMOVE, INCR, ZINCRBY) aur PostgreSQL ki MVCC se hota hai. Single-threaded Valkey command execution LMOVE ko inherently thread-safe banata hai.

### Async Processing
Controller <10ms me response de deta hai. User ko wait nahi karna padta. Background async processing scalability ke liye best hai — ek saath 200 users submit kare to bhi queue buffer karega, workers apni speed se process karenge.

### Crash Safety
LMOVE + Janitor + claim timestamps ensure karte hain ki koi bhi job lost nahi hogi. Worker crash → job processing list me → Janitor detect → queue me wapas → doosra worker uthayega.

---

---

# Follow-Up Questions

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

### FQ4: SSE connection (single VM, no (single VM, no cross-VM issue) issue) delivery problem — kaise solve kiya?

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
  Worst case ((single VM, no (single VM, no cross-VM issue) issue)): 3-5 seconds (polling catches it)

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