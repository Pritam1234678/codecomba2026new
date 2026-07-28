# Practice Submission Flow — Complete Architecture

---

## Ek Line Me Samjho

> User clicks Run → POST /api/practice/run → PracticeService.enqueuePractice() → pushes to `submission:queue` (SAME queue as contest) → Worker pool judges → SSE verdict + polling fallback → UI shows result + GitHub sync + streak update

**Important:** Practice and Contest use the **SAME** Valkey queue (`submission:queue`) and **SAME** worker pool. The only difference is flags in the job and which DB table the result goes to.

---

## Why Same Queue?

Pehle (June 2026) practice ka alag queue tha (`practice:queue`). But workers sirf `submission:queue` se jobs uthate the. Practice jobs kabhi process hi nahi hoti thi — user ko 40 second timeout ke baad error milta tha. July 2026 fix: dono same queue use karte hain.

---

## Poora Flow — Big Picture

```
USER (Browser)                         BACKEND                       VALKEY              POSTGRESQL
     │                                     │                            │                      │
     │── POST /api/practice/run ──────────►│                            │                      │
     │  {problemId, code, language}        │── Validate + enqueue ─────────────────────────►│
     │                                     │── LPUSH submission:queue ──►│                    │
     │◄── 202 Accepted ────────────────────│                            │                      │
     │                                     │                            │                      │
     │  [SSE open / polling]               │  [Worker: judge-worker-1]  │                      │
     │                                     │── LMOVE claim job ────────►│                    │
     │                                     │── DB → JUDGING ──────────────────────────────►│
     │                                     │── Fetch harness (cache)    │                    │
     │                                     │── bwrap sandbox execute     │                    │
     │                                     │── Parse TC: output          │                    │
     │                                     │── DB verdict ─────────────────────────────────►│
     │                                     │── Award points ───────────────────────────────►│
     │                                     │── GitHub auto-push                            │
     │                                     │── Streak update                               │
     │◄── SSE "verdict" ───────────────────│                            │                      │
```

---

## Step 1: User "Run" Button Dabata Hai

**File:** `PracticeSolve.jsx`

### Frontend se Request

```
POST /api/practice/run
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "problemId": 69,
  "code": "class Solution { public boolean containsDuplicate(int[] nums) { ... } }",
  "language": "JAVA"
}
```

### Frontend Response Handling

```
handleRun() {
  1. Set running=true, show "Running..." spinner
  2. POST /api/practice/run → 202 Accepted + {submissionId}
  3. Wait for verdict via SSE (primary) or polling (fallback)
  4. On verdict → show result in console panel
  5. On AC + GitHub connected → show "Pushing..." animation
}
```

### Theory: Run vs Submit in Practice

Practice mode me "Run" button hi "Submit" ka kaam karta hai. There's no separate "Submit" button. Har "Run" ek real submission hai (`isTestRun=false`). Karan: practice me leaderboard nahi hai, score count nahi hota — har attempt ek recordable submission hai.

---

## Step 2: Controller Validates + Delegates

**File:** `PracticeController.java`

```java
@PostMapping("/run")
public ResponseEntity<?> run(@RequestBody PracticeRunRequest req,
                              @AuthenticationPrincipal UserDetailsImpl user) {
    // 1. Problem exists?
    Problem problem = problemRepository.findById(req.problemId)
        .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));
    
    // 2. Code valid?
    if (req.code == null || req.code.isBlank())
        return badRequest("Code is required");
    
    // 3. Language valid?
    ProgrammingLanguage lang = ProgrammingLanguage.valueOf(req.language.toUpperCase());
    
    // 4. Enqueue
    Long submissionId = practiceService.enqueuePractice(
        user.getId(), req.problemId, req.code, req.language);
    
    // 5. Return immediately
    return ResponseEntity.ok(Map.of("submissionId", submissionId));
}
```

Response time: <5ms. User ko turant response.

---

## Step 3: PracticeService Queue Me Push Karta Hai

**File:** `PracticeService.java` → `enqueuePractice()`

### DB: PENDING Row Create (practice_submissions table)

```java
PracticeSubmission ps = new PracticeSubmission();
ps.setUserId(userId);
ps.setProblemId(problemId);
ps.setCode(code);
ps.setLanguage(lang);
ps.setStatus(PENDING);
ps.setProblemName(problem.getTitle());
ps.setUserName(user.getUsername());
practiceSubmissionRepository.save(ps);
```

### Valkey: Job Queue Me Push (submission:queue — SAME AS CONTEST)

```java
SubmissionJob job = new SubmissionJob();
job.setSubmissionId(ps.getId());    
job.setUserId(userId);
job.setProblemId(problemId);
job.setContestId(null);              // ← NULL = practice
job.setCode(code);
job.setLanguage(language);
job.setTimeLimit(problem.getTimeLimit());
job.setMemoryLimit(problem.getMemoryLimit());
job.setTestRun(false);               // ← REAL submission in practice
job.setPractice(true);               // ← PRACTICE flag
job.setDuelId(null);

String json = objectMapper.writeValueAsString(job);
redis.opsForList().leftPush("submission:queue", json);  // SAME QUEUE
```

