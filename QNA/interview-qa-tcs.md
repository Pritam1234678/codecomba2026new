# CodeCoder — TCS Prime Interview Q&A

> Based on your resume and project. 29 questions covering the platform, submission engine, practice mode, auth, and technology choices.

---

## Introductory Question

### Q0: What is CodeCoder? What does your platform do?

**Answer:**

CodeCoder ek competitive programming platform hai — jaise LeetCode ya Codeforces, but self-built. Users problems solve karte hain, code submit karte hain, aur platform judge karta hai ki sahi hai ya galat.

**Main features:**

1. **Contests:** Admin contests create karta hai, users register karte hain, live leaderboard ke saath compete karte hain. Proctored contests bhi hain with camera monitoring.

2. **Practice Mode:** Users apne pace pe problems solve karte hain. Points milte hain (Easy=5, Medium=7, Hard=10), streak track hoti hai, GitHub pe code auto-sync hota hai. Solutions share kar sakte hain.

3. **5 Language Support:** JAVA, C++, PYTHON, JAVASCRIPT, C — sabki judging hoti hai bwrap sandbox ke andar.

4. **Duel Mode:** 1v1 live coding battles with real-time WebSocket.

5. **Company Sheets:** Deloitte ke 100 handpicked problems ek sheet me organized hain. Users sheet follow karke company-specific interview prep kar sakte hain.

6. **Topic Playlists:** Problems topic-wise organized — Arrays, DP, Trees, Graphs etc — structured learning path.

7. **AI Problem Generator:** Admin ek query dega (e.g., "Two Sum"), AI (NVIDIA Nemotron) automatically problem statement + test cases + 5 language harnesses generate karega.

**Tech Stack:** Spring Boot (Java) + React.js + PostgreSQL + Valkey (Redis-compatible). Single VM deployment.

---

## Part 1: Submission Engine (Questions 1-8)

### Q1: Explain your code submission engine in 2 minutes.

Code likhta hu, submit karta hu, backend kya karta hai?

**Answer:**

User browser se POST request aati hai `{code, problemId, language}`. Controller validate karta hai — JWT token check, rate limiting (5 submissions per 10 seconds). Phir `SubmissionService` DB me PENDING status ke saath save karta hai, aur ek job JSON ke form me Valkey (Redis-compatible) queue me push karta hai.

Background me 2 worker threads continuously queue se jobs uthate hain. Har worker LMOVE command se job ko atomically claim karta hai — isse do workers kabhi ek hi job nahi uthate. Worker problem ka harness fetch karta hai (Valkey cache se, ya DB se), user code ko markers ke beech inject karta hai, aur bwrap sandbox ke andar execute karta hai.

bwrap Linux namespace isolation provide karta hai — naya PID space, no network, filesystem read-only. prlimit resource limits lagata hai — max memory 256MB, max CPU N seconds, max 64 processes. Code ka output parse karte hain — harness `TC:1:PASS` format me print karta hai. Verdict (AC/WA/CE/RE/TLE) DB me save hota hai aur SSE ke through <1 second me browser tak pahunchta hai.

Pura flow async hai — user ko <10ms me 202 Accepted response milta hai, actual execution background me hota hai.

---

### Q2: Why did you use a queue instead of executing directly?

**Answer:**

Direct execution me 3 problems hain:

1. **Back-pressure:** Agar 50 users ek saath submit kare, to server overload ho jayega. Queue buffer ki tarah kaam karta hai — workers apni speed se process karte hain.

2. **Crash safety:** Agar worker crash ho jaye (OOM, JVM crash), to job Valkey me safe hai. LMOVE command job ko queue se processing list me move karta hai — agar worker crash hua, to janitor 5 minute baad detect karega aur job wapas queue me daal dega. Zero job loss.

3. **Fairness:** FIFO queue ensure karta hai pehle aaya pehle process hoga.

Analogy: Restaurant me waiter (controller) order leta hai, kitchen me queue me order jaata hai, chef (worker) ek ek karke banata hai. Direct execution = har customer ko chef ke paas bhejna — chaos.

---

### Q3: LMOVE vs BRPOP — what's the difference and why did you choose LMOVE?

**Answer:**

BRPOP queue se item POP karta hai — permanently remove. Agar worker BRPOP ke baad crash ho jaye, to job **lost forever**. User ka submission hamesha PENDING reh jayega.

