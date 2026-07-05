# How Run & Submit Work in Contest Mode

---

## One-Line Summary

> **Run** = quick test (2 visible TCs, no score) | **Submit** = full judge (all TCs, leaderboard updates)

Both go through the **same async Valkey queue** but produce different results.

---

## "RUN" Flow — Step by Step

### Step 1: User clicks "Run" button
- Frontend sends: `POST /api/submissions/test`
- Body: `{ problemId: 38, code: "...", language: "JAVA" }`
- Header: `Authorization: Bearer <jwt>`

### Step 2: Controller validates
- JWT valid? ✓ (AuthTokenFilter already checked)
- Rate limit OK? → `rateLimiter.allowTestRun(userId)` ✓
- Run count < 10? → `countContestRuns(userId, problemId)` ✓
- If run count >= 10 → return 429 "Run limit reached (10/10)"

### Step 3: Service saves to DB
- Create NEW submission row (always new for test runs)
- Set `status = PENDING`, `testRun = true`
- Save to PostgreSQL → get `submissionId = 5001`

### Step 4: Push to Valkey queue
- Build `SubmissionJob` with `isTestRun = true`
- `LPUSH submission:queue ← job_json`
- Return 202 immediately to user (< 10ms)

### Step 5: Worker claims job (async, background)
- Worker on VM1 or VM2: `LMOVE submission:queue → submission:processing:vm2:3`
- Atomic — no two workers can grab same job
- Update DB: `status = JUDGING`

### Step 6: Fetch harness
- Check Valkey: `GET snippet:38:JAVA`
- If HIT → use cached harness (0.5ms)
- If MISS → query PostgreSQL → cache it (TTL 60min)

### Step 7: Inject user code
- Find `// USER_CODE_START` and `// USER_CODE_END` markers in harness
- Replace content between markers with user's code
- Result = complete executable program