### Theory: Shared Queue

Practice aur contest dono `submission:queue` use karte hain. Worker ko pata hota hai ki job practice hai ya contest — `job.isPractice()` flag se. Is flag ke hisaab se worker different paths leta hai (different DB table, no leaderboard, etc).

Pehle alag queue thi (`practice:queue`) lekin workers usse read nahi karte the — practice jobs stuck rehti thi. July 2026 me fix kiya.

### Cache Eviction

```java
redis.delete("practice:submissions:" + userId + ":" + problemId);
```

---

## Step 4: Worker Job Uthata Hai (SAME Worker Pool)

**File:** `SubmissionWorkerPool.java`

Practice ke liye koi alag worker pool nahi hai. Contest aur practice dono same `workerLoop()` se process hote hain:

```java
void processJob(SubmissionJob job) {
    // Step 1: DB status → JUDGING
    if (!job.isTestRun() && submissionId != null) {
        if (job.isPractice()) {
            practiceSubmissionRepository.updateStatus(submissionId, JUDGING);
        } else {
            submissionRepository.updateStatus(submissionId, JUDGING);
        }
    }
    
    // Step 2-5: Same harness fetch, code injection, sandbox execution
    // (identical to contest — see submission-flow.md Step 5)
    
    // Step 6: Parse output (identical)
    
    // Step 7: finalizeAndNotify with practice-specific hooks
}
```

---

## Step 5: Sandbox Execution (Identical to Contest)

Same `DockerJudgeService.execute()` call. Same bwrap + prlimit sandbox. Same harness injection. Same output parsing.

See `submission-flow.md` Step 6 for full details.

---

## Step 6: Practice-Specific finalizeAndNotify()

**File:** `SubmissionWorkerPool.java` → `finalizeAndNotify()`

### 6a: DB Update (practice_submissions table)

```java
if (job.isPractice()) {
    updated = practiceSubmissionRepository.updateResult(
        submissionId, inflight, status, errorMessage, 
        passed, total, (double) timeMs, score, details);
} else {
    updated = submissionRepository.updateResult(...);
}
```

### 6b: Leaderboard — SKIPPED

```java
// Practice has NO leaderboard — this entire block is skipped
if (!job.isTestRun() && job.getContestId() != null) {
    // ... contest leaderboard update
}
// contestId is null for practice → block doesn't execute
```

### 6c: Points Award (First AC Only)

```java
if (job.isPractice() && !job.isTestRun() 
    && status == AC && updated > 0) {
    try {
        practiceAwarded = practiceService.awardPointsIfFirstSolve(
            job.getUserId(), job.getProblemId());
        practiceSolved = (practiceAwarded == 0);
        // practiceAwarded = 0 means already solved before
    } catch (Exception e) {
        log.warn("Practice points award failed: {}", e.getMessage());
    }
}
```

Points by difficulty:
```
EASY   → 5 points
MEDIUM → 7 points
HARD   → 10 points
```

First solve only — `user_problem_solved` table tracks per-user per-problem. Duplicate AC pe 0 points.

### 6d: GitHub Auto-Push (AC Only)

```java
if (job.isPractice() && !job.isTestRun() && status == AC && updated > 0) {
    try {
        String problemTitle = cacheService.getProblemTitle(job.getProblemId());
        gitHubService.pushSolution(
            job.getUserId(), submissionId,
            problemTitle, job.getLanguage(), job.getCode(), null);
    } catch (Exception e) {
        log.warn("GitHub auto-push failed: {}", e.getMessage());
    }
}
```

**Flow:**
1. Check user has `githubToken` (OAuth connected?)
2. Ensure `CodeCoder` repo exists (create if not)
3. File path: `{problem-slug}/{lang}{n}Solution.{ext}`  
   Example: `contains-duplicate/java1Solution.java`
4. Count existing language files → auto-increment
5. Commit: "✅ Contains Duplicate — JAVA solution #1"

### 6e: Streak Update

```java
if (!job.isTestRun() && job.getDuelId() == null) {
    try {
        streakService.updateStreak(job.getUserId());
    } catch (Exception ignored) {}
}
```

Streak counts consecutive active days. See streak documentation for full details.

### 6f: SSE Push

```java
VerdictEvent event = new VerdictEvent(
    submissionId, status.name(), passed, total, score,
    timeMs, errorMessage, details,
    job.isTestRun(),        // false for practice
    job.isPractice(),       // TRUE
    practiceAwarded,        // points earned (0 if already solved)
    practiceSolved          // already solved before?
);
sseRegistry.sendVerdict(job.getUserId(), event);
```

Frontend receives `practice: true` flag — uses it to show/hide practice-specific UI elements.

---

## Step 7: User Ko Result Milta Hai

### Primary: SSE (Same as Contest)

Same SSE mechanism. PracticeSolve.jsx connects to same `/submissions/stream` endpoint. `VerdictEvent.practice = true` se frontend ko pata chal jata hai.