LMOVE ek atomic command hai jo queue se item ko MOVE karta hai doosri list me. Humara pattern:

```
LMOVE submission:queue → submission:processing:{host}:{workerIdx}
```

Job queue se move hui, processing list me safe hai. Agar worker crash hua → job processing list me pada hai → Janitor 5 min baad reclaim karega → wapas queue me. Zero job loss.

**Interviewer ke liye keyword:** Atomic claim pattern, exactly-once processing guarantee without distributed locks.

---

### Q4: How does your sandbox prevent malicious code? What if I write `System.exit(0)` or `while(true){}`?

**Answer:**

Do-layer security hai:

**Layer 1: bwrap (Bubblewrap)** — Linux namespace isolation. User code ke liye ek naya PID namespace, network namespace, aur user namespace create hota hai:
- `--unshare-all` → naya PID + NET + IPC namespace — host processes invisible, no network
- `--uid 65534` → "nobody" user — koi file permissions nahi
- `--ro-bind /usr /usr` → system files read-only
- `--tmpfs /tmp` → temporary write space, job ke baad delete
- `--die-with-parent` → agar JVM mare to sab kuch kernel kill karega

**Layer 2: prlimit** — Resource limits:
- `--as=268435456` → max virtual memory: 256MB (malloc(10GB) → ENOMEM)
- `--cpu=N` → max CPU seconds (while(true){} → SIGXCPU after N seconds → TLE)
- `--nproc=64` → max processes (fork bomb → 65th fork fails)
- `--nofile=64` → max open files

**Specific attacks handled:**
- `System.exit(0)` → process exit 0, partial TC output captured → RE/WA verdict
- `while(true){}` → RLIMIT_CPU → SIGXCPU → process killed → TLE
- `Runtime.exec("rm -rf /")` → bwrap me `/` read-only hai, delete impossible
- `new Socket("google.com", 80)` → `--unshare-all` → koi network nahi → ENETUNREACH
- `new FileInputStream("/etc/passwd")` → bwrap me /etc read-only mounted hai, padh sakta hai (contains no secrets), lekin /home invisible

---

### Q5: How does your leaderboard work? If a user submits WA then AC, does the score double-count?

**Answer:**

Leaderboard Valkey Sorted Set (ZSET) pe based hai. Per-contest leaderboard key: `leaderboard:contest:{contestId}`.

**Score tracking:** Delta approach use karte hain. Har problem ke liye user ka previous score Valkey me stored hai (`contest:score:{contestId}:{userId}:{problemId}`). Naye submission pe:

```java
int prevScore = getPreviousScore();  // e.g., 50 (WA)
int delta = newScore - prevScore;    // 100 - 50 = +50
leaderboard.updateScore(contestId, userId, delta);
previousScore = newScore;            // store 100 for next time
```

Isse double-counting prevent hoti hai — sirf IMPROVEMENT leaderboard me add hota hai, poora score nahi.

**Atomicity:** ZINCRBY Valkey me atomic command hai. 100 users simultaneous submit kare to bhi each score correctly increment hoga. Single-threaded Valkey command execution se race condition impossible hai.

---

### Q6: How does the SSE (Server-Sent Events) verdict delivery work? What if SSE fails?

**Answer:**

**Primary Path — SSE:**
1. Frontend `POST /api/submissions/sse-ticket` se single-use ticket leta hai (Valkey GETDEL — consumed on read)
2. `new EventSource("/submissions/stream?ticket=...")` se persistent connection open
3. Worker verdict complete karte hi `sseRegistry.sendVerdict(userId, verdict)` call karta hai
4. `SseEmitter.send()` browser ko <1ms me event push karta hai

**Fallback — Polling:**
SSE drop hone pe (network hiccup, page refresh), frontend auto-reconnect karta hai: `onerror` → close dead EventSource → 3s delay → new ticket → reconnect.

Parallel me polling bhi chalta hai — `GET /api/submissions/{id}/status` every 2-3s. Jo pehle verdict deliver kare, wo UI update karta hai. Typically SSE wins (<1s), polling safety net hai (3-5s worst case).

---

### Q7: Why did you choose SSE over WebSocket for verdict delivery?

**Answer:**

SSE (Server-Sent Events) is uni-directional — server pushes, browser listens. Perfect for verdict delivery kyunki browser ko sirf result receive karna hai, send nahi karna.

