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