### Step 8: Execute in sandbox
- `bwrap` creates isolated namespace (no network, UID=65534, readonly FS)
- `prlimit` sets: memory limit, CPU limit, max processes
- Compile: `javac Solution.java` (30s timeout)
- Run: `java -Xmx256m Solution` (problem's time limit)
- Capture stdout + stderr

### Step 9: Parse output (TEST RUN MODE)
- Read stdout lines: `TC:1:PASS`, `TC:2:FAIL:input=...:expected=...:got=...`, `TC:3:PASS:hidden`
- **KEY: Filter only VISIBLE (non-hidden) test cases**
- **Take only first 2 visible TCs** (SAMPLE_TC_LIMIT = 2)
- Result: passed=1, total=2, status=WA

### Step 10: Save verdict + notify
- Update DB: status=WA, passed=1, total=2, score=50
- **❌ NO leaderboard update** (isTestRun = true)
- Push SSE to user: `{ isTestRun: true, status: "WA", passed: 1, total: 2 }`
- Remove job from processing list (LREM = ACK)

### Step 11: User sees result
- Browser receives SSE event OR polls `/submissions/{id}/status`
- Shows: "1/2 sample test cases passed"
- User fixes code and tries again

---

## "SUBMIT" Flow — Step by Step

### Step 1: User clicks "Submit" button
- Frontend sends: `POST /api/submissions`
- Body: `{ problemId: 38, code: "...", language: "JAVA" }`
- Button shows: "Submit (2/5)" — tracks usage

### Step 2: Controller validates
- JWT valid? ✓
- Rate limit OK? ✓
- Submit count < 5? → `countContestSubmits(userId, problemId)` ✓
- If count >= 5 → return 429 "Submit limit reached (5/5)"

### Step 3: Service saves to DB (UPSERT)
- Find latest submission for this (user, problem):
  - **Case A:** Existing row with final verdict (AC/WA/CE/RE) → **reuse same row**
  - **Case B:** Existing row PENDING/JUDGING → **create new row** (don't overwrite in-flight)
  - **Case C:** No existing row → **create new row**
- Set `status = PENDING`, `testRun = false`
- Evict caches: `DEL submissions:user:{userId}`, `DEL submission:user:problem:{userId}:{problemId}`

### Step 4: Push to Valkey queue
- Build `SubmissionJob` with `isTestRun = false`
- `LPUSH submission:queue ← job_json`
- Return 202 immediately to user (< 10ms)

### Step 5: Worker claims job (same as Run)
- `LMOVE submission:queue → submission:processing:<host>:<idx>`
- Update DB: `status = JUDGING`

### Step 6–8: Same as Run (fetch harness, inject code, execute in sandbox)

### Step 9: Parse output (SUBMIT MODE)
- Read ALL stdout lines (visible + hidden)
- **KEY: Uses ALL test cases — not limited to 2**
- Example: TC:1:PASS, TC:2:PASS, TC:3:PASS:hidden, TC:4:FAIL:hidden, TC:5:PASS:hidden, TC:6:PASS:hidden
- Result: passed=5, total=6, score=83 (Math.round(5*100/6)), status=WA

### Step 10: Save verdict + Leaderboard update
- Update DB: status=WA, passed=5, total=6, score=83

- **✅ LEADERBOARD UPDATE (delta approach):**
  ```
  1. GET contest:score:{contestId}:{userId}:{problemId} → prev = 0 (first submit)
  2. SET contest:score:{contestId}:{userId}:{problemId} = 83 (TTL 26h)
  3. delta = 83 - 0 = +83
  4. ZINCRBY leaderboard:contest:{contestId} +83 {userId}
  ```

- Evict caches
- Push SSE to user: `{ isTestRun: false, status: "WA", passed: 5, total: 6, score: 83 }`
- LREM (job ACK)

### Step 11: User sees result
- "5/6 test cases passed — Score: 83"
- Leaderboard immediately shows user with 83 points for this problem

---

## Leaderboard Delta Scoring — Example

```
Problem X has 6 test cases. User makes 4 submits:

Submit 1: 3/6 pass → score=50
  prev=0, new=50, delta=+50
  Leaderboard: 0+50 = 50

Submit 2: 5/6 pass → score=83
  prev=50, new=83, delta=+33
  Leaderboard: 50+33 = 83

Submit 3: 6/6 pass → score=100
  prev=83, new=100, delta=+17
  Leaderboard: 83+17 = 100

Submit 4 (worse): 2/6 pass → score=33
  prev=100, new=33, delta=-67
  Leaderboard: 100-67 = 33  (score DROP!)

Key: contest:score:{contestId}:{userId}:{problemId}
TTL: 26 hours (auto-cleanup after contest ends)
No double-counting ever. Latest score = what's on leaderboard.
```

---

## Run vs Submit — Comparison Table

| Aspect | Run (Test) | Submit (Real) |
|--------|-----------|---------------|
| Endpoint | `POST /submissions/test` | `POST /submissions` |
| DB row | Always NEW row, `testRun=true` | Upserts (reuses finished row) |
| Test cases evaluated | First 2 visible only | ALL (visible + hidden) |
| Leaderboard | Never touched | Delta update on every submit |
| Score meaning | Cosmetic only | Actual ranking score |
| Max per problem | 10 runs | 5 submits |
| Purpose | Quick check before committing | Final official verdict |

---

## How User Gets Result (Both Run & Submit)

```
Path 1 — SSE (primary, <1 second):
  1. Browser already has EventSource open (connected before first Run/Submit)
  2. Worker calls sseRegistry.sendVerdict(userId, verdict)
  3. Browser receives event instantly

Path 2 — Polling (fallback, 3-5 seconds):
  1. If SSE drops (cross-VM, network issue)
  2. Frontend polls: GET /submissions/{id}/status every 3s
  3. Reads from shared PostgreSQL (always has the verdict)
  4. Works regardless of which VM processed the job
```

---

## Crash Recovery (Janitor)

```
Problem: Worker dies mid-execution → job stuck in processing list

Solution: @Scheduled every 60 seconds:
  1. SMEMBERS submission:processing:registry → all processing keys
  2. For each key: LRANGE → get stuck jobs
  3. For each job:
     - Check DB: still PENDING/JUDGING?
     - Check age: > 5 minutes?
     - YES → LREM from processing + LPUSH back to queue
  4. Another worker picks it up → no job ever lost
```

---

## Interview Answer (2 minutes)

> "In contest mode, both Run and Submit use the same async pipeline. Code is saved as PENDING in PostgreSQL, a job is pushed to a durable Valkey queue, and the response returns instantly as 202. Background workers on two VMs claim jobs atomically via LMOVE.
>
> The difference is in result handling. Run evaluates only 2 visible test cases and never touches the leaderboard — it's for quick validation. Submit runs ALL test cases and updates the leaderboard using a delta approach: we track the previous per-problem score in Valkey and apply only the difference via ZINCRBY. This means re-submitting replaces your old score without double-counting.
>
> We cap submits at 5 per problem and runs at 10 per problem. Results reach the user via SSE in under 1 second, with a polling fallback for cross-VM reliability. If a worker crashes, a janitor process reclaims stuck jobs after 5 minutes and re-queues them."