### Fallback: Polling (Dual-Table Check — July 2026 Fix)

```
GET /api/submissions/{submissionId}/status

Backend logic:
  1. Check submissions table
  2. If NOT found → check practice_submissions table
  3. Return verdict if status is final
  
Pehle sirf submissions table check hota tha. Practice submissions
practice_submissions table me hain — so polling always returned 404.
July 2026 fix: both tables check kiye jate hain.
```

### Frontend: GitHub Push Animation

```
On AC verdict:
  if (githubConnected) {
    show "Pushing..." with spinning sync icon (2s)
    → show "Pushed ✓" with green check (3s)
    → back to "Connected ✓"
  }
```

---

## Complete Practice Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│ USER: /practice/69                                                      │
│   │                                                                     │
│   │  Writes code, clicks "Run"                                          │
│   ▼                                                                     │
│ POST /api/practice/run                                                   │
│   │                                                                     │
│   ▼                                                                     │
│ PracticeController.run()                                                │
│   ├── Validate: problem exists, code valid, language supported          │
│   └── → PracticeService.enqueuePractice()                              │
│          │                                                               │
│          ▼                                                               │
│   PracticeService.enqueuePractice()                                      │
│     ├── Save PENDING → practice_submissions (PostgreSQL)                │
│     ├── Cache evict                                                      │
│     └── LPUSH SubmissionJob → submission:queue (SAME as contest!)       │
│          │                                                               │
│   ◄── 202 {submissionId} ── user gets response                          │
│                                                                          │
│   ═══════════ BACKGROUND (Worker Thread) ═══════════════                 │
│                                                                          │
│   SubmissionWorkerPool.processJob()                                      │
│     ├── DB: UPDATE practice_submissions → JUDGING                        │
│     ├── Fetch harness (Valkey cache)                                     │
│     ├── Inject user code                                                 │
│     ├── DockerJudgeService.execute() ← same sandbox                     │
│     └── parseOutput()                                                    │
│          │                                                               │
│          ▼                                                               │
│   finalizeAndNotify():                                                   │
│     ├── DB UPDATE: practice_submissions (verdict, score, time)          │
│     ├── Leaderboard: SKIPPED (contestId=null)                           │
│     ├── Points: awardPointsIfFirstSolve() — 5/7/10 by difficulty        │
│     ├── GitHub: pushSolution() → CodeCoder repo                          │
│     ├── Streak: updateStreak(userId)                                     │
│     ├── Cache eviction                                                   │
│     ├── SSE push → user browser                                          │
│     └── LREM → job done                                                  │
│                                                                          │
│   ════════════════════════════════════════════════════                    │
│                                                                          │
│   ▼  SSE "verdict" (primary) or polling (fallback)                      │
│ USER:                                                                   │
│   AC → shows "Accepted ✓" + "Pushing..." (GitHub)                       │
│   WA → shows failed test cases                                          │
│   CE → shows compiler error                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Practice vs Contest — Key Differences

| Aspect | Contest | Practice |
|--------|---------|----------|
| Endpoint | `POST /api/submissions` | `POST /api/practice/run` |
| DB Table | `submissions` | `practice_submissions` |
| Queue | `submission:queue` | `submission:queue` (SAME!) |
| Worker Pool | SubmissionWorkerPool | SubmissionWorkerPool (SAME!) |
| Leaderboard | Yes (ZINCRBY) | No |
| Score | Per-problem scored | Simple points (5/7/10) |
| GitHub Push | No | Yes (on AC) |
| Streak | Yes | Yes |
| isPractice flag | false | true |
| contestId | Set | null |
| Run button | Separate (isTestRun=true) | Run IS Submit (isTestRun=false) |

---

## Polling Fix (July 2026)

**Bug:** Practice submissions save to `practice_submissions` table. Polling endpoint `GET /api/submissions/{id}/status` sirf `submissions` table check karta tha. Practice polling always returned 404 → frontend kept retrying → 40 second timeout → error.

**Fix:** Polling endpoint now checks BOTH tables:
```java
// First check submissions table
Submission sub = submissionRepo.findById(submissionId).orElse(null);
if (sub != null) return buildResponse(sub);

// Not found → check practice_submissions
PracticeSubmission psub = practiceSubmissionRepo.findById(submissionId)
    .orElseThrow(() -> new ResourceNotFoundException());
return buildResponse(psub);
```

---

## Key Design Decisions

### Shared Queue Design
Practice and contest share `submission:queue`. This was a bug-fix-driven design choice — separate queues failed because workers only drained one. Shared queue with flags is simpler and more maintainable.

### Practice = Real Submission
Unlike contests where "Run" (test) and "Submit" (real) are separate, practice has no distinction. Every code execution is a real submission stored permanently. This enables: submission history, GitHub sync, streak tracking.

### Separate DB Table
Practice uses `practice_submissions` table to keep contest data clean. Polling needs to check both tables (July 2026 fix). GitHub sync flag (`github_pushed`) only exists on practice_submissions.

---
