# How Practice Run Works — Full Architecture

---

## One-Line Summary

> User clicks Run → code executes **synchronously** (no queue, no SSE) → verdict returns directly in HTTP response

---

## Step-by-Step Flow

### Step 1: User clicks "Run" on practice page
- Frontend sends: `POST /api/practice/run`
- Body: `{ problemId: 38, code: "class Solution...", language: "PYTHON" }`
- Header: `Authorization: Bearer <jwt>`

### Step 2: Controller validates request
- Code not blank? ✓
- Code length < 50,000 chars? ✓
- Pass to `PracticeService.runPractice(userId, problemId, code, language)`

### Step 3: Service validates problem
- Fetch problem from DB: `SELECT * FROM problems WHERE id=38`
- Is `active = true`? ✓ (if false → "This problem has been disabled")
- Get time limit (5.0s) and memory limit (256MB) from the problem row

### Step 4: Fetch code harness (cache-aside)
- Check Valkey: `GET snippet:38:PYTHON`
- **Cache HIT** (~0.5ms): return cached harness string
- **Cache MISS**: query PostgreSQL `code_snippets` table → cache it (60min TTL)
- If no harness exists at all → return "No code harness configured for language: PYTHON"

### Step 5: Submit to bounded thread pool
- `pool.submit(() → doJudge(...))`
- Pool config: `PRACTICE_WORKERS` threads (VM1=2, VM2=4)
- Queue: `ArrayBlockingQueue(1000)` — max 1000 pending requests
- **If queue is FULL** → `RejectedExecutionException` → return "Server busy — try again in a few seconds"
- **If accepted** → get a `Future<PracticeVerdict>`
- Wait for result: `future.get(15, TimeUnit.SECONDS)` — blocks the HTTP thread

### Step 6: Inject user code into harness
- Find `# USER_CODE_START` and `# USER_CODE_END` markers
- Replace content between markers with user's submitted code
- Result = complete Python program with test driver

### Step 7: Execute in sandbox
- Same sandbox as contest (bwrap + prlimit):
  - bwrap: new PID/NET/USER namespace, no internet, UID=65534, readonly FS
  - prlimit: RLIMIT_AS (memory), RLIMIT_CPU (seconds), RLIMIT_NPROC (fork limit)
- Run: `python3 solution.py`
- Capture: stdout, stderr, exit code, elapsed time
- If compile error (Java/C++) → return CE verdict immediately
- If time limit exceeded → kill process → return TLE
- If exit code 137 (SIGKILL) → Memory Limit Exceeded (MLE)

### Step 8: Parse test case output
- Read stdout line by line looking for `TC:N:PASS` or `TC:N:FAIL` format
- Example output:
  ```
  TC:1:PASS
  TC:2:PASS
  TC:3:PASS:hidden
  TC:4:PASS:hidden
  ```
- Count: passed=4, total=4
- **Practice mode runs ALL test cases** (no 2-TC limit like contest test-run)
- If passed == total → status = "AC"
- If passed < total → status = "WA"

### Step 9: Award points (only if ALL passed AND first time)
- Check: `SELECT EXISTS FROM user_problem_solved WHERE user_id=42 AND problem_id=38`
- **Already solved before?** → skip, set `alreadySolved = true`, `pointsAwarded = 0`
- **First time AC?** →
  1. `INSERT INTO user_problem_solved (user_id, problem_id, points_earned, solved_at)`
  2. `UPDATE users SET total_points = total_points + 7 WHERE id=42`
  3. Set `pointsAwarded = 7`
- **Race condition safety:** Unique constraint on `(user_id, problem_id)` — if two concurrent ACs arrive, one INSERT succeeds, other throws `DataIntegrityViolationException` → caught → returns 0 points (no double-award)

### Step 10: Return verdict directly in HTTP response
- No SSE, no polling — synchronous response:
```json
{
  "status": "AC",
  "passed": 4,
  "total": 4,
  "executionTime": 850,
  "pointsAwarded": 7,
  "alreadySolved": false,
  "totalPointsByLevel": 7,
  "testCases": [
    { "testCase": 1, "passed": true, "hidden": false, "input": "[1,2]", "expected": "3", "got": "3" },
    { "testCase": 2, "passed": true, "hidden": false },
    { "testCase": 3, "passed": true, "hidden": true },
    { "testCase": 4, "passed": true, "hidden": true }
  ]
}
```

### Step 11: User sees result
- "All Correct! +7 points"
- Hidden test cases show pass/fail status but NOT input/expected/got (anti-cheating)

---

## Key Architecture: Why Synchronous?