| | SSE | WebSocket |
|---|---|---|
| Direction | Server → Client | Bidirectional |
| Protocol | HTTP/1.1 | WebSocket upgrade |
| Auto-reconnect | Built-in (EventSource) | Manual |
| Firewall/Proxy | Works everywhere | Sometimes blocked |
| Complexity | Simple | Complex (ping/pong, heartbeat) |

Verdict delivery ke liye SSE ideal hai — simple, reliable, firewall-friendly. WebSocket unnecessary complexity add karta. WebSocket sirf wahan use kiya jahan bidirectional communication chahiye — proctoring (camera frames) aur duel mode (real-time).

---

### Q8: How did you handle the bug where practice polling always returned 404?

**Answer:**

Practice submissions `practice_submissions` table me save hote hain, contest submissions `submissions` table me. Polling endpoint `GET /api/submissions/{id}/status` sirf `submissions` table check karta tha. Practice ke saare submissions 404 return karte the — frontend retry loop me fans jata tha → 40 second timeout → error.

**Fix:** Polling endpoint me dual-table check add kiya:

```java
// Pehle submissions table check karo
Submission sub = submissionRepo.findById(id).orElse(null);
if (sub != null) return buildResponse(sub);

// Nahi mila → practice_submissions table check karo
PracticeSubmission psub = practiceSubmissionRepo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException());
return buildResponse(psub);
```

Isse dono tables cover ho gaye. Simple fix, big impact.

---

## Part 2: Practice Mode (Questions 9-15)

### Q9: Explain your practice mode flow. How is it different from contest mode?

**Answer:**

Practice mode ek self-paced learning environment hai jo contest jaisa hi submission engine use karta hai — same queue, same worker pool, same sandbox. Differences:

| Aspect | Contest | Practice |
|--------|---------|----------|
| DB Table | `submissions` | `practice_submissions` |
| Leaderboard | Yes | No |
| Points | Per-problem scoring | Simple points (Easy=5, Medium=7, Hard=10) |
| GitHub Sync | No | Yes (auto-push on AC) |
| Run vs Submit | Separate buttons | Run IS Submit |

Practice me user problems solve kar sakta hai bina contest ke pressure ke. Points milte hain, streak track hoti hai, aur GitHub pe code auto-sync hota hai.

---

### Q10: How does GitHub auto-sync work in practice mode?

**Answer:**

Jab practice submission AC hota hai, worker `GitHubService.pushSolution()` call karta hai:

1. Check if user has `githubToken` (OAuth connected?)
2. Create/ensure `CodeCoder` repo exists
3. File path: `{problem-slug}/{lang}{n}Solution.{ext}`  
   Example: `two-sum/java3Solution.java`
4. Count existing language files in that folder → auto-increment n
5. Commit via GitHub API with message: "✅ Two Sum — JAVA solution #3"

User ka GitHub profile pe `CodeCoder` repo me saare solved problems organized milte hain. First connect via OAuth on practice page, then automatic.

---

### Q11: How does the streak system work? What resets it?

**Answer:**

`StreakService.updateStreak(userId)` har real submission pe call hota hai. Logic:

1. User ki `lastActiveDate` + `currentStreak` + `maxStreak` DB se fetch
2. Compare with today:
   - First activity ever → streak = 1
   - Same day → no change (duplicate submit se streak spam nahi hogi)
   - Yesterday (consecutive) → streak++
   - Gap > 1 day → streak = 1 (RESET)
3. `maxStreak = MAX(maxStreak, currentStreak)`
4. Save to DB: `users.current_streak`, `users.max_streak`, `users.last_active_date`

Dashboard pe "Current: Xd" + "Best: Yd" dikhta hai. Socials page pe achievement posters hain: 10d, 50d, 111d, 222d, 555d streak milestones.

---

### Q12: How do practice submissions not affect contest leaderboards?

**Answer:**

Job me `contestId = null` set hota hai practice submissions ke liye. Worker ke `finalizeAndNotify()` method me leaderboard update block contestId check karta hai:

```java
if (!job.isTestRun() && job.getContestId() != null) {
    leaderboard.updateScore(contestId, userId, delta);
}
```

Practice me `contestId == null`, to ye block skip ho jata hai. Clean separation — ek flag se pura flow control hota hai.

---

