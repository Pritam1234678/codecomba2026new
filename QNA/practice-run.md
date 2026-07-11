# Practice Run Flow — Complete Architecture

---

## Ek Line Me Samjho

> User clicks Run → POST /api/practice/run → PracticeService.enqueuePractice() → Valkey queue `practice:queue` → Worker pool judges → SSE verdict + polling fallback → UI shows result

---

## Poora Flow — Big Picture

```
USER (Browser)                    Backend (VM1/VM2)                    Valkey          PostgreSQL
     │                                 │                                │                  │
     │── POST /api/practice/run ─────►│                                │                  │
     │                                │── Validate + enqueue ──────────────────────────►│
     │                                │── LPUSH practice:queue      │                 │
     │◄── 202 Accepted {subId} ───────│                               │                 │
     │                                │                                │                 │
     │                                │   [Worker Pool: 2 on VM1, 4 on VM2]          │
     │                                │  LMOVE practice:queue                   │
     │                                │    → practice:processing:vm1:1            │
     │                                │                                │                 │
     │                                │   [Worker Thread]                        │
     │                                │   1. Fetch harness (cache)                │
     │                                │   2. Inject user code                     │
     │                                │   3. bwrap + prlimit + execute            │
     │                                │   4. Parse TC output → verdict            │
     │                                │   4. sseRegistry.sendVerdict()            │
     │                                │        → Valkey if no SSE subscriber      │
     │                                │                                          │
     │◄─── SSE "verdict" ─────────────│                                │                 │
     │   (or polling fallback)        │                                │                 │
```

---

## Complete Flow — Step by Step

### Step 1: User Clicks "Run" on Practice Page

**Frontend:** `PracticeSolve.jsx` → `handleRun()`
```javascript
PracticeService.run(id, code, language)
  → POST /api/practice/run
  Body: { problemId: 38, code: "class Solution...", language: "JAVA" }
  Header: Authorization: Bearer <jwt>
```

### Step 2: Controller Validates & Enqueues

**File:** `PracticeController.java` → `runPractice()`
```java
@PostMapping("/run")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> runPractice(@RequestBody PracticeRunRequest req,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
    // 1. Validate code
    if (req.getCode() == null || req.getCode().isBlank())
        return ResponseEntity.badRequest().body(error("Code cannot be empty"));
    if (req.getCode().length() > 50_000)
        return ResponseEntity.badRequest().body(error("Code too large (max 50KB)"));

    // 2. Enqueue
    Long submissionId = practiceService.enqueuePractice(user.getId(), req.getProblemId(), req.getCode(), req.getLanguage());
    return ResponseEntity.accepted().body(Map.of("submissionId", submissionId));
}
```

### Step 3: PracticeService.enqueuePractice()

```java
public Long enqueuePractice(Long userId, Long problemId, String code, String language) {
    // 1. Validate problem
    Problem problem = problemRepository.findById(problemId)
        .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
    if (!Boolean.TRUE.equals(problem.getActive()))
        throw new IllegalArgumentException("Problem disabled");

    // 2. Get harness
    String harness = cacheService.getSnippetHarness(problemId, language);
    if (harness == null)
        throw new IllegalArgumentException("No harness for language: " + language);

    // 3. Rate limit: 20 runs / 60s per user per problem
    String rateKey = "practice:runs:" + userId + ":" + problemId;
    Long count = redis.opsForValue().increment(rateKey);
    if (count == 1) redis.expire(rateKey, Duration.ofSeconds(60));
    if (count > 20) throw new IllegalArgumentException("Rate limit exceeded");

    // 5. Create Submission row (practice=true, contest=null, testRun=false)
    Submission sub = new Submission();
    sub.setUser(user);
    sub.setProblem(problem);
    sub.setContest(null);          // ← Practice has NO contest
    sub.setCode(code);
    sub.setLanguage(lang);
    sub.setStatus(PENDING);
    sub.setTestRun(false);         // Practice = real submission, not test run
    sub.setPractice(true);         // ← Flags as practice
    sub = submissionRepository.save(sub);

    // 6. Build job & push to PRACTICE queue
    SubmissionJob job = new SubmissionJob();
    job.setSubmissionId(sub.getId());
    job.setUserId(userId);
    job.setProblemId(problemId);
    job.setContestId(null);              // ← No contest
    job.setCode(code);
    job.setLanguage(language);
    job.setTestRun(false);            // Practice = real submission
    job.setPractice(true);            // ← Practice flag
    job.setTimeLimit(problem.getTimeLimit());
    job.setMemoryLimit(problem.getMemoryLimit());

    String json = objectMapper.writeValueAsString(job);
    redis.opsForList().leftPush("practice:queue", json);  // Separate queue!

    return sub.getId();
}
```