```
Contest mode (async queue):
  User → DB → Queue → 202 → Worker (later) → SSE/poll
  + Durable (job survives crash)
  + Fair (FIFO)
  - User waits 3-5 seconds for result
  - Complex (SSE + polling + janitor)

Practice mode (synchronous pool):
  User → Execute NOW → 200 with verdict
  + Instant feedback (< 2 seconds total)
  + Simple (no SSE, no polling, no queue delivery)
  + No submission table pollution
  - Not durable (crash = retry manually)
  - Bounded capacity (queue full = reject)

WHY this tradeoff?
  Practice is solo — no fairness requirement.
  Instant feedback is critical for learning iteration.
  If server crashes, user just clicks Run again (no harm).
```

---

## Thread Pool Details

```
Type:           ThreadPoolExecutor
Workers:        VM1 = 2 threads, VM2 = 4 threads
Queue:          ArrayBlockingQueue(1000)
Rejection:      AbortPolicy (throws → "Server busy")
Thread names:   practice-1, practice-2, practice-3...
Daemon:         true (don't block JVM shutdown)

Request flow:
  1. Request arrives
  2. Worker thread free? → execute immediately
  3. No free worker? → put in queue (up to 1000 waiting)
  4. Queue also full? → AbortPolicy → "Server busy" to user
  5. Execution timeout: Future.get(15s) — if still not done, cancel

Capacity math:
  VM1 (2 workers) + VM2 (4 workers) = 6 workers total
  Average execution: ~3-5 seconds
  Throughput: ~72-120 practice runs/minute
```

---

## nginx Load Balancing

```
Practice is the ONLY route load-balanced across both VMs:

  nginx config:
    upstream practice_pool {
        server 127.0.0.1:8080;    ← VM1
        server 10.0.0.34:8080;    ← VM2
        keepalive 16;
    }
    location /api/practice/ {
        proxy_pass http://practice_pool;
        proxy_next_upstream error timeout http_502 http_503;
    }

  Request 1 → VM1 (2 practice workers)
  Request 2 → VM2 (4 practice workers)
  Request 3 → VM1
  Request 4 → VM2
  ... round-robin

  If VM2 is down:
    nginx detects timeout/502 → retries on VM1 automatically
    User never sees an error (just slightly slower)
```

---

## Points System

```
Level     Points
────────────────
EASY      5
MEDIUM    7
HARD      10

Rules:
  - Only on AC (all test cases pass)
  - Only FIRST TIME per (user, problem) — unique constraint prevents double
  - Stored in: user_problem_solved table
  - user.total_points incremented atomically
  - Concurrent AC race: one wins INSERT, other catches constraint violation → 0 points
```

---

## All Possible Verdicts

| Status | Meaning | When |
|--------|---------|------|
| AC | All Correct | All test cases pass → points awarded (first time) |
| WA | Wrong Answer | Some test cases fail → no points |
| CE | Compile Error | javac/g++/gcc fails → show compiler error |
| RE | Runtime Error | Segfault, uncaught exception, or no output |
| TLE | Time Limit Exceeded | Code runs longer than problem's time limit |
| ERROR | System Error | Pool full / harness missing / problem disabled |

---

## Practice vs Contest — Side by Side

| | Practice Run | Contest Run | Contest Submit |
|---|---|---|---|
| Endpoint | `/api/practice/run` | `/api/submissions/test` | `/api/submissions` |
| Execution | Synchronous (in-process) | Async (queue) | Async (queue) |
| Response | 200 with verdict | 202 → SSE/poll | 202 → SSE/poll |
| Test cases | ALL | 2 visible only | ALL |
| DB writes | Only `user_problem_solved` on AC | submissions table | submissions table |
| Scoring | user.total_points (profile) | Nothing | Leaderboard ZSET |
| Limit | None (unlimited runs) | 10 runs/problem | 5 submits/problem |
| Load balance | nginx round-robin (VM1+VM2) | Workers drain shared queue | Workers drain shared queue |
| Crash recovery | User retries manually | Janitor re-queues after 5min | Janitor re-queues after 5min |

---

## Interview Answer (2 minutes)

> "Practice mode is architecturally different from contests. It's fully synchronous — the user's HTTP request blocks while code executes, and the verdict is returned directly in the 200 response. There's no queue, no SSE, no polling.
>
> PracticeService has a bounded ThreadPoolExecutor with an ArrayBlockingQueue of 1000 capacity. If all workers are busy and the queue is full, we reject with 'Server busy' rather than crashing. The code runs inside the same bwrap+prlimit sandbox as contests — identical security.
>
> nginx load-balances practice requests across both VMs in round-robin. VM1 has 2 workers, VM2 has 4, giving us about 72-120 runs per minute combined. If a VM goes down, nginx retries on the other transparently.
>
> Points are awarded only on first AC — we use a unique constraint on (user_id, problem_id) in the user_problem_solved table. Even if two concurrent requests both get AC, the constraint ensures only one INSERT succeeds. The other catches DataIntegrityViolationException and returns 0 points."