### Q13: What happens when multiple users submit the same problem simultaneously in practice?

**Answer:**

Koi issue nahi — har submission ka apna DB row hota hai (`practice_submissions` table). Do users ka same problem pe submit karna independent hai. Valkey queue FIFO handle karti hai, workers ek-ek karke process karte hain.

Practice me leaderboard nahi hai, to koi conflict nahi. Har user apne pace pe solve karta hai.

---

### Q14: How do you handle the 50,000 character code limit?

**Answer:**

Controller level pe validation hai:

```java
if (req.code.length() > 50_000) {
    return ResponseEntity.badRequest().build();
}
```

50K characters sufficient hai even for verbose Java solutions. Isse DB me blob overflow prevent hota hai aur malicious users large payloads nahi bhej sakte. Simple guard — request hi reject before DB/queue me jaye.

---

### Q15: How does the problem solution sharing work?

**Answer:**

Users practice problems pe solutions share kar sakte hain. Multi-language support — ek solution me JAVA, CPP, PYTHON, JS, C sab ka code ho sakta hai. Solution Monaco Editor me syntax-highlighted dikhta hai.

Backend: `problem_solutions` table stores codes as JSON map (`{"JAVA":"...", "PYTHON":"..."}`). Users view solutions, post their own, edit/delete only their own. Valkey cached for fast reads (15min TTL). Edit/delete invalidates cache.

---

## Part 3: Login & Auth (Questions 16-22)

### Q16: Explain your authentication flow. JWT vs Session — why JWT?

**Answer:**

Stateless JWT authentication use karte hain:

1. User POST `/api/auth/signin` with username + password
2. Server validates credentials → generates JWT with userId + roles + expiry (24h)
3. JWT signed with server secret (HMAC-SHA256)
4. All subsequent requests carry JWT in `Authorization: Bearer <token>` header
5. `AuthTokenFilter` intercepts every request → validates JWT → sets SecurityContext

**Why JWT over Session:**
- Stateless: koi server-side session store nahi chahiye
- Scalable: agar multiple servers ho to bhi JWT work karta (shared secret)
- Mobile-friendly: mobile apps can easily use JWT
- No DB lookup per request: JWT self-contained hai (userId, roles inside token)

---

### Q17: What happens when a JWT expires? How do you handle refresh?

**Answer:**

JWT expiry 24 hours hai. Expire hone pe frontend 401 Unauthorized receive karta hai → Axios interceptor catches it:

```javascript
api.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
    }
);
```

User auto-redirect to login page. No refresh token — 24h expiry practical hai for a platform where users actively solve problems. Background me session extend nahi hota — re-login is intentional security measure.

---

### Q18: How do you store passwords? What if the DB gets leaked?

**Answer:**

Passwords BCrypt se hashed hain. Spring Security ka `BCryptPasswordEncoder` use karte hain:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt ki properties:
- Salted: har password ka unique random salt
- Adaptive: work factor configurable (default 10 rounds)
- One-way: hash se original password recover impossible

DB leak hone pe attacker ko hashed passwords milenge — BCrypt brute-force prohibitively expensive hai. Saath me rate limiting (5 login attempts per IP per minute) brute-force attempts block karta hai.

---

### Q19: How does rate limiting work on login? What prevents brute force?

**Answer:**

Do-layer rate limiting:

**Layer 1: Valkey INCR (shared):**
```
Key: auth:fail:{ip}
Logic: INCR on failed login
       If count >= 5 → LOCK for 15 minutes
       Successful login → delete key
```

**Layer 2: ConcurrentHashMap fallback:**
```
Valkey down ho jaye to in-memory counter per JVM
```

Cloudflare Turnstile bhi active hai login page pe — bot submissions block. Three layers combined make brute force practically impossible.

---

### Q20: What is Cloudflare Turnstile and why did you add it?

**Answer:**

Turnstile Cloudflare ka CAPTCHA alternative hai — user ko "I'm not a robot" checkbox se verify karta hai, bina image puzzles ke. Privacy-friendly, frictionless.

Login aur signup dono pe active hai. Backend `TurnstileService` Turnstile ke `/siteverify` API se token validate karta hai. Without valid token, authentication attempt reject hoti hai.

Bot protection ke liye — automated scripts login/signup spam nahi kar sakte. Production me `TURNSTILE_ENABLED=true`, development me false.

---