---

## Queue & Workers — Separate Pipeline

| Aspect | Practice Run | Contest Run/Submit |
|---|---|---|
| **Queue** | `practice:queue` | `submission:queue` |
| **Workers** | VM1: 2 dedicated | Shared 8 (VM1=2, VM2=6) |
| **Worker Pool** | Dedicated 2 (VM1) + 4 (VM2) | Shared 8 workers |
| **DB Row** | `practice=true, contest=null, testRun=false` | `testRun=true` / `practice=false` |
| **Rate Limit** | 20 runs / 60s per problem | 10 runs / 60s per problem |
| **Verdict Delivery** | SSE + 3s polling fallback | SSE + 1s polling |
| **DB Visibility** | `/api/user/practice/history` | Dashboard + Leaderboard |
| **Points** | Profile points (first AC only) | Leaderboard ZSET |

---

## Worker Pool — Dedicated Practice Threads

**File:** `SubmissionWorkerPool.java`

```java
@Component
public class SubmissionWorkerPool {
    // Practice workers: VM1=2, VM2=4 (separate from contest pool)
    @Value("${PRACTICE_WORKERS:2}")  // VM1:2, VM2:4 (via env)
    private int practiceWorkerCount;

    @PostConstruct
    public void init() {
        // Contest workers
        for (int i = 0; i < workerCount; i++)
            pool.submit(() -> workerLoop("contest", QUEUE_KEY));

        // Practice workers — separate thread pool
        for (int i = 0; i < practiceWorkerCount; i++)
            pool.submit(() -> workerLoop("practice", PRACTICE_QUEUE_KEY));
    }

    private void workerLoop(String type, String queueKey) {
        while (!Thread.currentThread().isInterrupted()) {
            String jobJson = workerRedis.opsForList()
                .move(type.equals("practice") ? PRACTICE_QUEUE_KEY : QUEUE_KEY,
                      PROCESSING_KEY_PREFIX + instanceId + ":" + workerId,
                      Duration.ofSeconds(3));
            if (jobJson == null) continue;
            processJob(jobJson, type.equals("practice"));
        }
    }

    private void processJob(String jobJson, boolean isPractice) {
        // Same judging logic, but:
        // - Practice: contestId=null, practice=true, testRun=false
        // - Contest: contestId!=null, practice=false, testRun=false/true
    }
}
```

---

## Verdict Delivery — SSE + Polling Fallback

### Backend: `SseEmitterRegistry.sendVerdict()`

```java
public void sendVerdict(Long userId, Object verdictData) {
    ConcurrentHashMap<String, SseEmitter> subs = emitters.get(userId);
    if (subs == null || subs.isEmpty()) {
        // No active SSE — cache for polling fallback
        String key = "pending:verdict:" + userId + ":" + verdict.getSubmissionId();
        redis.opsForValue().set(key, json, Duration.ofMinutes(5));
        log.debug("No SSE subscriber for user {} — verdict cached for polling", userId);
        return;
    }
    // Fan out to all tabs
    subs.forEach((subId, emitter) -> {
        try {
            emitter.send(SseEmitter.event().name("verdict").data(verdict));
        } catch (Exception ex) {
            remove(userId, subId, "send-failed");
        }
    });
}
```

### Frontend: PracticeSolve.jsx — handleRun()

```javascript
const handleRun = () => {
    if (runningRef.current) return;
    runningRef.current = true;
    setRunning(true);
    setOutput(<Spinner />);

    PracticeService.run(id, code, language)
        .then(res => {
            const sid = res.data.submissionId;
            activeSubRef.current = sid;

            // Start polling fallback AFTER 3s (give SSE head start)
            pollTimer.current = setTimeout(() => startPolling(sid), 3000);

            // 40s hard timeout
            timeoutRef.current = setTimeout(() => {
                if (activeSubRef.current === sid) {
                    activeSubRef.current = null;
                    runningRef.current = false;
                    setRunning(false);
                    setOutput(<Error>Judging timed out. Try again.</Error>);
                }
            }, 40000);
        })
        .catch(err => {
            runningRef.current = false;
            setRunning(false);
            setOutput(<Error>Error: {err.message}</Error>);
        });
    };
```

### Polling Fallback (`startPolling`)

```javascript
const startPolling = (sid) => {
    let attempts = 0;
    const poll = async () => {
        if (activeSubRef.current !== sid) return; // Already handled by SSE
        try {
            const res = await api.get(`/submissions/${sid}/status`);
            const data = res.data;
            if (data.status && !['PENDING','JUDGING'].includes(data.status)) {
                // Verdict ready!
                activeSubRef.current = null;
                clearTimeout(timeoutRef.current);
                clearTimeout(pollTimerRef.current);
                runningRef.current = false;
                setRunning(false);
                if (data.status === 'AC') setSolved(true);
                setOutput(buildVerdictUI(toVerdict(data), false));
            } else if (attempts < 30) {
                pollTimerRef.current = setTimeout(poll, 2000);
            }
        } catch (err) {
            if (attempts < 30) pollTimerRef.current = setTimeout(poll, 2000);
        }
    };
    pollTimerRef.current = setTimeout(() => poll(), 3000); // SSE gets 3s head start
};
```

---

## Frontend Verdict Handling — Unified

### `toVerdict(raw)` — Normalizes API response

```javascript
function toVerdict(raw) {
    return {
        status:         raw.status,
        passed:         raw.testCasesPassed,
        total:          raw.totalTestCases,
        executionTime:  raw.timeConsumedMs,
        errorMessage:   raw.errorMessage,
        testCases:      raw.testCaseDetails ? JSON.parse(raw.testCaseDetails) : [],
        pointsAwarded:  raw.pointsAwarded || 0,
        alreadySolved:  raw.alreadySolved || false,
    };
}
```

### Unified `buildVerdictUI(v)` — Works for Both Practice & Contest

```javascript
function buildVerdictUI(v, isTestRun) {
    // Status badge
    if (v.status === 'CE' || v.status === 'RE') { ... }
    if (v.status === 'TLE') { ... }

    // Test cases
    const visibleTCs = v.testCases.filter(tc => !tc.hidden);
    return (
        <div>
            {visibleTCs.map((tc, i) => (
                <div key={i} className={tc.passed ? 'pass' : 'fail'}>
                    <span>{tc.passed ? '✓' : '✗'} Test Case {tc.testCase}</span>
                    {!tc.passed && (tc.input || tc.expected || tc.got) && (
                        <details><summary>Details</summary>
                            {tc.input && <div>Input: {tc.input}</div>}
                            {tc.expected && <div>Expected: {tc.expected}</div>}
                            {tc.got && <div>Got: {tc.got}</div>}
                        </details>
                    )}
                </div>
            ))}
        )}
    </div>
);
```

---

## Verdict Delivery — SSE + Polling Fallback

### Backend: `SseEmitterRegistry.sendVerdict()`

```java
public void sendVerdict(Long userId, Object verdictData) {
    ConcurrentHashMap<String, SseEmitter> subs = emitters.get(userId);
    if (subs == null || subs.isEmpty()) {
        // No active SSE — cache for polling fallback
        String key = "pending:verdict:" + userId + ":" + verdict.getSubmissionId();
        redis.opsForValue().set(key, json, Duration.ofMinutes(5));
        log.debug("No SSE subscriber for user {} — verdict cached for polling", userId);
        return;
    }
    // Fan out to all open tabs
    subs.forEach((subId, emitter) -> {
        try {
            emitter.send(SseEmitter.event().name("verdict").data(verdict));
        } catch (Exception ex) {
            remove(userId, subId, "send-failed");
        }
    });
}
```

### Frontend — SSE Handler (PracticeSolve.jsx)

```javascript
useEffect(() => {
    const user = AuthService.getCurrentUser();
    if (!user?.token) return;

    api.post('/submissions/sse-ticket')
        .then(res => {
            const ticket = res.data?.ticket;
            if (!ticket) return;
            const url = `${API_BASE}/submissions/stream?ticket=${ticket}`;
            const es = new EventSource(url);
            sseRef.current = es;

            es.addEventListener('verdict', (e) => {
                const raw = JSON.parse(e.data);
                if (activeSubRef.current != null &&
                    raw.submissionId !== activeSubRef.current) return;

                activeSubRef.current = null;
                if (timeoutRef.current) clearTimeout(timeoutRef.current);
                runningRef.current = false;
                setRunning(false);
                if (raw.status === 'AC') setSolved(true);
                setOutput(buildVerdictUI(toVerdict(raw), raw.testRun === true));
            });

            es.onerror = () => console.warn('Practice SSE connection error');
        });
    }, []);
}, []);
```