### Q21: How does account disable/enable work? What happens to disabled users?

**Answer:**

Admin user ko disable kar sakta hai (`users.enabled = false`). Disabled user ke saath:

1. Login attempt → "ACCOUNT_DISABLED" error message
2. Existing JWT still works until expiry (24h max wait)
3. Frontend interceptor 403 + ACCOUNT_DISABLED check karta hai:
   ```javascript
   if (error.response?.status === 403 && 
       error.response?.data?.message?.includes('ACCOUNT_DISABLED')) {
       // Show blocking modal, redirect to login
   }
   ```
4. JWT blacklist nahi karte — expire hone tak wait karte hain (tradeoff: simplicity over instant kill)

---

### Q22: How do you handle CORS? What origins are allowed?

**Answer:**

`application.properties` me CORS configured hai:

```properties
APP_ALLOWED_ORIGINS=codecoder.in,www.codecoder.in,localhost:5173
```

Spring Boot `CorsConfiguration` allow karta hai:
- Frontend (Vercel) → codecoder.in
- Local dev → localhost:5173
- Preflight OPTIONS requests automatically handled

Only trusted origins. `allowedMethods: *`, `allowedHeaders: *` for simplicity — JWT token in Authorization header allowed. Credentials allowed (cookies/tokens).

---

## Bonus: Architecture Questions

### Q23: Why PostgreSQL + Valkey (Redis) instead of just one?

**Answer:**

PostgreSQL = persistent storage (problems, submissions, users — data that must survive restart).  
Valkey = ephemeral high-speed cache + queue + leaderboard.

Why both:
- **Leaderboard:** ZINCRBY O(log N) vs PostgreSQL COUNT + ORDER BY O(N)
- **Cache:** Valkey sub-millisecond vs DB 100-200ms
- **Queue:** LMOVE atomic job claiming without DB locks
- **Separation:** Judge queue traffic doesn't impact DB performance

PostgreSQL handles what it's best at (relational data, ACID). Valkey handles what it's best at (speed, atomic ops, caching). Right tool for right job.

---

### Q24: How would you scale this to handle 10,000 concurrent users?

**Answer:**

Current single VM handles ~200 concurrent. For 10K:

1. **Horizontal worker scaling:** Multiple VM instances, all draining same Valkey queue. No code change — LMOVE naturally distributes across workers.
2. **DB read replicas:** PostgreSQL read replicas for dashboard queries. Writes go to master (submissions).
3. **CDN for static:** React app already on Vercel CDN.
4. **Valkey cluster:** Sharding for larger cache + queue capacity.
5. **WebSocket sticky sessions:** If needed for proctoring — load balancer with session affinity.

Architecture designed for this — shared Valkey queue means adding workers is just deploying more VMs with same config.

---

### Q25: What happens if Valkey crashes? Does the entire website go down?

**Answer:**

Nahi, poori website down nahi hoti. Impact feature-by-feature:

**Immediately broken (Valkey-dependent):**
| Feature | Impact | Recovery |
|---------|--------|----------|
| **Submission queue** | Naye submissions queue me push nahi honge → `RejectedExecutionException` | Service auto-restart pe queue resume |
| **SSE verdict push** | Real-time verdict browser tak nahi pahuchega | Polling fallback active — 3-5s me result milega |
| **Leaderboard** | Leaderboard stale ho jayegi (ZSET unavailable) | DB me raw data intact hai, recalculate kar sakte hain |
| **Problem cache** | Har request DB hit karega → response 100-200ms slow (instead of <1ms) | DB queries function normally, just slower |
| **Rate limiting** | Shared rate limit unavailable | Falls back to per-JVM in-memory ConcurrentHashMap |

**NOT affected (no Valkey dependency):**
- **Login/Signup** — PostgreSQL se directly
- **Practice page load** — problems DB se fetch honge (slower, but working)
- **User dashboard** — submissions DB se
- **Frontend UI** — React app already loaded in browser
- **Existing SSE connections** — until they timeout

**Worst case timeline:**
```
T+0s:    Valkey crash
T+5s:    New submissions rejected (queue unavailable)
T+30s:   Leaderboard stale, caches start expiring
T+60s:   janitor fails, but no jobs were in processing
T+∞:     Website functional but slower, SSE down, polling only
```