### Polling Fallback (starts 3s after Run click)

```javascript
const startPolling = (sid) => {
    let attempts = 0;
    const poll = () => {
        if (activeSubRef.current !== sid) return;
        api.get(`/submissions/${sid}/status`)
            .then(res => {
                const data = res.data;
                if (data.status && !['PENDING','JUDGING'].includes(data.status)) {
                    activeSubRef.current = null;
                    if (timeoutRef.current) clearTimeout(timeoutRef.current);
                    if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
                    runningRef.current = false;
                    setRunning(false);
                    if (data.status === 'AC') setSolved(true);
                    setOutput(buildVerdictUI(toVerdict(data), false));
                    return;
                }
                if (attempts < 30) pollTimerRef.current = setTimeout(poll, 2000);
            })
            .catch(() => { if (attempts < 30) pollTimerRef.current = setTimeout(poll, 2000); });
    };
    pollTimerRef.current = setTimeout(poll, 3000); // 3s head start for SSE
};
```

---

## Verdict UI — Unified `buildVerdictUI(verdict, isTestRun)`

```javascript
function buildVerdictUI(v, isTestRun) {
    if (v.status === 'CE' || v.status === 'RE') { /* red error box */ }
    if (v.status === 'TLE') { /* yellow timeout */ }
    if (v.status === 'MLE') { ... }
    if (v.status === 'AC') { /* green success + points badge */ }
    if (v.status === 'WA') { /* red with test case table */ }

    // Test case table — uses tc.status === 'PASS' (NOT tc.passed!)
    {visibleTCs.map((tc, idx) => (
        <div key={idx} className={tc.status === 'PASS' ? 'pass' : 'fail'}>
            <span>{tc.status === 'PASS' ? '✓' : '✗'} Test Case {tc.testCase}</span>
            {!tc.passed && (tc.input || tc.expected || tc.got) && (
                <details><summary>Details</summary>
                    {tc.input && <div>Input: {tc.input}</div>}
                    {tc.expected && <div>Expected: {tc.expected}</div>}
                    {tc.got && <div>Your Output: {tc.got}</div>}
                </details>
            )}
        </div>
    ))}
```

> **Note:** Backend stores `status: "PASS" | "FAIL"` in test case JSON. Both `PracticeSolve.jsx` and `ProblemSolve.jsx` now use `tc.status === 'PASS'` consistently.

---

## Rate Limits

| Action | Limit | Window | Key |
|---|---|---|---|
| Practice Run | 20 runs | 60s | `practice:runs:{userId}:{problemId}` |
| Contest Test Run | 10 runs | 60s | `sub:rl:events:{userId}:{problemId}` |
| Contest Submit | 5 submits | ∞ (per contest) | `sub:submit:{userId}:{problemId}` |
| Screenshot Upload | 20/min | 60s | `practice:screenshots:{userId}` |

---

## Common Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| `RATE_LIMITED` (429) | Too many runs | Wait 60s |
| `HARNESS_MISSING` | No harness for language | Admin configure harness |
| `CONTEST_INACTIVE` | 403 | Problem disabled |
| `RATE_LIMITED` (503) | Queue full (1000 pending) | Wait and retry |
| `HARNESS_MISSING` | No harness for language | Admin configure harness |
| `UNAUTHORIZED` on SSE | Ticket expired/used | Auto-reconnect via polling |

---

## Debugging Checklist

| Symptom | Likely Cause | Fix |
|---|---|---|
| "Judging timed out" | SSE race / slow worker | Polling fallback catches it |
| All test cases show FAIL | Frontend reads `tc.passed` but backend sends `status` | Use `tc.status === 'PASS'` |
| CE shows "No error details" | `parseClientTimestamp` failed | Check `result.getStderr()` in backend |
| Verdict never arrives | SSE ticket expired / not connected | Polling fallback covers this |
| Practice shows in dashboard | `findByUser_Id` missing `contest.id IS NOT NULL` | Add `AND s.contest.id IS NOT NULL` |

---

*Architecture: VM1 (2 contest + 2 practice workers) + VM2 (6 contest + 4 practice workers) → shared Valkey `practice:queue` & `submission:queue` → 0.4ms private network latency.*