**Recovery:** Valkey restart → system auto-reconnects (Spring Data Redis auto-reconnect). Queue resumes, caches rebuild naturally on next requests. No data loss — submissions are in PostgreSQL.

**Design philosophy:** Valkey is a performance layer, not a correctness layer. Database (PostgreSQL) is the single source of truth. If Valkey is down, the site is slower but functional. No critical path DEPENDS on Valkey alone — always a DB fallback.

---

### Q26: Why didn't you use Docker? Your class is even named DockerJudgeService.

**Answer:**

DockerJudgeService me "Docker" naam misleading hai — actually **bwrap (bubblewrap)** use karta hai, Docker nahi. Naam rakha tha jab plan Docker ka tha, but later switch kiya.

**Why bwrap over Docker:**

| | Docker | bwrap |
|---|---|---|
| Startup | Full container boot (~2-5s) | Linux namespaces (~10-20ms) |
| Overhead | Docker daemon, image layers | Single binary, no daemon |
| Memory | Per-container overhead (~50MB+) | Zero overhead (host kernel) |
| Filesystem | Image layers, union mounts | Direct bind mounts |
| Complexity | Dockerfile, registry, daemon | Single CLI command |
| Use case | Production deployments | Process isolation |

For a judge system, **speed matters most**. User submit kare aur 5 second Docker startup wait kare — unacceptable. bwrap 10-20ms me namespace setup karta hai. Har submission ek alag bwrap process — fast, lightweight, disposable.

`SandboxRunner.wrap()` method actual bwrap command build karta hai: namespace flags, bind mounts, prlimit resource limits — sab ek command line me. Docker in sab ke liye heavyweight solution hota.

**TLDR:** bwrap = Linux namespace isolation without container overhead. Perfect for short-lived, high-frequency process isolation.

---

### Q27: Why Spring Boot? Kyun Node.js ya Django nahi use kiya?

**Answer:**

**Spring Boot chose kiya because:**

1. **JVM for Judge Engine:** Judge engine Java me likhna easy tha — ProcessBuilder, concurrency control, Valkey integration sab built-in. Node.js me child_process less reliable for long-running processes.

2. **Spring Data JPA:** Complex DB operations (upsert submission rows, leaderboard queries with JOINs, multiple tables) JPA handles elegantly. Django ORM bhi capable hai, but Node.js me Prisma/TypeORM relatively immature the.

3. **Spring Security:** Battle-tested auth framework. JWT, role-based access, method-level security — sab annotation-based, minimal boilerplate.

4. **Production maturity:** Spring Boot 3+ on JDK 21 (virtual threads coming). Large ecosystem, excellent documentation.

**Why not Node.js:** Single-threaded event loop judge engine ke liye problematic — code execution CPU-bound hai, event loop block ho jata. Worker threads API relatively new (Node 12+) and less battle-tested.

**Why not Django:** Python perfect for judge execution (we actually use python3 for AI reference solution verification), but for web layer — same CPU-bound issue. Django ORM powerful but JPA's lazy loading and caching more mature.

---

### Q28: Why PostgreSQL and not MySQL or MongoDB?

**Answer:**

**PostgreSQL over MySQL:**

| Feature | PostgreSQL | MySQL |
|---------|-----------|-------|
| ACID compliance | Full, robust | InnoDB only |
| JSON support | JSONB (indexed, binary) | JSON (text-based) |
| Full-text search | Built-in | Limited |
| MVCC | True multi-version | Undo log based |
| Window functions | Rich | Basic |
| Concurrency | Better under write-heavy load | Faster for simple reads |

Specific reasons for PostgreSQL:
- **JSONB columns:** `code_snippets` table me harness full code store karte hain. JSONB indexed queries fast hain
- **MVCC:** Multiple workers simultaneous UPDATE on submissions — no read locks
- **Flyway migrations:** Version-controlled schema changes, easy rollback
- **Production stability:** Oracle Cloud pe PostgreSQL managed service available

**Why not MongoDB:** Problem + submission data inherently relational hai:
- Users → Submissions → Problems (JOINs daily)
- Leaderboard ranking (sorted queries)
- Contest → ContestProblems → Problems (many-to-many)
- User → SolvedProblems (tracking)

MongoDB me in sab ke liye manual denormalization + multiple queries chahiye. PostgreSQL me simple JOINs. "Code judging data is relational, not document-based."

---

*End of TCS Prime Interview Q&A*
