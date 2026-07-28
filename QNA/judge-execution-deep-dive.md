# Judge Execution — Complete Deep Dive

> Jab worker `DockerJudgeService.execute()` call karta hai, tab se lekar result DB me save hone tak — har ek step, simple language me, with real code.

---

## 1. Temporary File System — Kaise File Banti Hai

### Work Directory Create

```java
String jobId = UUID.randomUUID().toString().substring(0, 8);  // random ID like "a3f2b9c1"
Path workDir = Paths.get("/tmp/judge", jobId);                  // /tmp/judge/a3f2b9c1
Files.createDirectories(workDir);                               // folder create karo
```

**Kya hua:**
- `/tmp` ek temporary filesystem hai — har reboot pe clear, disk space waste nahi
- Har submission ke liye **ek naya folder** banta hai (`/tmp/judge/a3f2b9c1`)
- Folder unique hai — 8-char random ID se koi collision nahi

### Java File Write

```java
// Java ke liye special — class name extract karte hain
String className = extractJavaClassName(code);   // "Main" ya "Solution"
Path sourceFile = workDir.resolve(className + ".java");  // /tmp/judge/a3f2b9c1/Main.java
Files.writeString(sourceFile, code);              // file write
```

**Kya likha:** User ke code ke saath harness ka full code ek file me likha jata hai. Ye file `/tmp/judge/a3f2b9c1/Main.java` pe exist karti hai.

**ExtractJavaClassName kya karta hai:** Code scan karta hai `public class Main` pattern ke liye. Harness hamesha `public class Main` rakhti hai, to className = "Main" milta hai.

### C++ / C File Write

```java
// C++:
Path sourceFile = workDir.resolve("solution.cpp");
Files.writeString(sourceFile, code);

// C:
Path sourceFile = workDir.resolve("solution.c");
Files.writeString(sourceFile, code);
```

### Python / JavaScript

```java
// Python:
Path sourceFile = workDir.resolve("solution.py");
Files.writeString(sourceFile, code);

// JavaScript:
Path sourceFile = workDir.resolve("solution.js");
Files.writeString(sourceFile, code);
```

---

## 2. Java Compilation — Step by Step

### Compile Command

```java
ExecutionResult compileResult = runProcess(
    List.of("javac", "-J-Xms16m", "-J-Xmx256m", "/tmp/judge/a3f2b9c1/Main.java"),
    workDir,        // working directory
    30,             // 30 second timeout (compilation max time)
    false,          // enforceTimeLimit = false (compilation timing handle alag se)
    null,           // no stdin
    SandboxLimits.forCompile()  // memory limits for compiler
);
```

**Ye command actual OS level pe yahi hai:**
```
bwrap [...] -- javac -J-Xms16m -J-Xmx256m /tmp/judge/a3f2b9c1/Main.java
```

### Compilation Steps (OS Level)

1. `javac` (Java Compiler) `Main.java` file read karta hai
2. Parse karta hai — syntax check, type check
3. Agar sab sahi → `Main.class` bytecode file create karta hai same folder me
4. Agar koi error → stderr pe error message print karta hai (e.g., "Main.java:5: error: ';' expected")

### Compilation Result Handle

```java
if (compileResult.getExitCode() != 0) {
    // Exit code 0 = success, anything else = error
    String err = compileResult.getStderr();  // compiler error message
    return new ExecutionResult(
        "",     // stdout empty
        err,    // stderr = compilation error (user ko dikhega)
        0,      // timeTaken = 0 (compile hua hi nahi)
        0,      // exitCode
        1,      // compilationError flag = true
        false,  // timeLimitExceeded = false
        false,  // memoryLimitExceeded = false
        true    // compilationError = TRUE → parseOutput CE verdict dega
    );
}
```

### Compile Succeed → Ab Binary Run

```java
return runProcess(
    List.of("java", "-Xmx" + userMemMB + "m", "-cp", "/tmp/judge/a3f2b9c1", "Main"),
    workDir,
    (int) Math.ceil(timeLimitSeconds),  // e.g., 5 seconds
    true,                                // enforceTimeLimit = true
    stdin,
    SandboxLimits.forRun(asMemMB, cpuSec)
);
```

**Ye command OS level pe:**
```
bwrap [...] -- java -Xmx256m -cp /tmp/judge/a3f2b9c1 Main
```

- `-Xmx256m`: JVM ko max 256MB heap memory
- `-cp /tmp/judge/a3f2b9c1`: classpath — yahan se `Main.class` load karega
- `Main`: entry point class (contains `public static void main(String[] args)`)

---

## 3. Sandbox — What Is It?

Sandbox ek **virtual jail** hai. User ke code ko ek restricted environment me execute karte hain jahan wo:
- Internet access nahi kar sakta
- Server ki files nahi padh sakta
- Zyada memory nahi le sakta
- Zyada time nahi le sakta

Ye do tools se banta hai: **bwrap** + **prlimit**.

### bwrap (Bubblewrap)

Bubblewrap ek Linux tool hai jo **namespaces** (alag-alag "views") create karta hai. Har namespace ek alag duniya hai — uske andar ka process bahar ki duniya nahi dekh sakta.

**Technical term "namespace" ka simple matlab:** Jaise ek ghar me alag-alag rooms hote hain. Ek room me jo banda hai, wo doosre room me kya ho raha hai nahi dekh sakta. Namespace bhi aisa hi hai — process ek "room" me band hai.

### SandboxRunner.wrap() — Poora Command Build

```java
// Final command for Java execution:
bwrap \
  --ro-bind /usr /usr \        // /usr folder read-only mount
  --ro-bind /lib /lib \        // /lib libraries read-only
  --ro-bind /bin /bin \        // /bin commands read-only
  --ro-bind /etc /etc \        // /etc config read-only
  --bind /tmp/judge/a3f2b9c1 /tmp/judge/a3f2b9c1 \  // work dir read-write
  --chdir /tmp/judge/a3f2b9c1 \  // start in this directory
  --proc /proc \               // fresh /proc (process info)
  --dev /dev \                 // fresh /dev (null, zero, random, urandom)
  --tmpfs /tmp \               // temporary write space (RAM-based)
  --unshare-all \              // NAYA PID + NET + IPC + UTS namespace
  --uid 65534 --gid 65534 \    // run as "nobody" user (no privileges)
  --new-session \              // detached from terminal
  --die-with-parent \          // agar JVM mare to sab kuch kill
  --cap-drop ALL \             // saari Linux capabilities drop
  --clearenv \                 // environment variables clear
  --setenv PATH /usr/bin:/bin \
  --hostname sandbox \
  -- \                         // bwrap arguments end
  prlimit \                    // resource limiter starts
    --as=268435456 \           // virtual memory limit (256MB in bytes)
    --cpu=6 \                  // CPU seconds limit
    --nproc=64 \               // max 64 processes
    --fsize=16777216 \         // max file size (16MB in bytes)
    --nofile=64 \              // max 64 open files
    -- \                       // prlimit arguments end
  java -Xmx256m -cp /tmp/judge/a3f2b9c1 Main   // ACTUAL user command
```

### Each Flag Explained (Simple Language)

| Flag | Simple Meaning |
|------|---------------|
| `--ro-bind /usr /usr` | `/usr` folder dikhega, lekin sirf padh sakta hai, likh nahi sakta |
| `--bind /work/dir /work/dir` | Work folder dikhega, padh aur likh dono sakta hai |
| `--chdir /work/dir` | Yahan se start karo (jaise `cd /work/dir`) |
| `--proc /proc` | Naya `/proc` — host ke processes nahi dikhenge |
| `--dev /dev` | Minimal devices — sirf `/dev/null`, `/dev/zero` |
| `--tmpfs /tmp` | RAM-based temporary storage — reboot pe clear |
| `--unshare-all` | Naya PID space + no network + naya IPC |
| `--uid 65534` | User ID 65534 = "nobody" — koi permissions nahi |
| `--new-session` | Terminal se disconnected (Ctrl+C se kill nahi hoga) |
| `--die-with-parent` | JVM crash hua to sab kuch apne aap kill |
| `--cap-drop ALL` | Linux superpowers sab revoke (jaise root bhi kuch na kar sake) |
| `--clearenv` | Saare environment variables clear (JAVA_HOME, PATH, secrets sab gayab) |

### prlimit Flags

| Flag | Simple Meaning |
|------|---------------|
| `--as=268435456` | Max virtual memory: 256MB. Isse zyada maanga to ENOMEM error |
| `--cpu=6` | Max CPU seconds: 6. 6 second se zyada CPU use kiya to SIGXCPU signal |
| `--nproc=64` | Max processes/threads: 64. `fork()` bomb 65th time fail hoga |
| `--fsize=16777216` | Max file size: 16MB. Isse badi file banana impossible |
| `--nofile=64` | Max open files: 64. 65th `fopen()` fail hoga |

---

## 4. Process Execution — runProcess() Method

### Process Start

```java
List<String> wrapped = sandbox.wrap(command, workDir, limits);  // bwrap command build
ProcessBuilder pb = new ProcessBuilder(wrapped);
pb.directory(workDir.toFile());          // working directory
pb.redirectErrorStream(false);          // stdout/stderr alag-alag

Process process = pb.start();           // ← OS-level process create
```

**Kya hua OS level pe:** Java ne OS ko bola "ek naya process start karo". OS ne ek naya PID assign kiya, memory allocate ki, aur `bwrap` binary execute karna start kiya. bwrap ne apne namespaces set kiye, fir prlimit start kiya, fir actual `java Main` command start hua.

### Stdin Handling (Important!)

```java
// User code agar Scanner.nextInt() ya cin >> use karta hai to ye stdin close
// karne se turant EOF milta hai — code hang nahi hota
try {
    if (stdin != null && !stdin.isEmpty()) {
        process.getOutputStream().write(stdin.getBytes());  // input data bhejo
        process.getOutputStream().flush();
    }
    process.getOutputStream().close();  // stdin CLOSE → EOF → Scanner.nextInt() return karega
} catch (IOException ignored) {}
```

**Kyun zaroori:** Agar `Scanner.nextInt()` call kiya aur stdin open hai, to code FOREVER wait karega user input ke liye. Stdin close karne se Scanner ko turant `NoSuchElementException` milega — worker hang nahi hoga.

### Timer Start

```java
long startMs = System.currentTimeMillis();  // ← TIMER START
```

**Important (July 2026 fix):** Ye line `pb.start()` ke BAAD hai. Pehle pehle hota tha to bwrap startup ka 1-5s overhead bhi time me count ho jata tha. Ab sirf actual code execution time count hota hai.

### Output Reading — Concurrent Threads

```java
// stdout reader thread
Thread stdoutReader = new Thread(() -> {
    try (InputStream is = process.getInputStream()) {
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {       // -1 means EOF (process closed stdout)
            synchronized (stdout) {
                stdout.append(new String(buf, 0, n, UTF_8));
            }
        }
    } catch (IOException ignored) {}
}, "judge-stdout-reader");
stdoutReader.setDaemon(true);
stdoutReader.start();

// stderr reader thread (same pattern)
```

**Kyun do threads:** Agar hum pehle stdout padhe, fir stderr padhe, to deadlock ho sakta hai — process stderr pe likh rahi hai, hum stdout padh rahe hain, dono wait kar rahe hain. Do parallel threads se dono streams simultaneously read hoti hain.

### Wait For Process

```java
boolean finished = process.waitFor(timeLimitSeconds + 5, TimeUnit.SECONDS);
long elapsed = System.currentTimeMillis() - startMs;
```

`waitFor` block karta hai — JVM thread wait karta hai jab tak process exit na ho jaye, ya timeout na ho jaye. Timeout = `timeLimit + 5 seconds` (5 sec buffer for JVM startup). Agar timeout ho jaye (`finished = false`), to process kill karte hain.

### Process Kill If Timeout

```java
if (!finished) {
    killProcessTree(process);  // destroyForcibly() + descendants kill
    return new ExecutionResult(
        stdout.toString(),
        "Time Limit Exceeded",
        elapsed, 0, 1,
        true,   // timeLimitExceeded = true
        false,  // memoryLimitExceeded = false  
        false   // compilationError = false
    );
}
```

### Normal Exit — Check Exit Code

```java
int exitCode = process.exitValue();

// Check if wall-clock time exceeded limit
boolean isTle = enforceTimeLimit && elapsed > (timeLimitSeconds * 1000L);

if (isTle) {
    return new ExecutionResult(stdout.toString(), "Time Limit Exceeded",
        elapsed, 0, exitCode, true, false, false);
}

// Exit codes from kernel
if (exitCode == 137 || exitCode == 139) {  // 137=SIGKILL, 139=SIGSEGV
    String error = exitCode == 137 ? "Memory Limit Exceeded" : "Runtime Error";
    return new ExecutionResult(stdout.toString(), error, elapsed, 0, exitCode, 
        false, exitCode == 137, false);
}
```

**Exit Codes Explained:**
- **0**: Normal exit — process ne khud `return 0` kiya
- **137** = 128 + 9 (SIGKILL): Kisi ne `kill -9` kiya — usually OOM killer ya prlimit
- **139** = 128 + 11 (SIGSEGV): Segmentation fault — memory access violation (C/C++ pointer bug)
- **152** = 128 + 24 (SIGXCPU): CPU time limit exceeded — prlimit ne kill kiya
- **1**: General error — usually compile error ya uncaught exception

---

## 5. Result Flow Back — Worker to User

### parseOutput() — TC Lines Parse

```java
ParsedResult parseOutput(ExecutionResult result, boolean isTestRun) {
    // 1. Compile error?
    if (result.isCompilationError()) {
        return new ParsedResult(CE, result.getStderr(), 0, 0, 0, "[]");
    }
    
    // 2. TLE?
    if (result.isTimeLimitExceeded()) {
        return new ParsedResult(TLE, "Time Limit Exceeded", 0, 0, 0, "[]");
    }
    
    // 3. MLE?
    if (result.isMemoryLimitExceeded()) {
        return new ParsedResult(MLE, "Memory Limit Exceeded", 0, 0, 0, "[]");
    }
    
    // 4. Parse stdout for TC: lines
    String stdout = result.getStdout();
    for (String line : stdout.split("\n")) {
        if (!line.startsWith("TC:")) continue;
        
        // Parse: TC:1:PASS  or  TC:2:FAIL:input=...:expected=...:got=...
        int tcNum = parseTcNumber(line);
        boolean passed = line.contains(":PASS");
        boolean hidden = line.contains(":hidden");
        String input = extractField(line, "input");
        String expected = extractField(line, "expected");
        String got = extractField(line, "got");
    }
    
    int total  = allTcLines.size();
    int passed = (int) allTcLines.stream().filter(l -> l.passed).count();
    int score  = total > 0 ? (passed * 100 / total) : 0;
    
    SubmissionStatus status = (passed == total) ? AC : WA;
    
    return new ParsedResult(status, null, passed, total, score, buildTcDetailsJson());
}
```

### finalizeAndNotify() — Save + Push

```java
void finalizeAndNotify(SubmissionJob job, Long subId, SubmissionStatus status,
                       String error, int passed, int total, int score, 
                       long timeMs, String details) {
    
    // ── 1. DB Update ──
    int updated = submissionRepository.updateResult(
        subId, 
        List.of(PENDING, JUDGING),  // only update if in-flight
        status, error, passed, total, (double)timeMs, score, details, TimeUtil.now()
    );
    // SQL: UPDATE submissions SET status=?, score=?, ... 
    //      WHERE id=? AND status IN ('PENDING','JUDGING')
    // "updated" = number of rows affected (should be 1, 0 means already finalized)
    
    // ── 2. Leaderboard (contest only) ──
    if (!job.isTestRun() && job.getContestId() != null && updated > 0) {
        String key = "contest:score:" + contestId + ":" + userId + ":" + problemId;
        String prev = redis.opsForValue().get(key);    // previous score
        int prevScore = (prev != null) ? parseInt(prev) : 0;
        int delta = score - prevScore;                  // only the difference
        
        if (delta != 0) {
            redis.opsForValue().set(key, String.valueOf(score), Duration.ofDays(30));
            leaderboard.updateScore(contestId, userId, delta);
            // ZINCRBY leaderboard:contest:{id} {delta} {userId}
        }
    }
    
    // ── 3. Cache Eviction ──
    redis.delete("submissions:user:" + userId);
    redis.delete("submission:status:" + subId);
    
    // ── 4. SSE Push ──
    VerdictEvent event = new VerdictEvent(
        subId, status.name(), passed, total, score, timeMs, error, details,
        job.isTestRun(), job.isPractice(), pointsAwarded, solved
    );
    sseRegistry.sendVerdict(userId, event);
    // → SseEmitter.send() → browser receives event
    
    // ── 5. Job Cleanup ──
    redis.opsForList().remove("submission:processing:...", 0, jobJson);
}
```

---

## 6. SSE — Browser Tak Result Kaise Pahunchta Hai

### Ticket Exchange

```
Frontend:
  POST /api/submissions/sse-ticket
  → Backend: Valkey SET "sse:ticket:uuid123" "userId=42" EX 30s
  → Response: { "ticket": "uuid123" }

  const es = new EventSource("/api/submissions/stream?ticket=uuid123")
  es.addEventListener("verdict", handler)
```

### Backend SSE Registration

```java
// SseTicketService.consume()
String userId = redis.opsForValue().getAndDelete("sse:ticket:" + ticket);
// Get AND Delete in one atomic operation — ticket single-use

if (userId == null) return 401;  // expired/used ticket

// Register emitter for this user
SseEmitter emitter = new SseEmitter(300_000L);  // 5 minute timeout
sseRegistry.register(userId, emitter);
```

### Verdict Push

```java
// SseEmitterRegistry.sendVerdict()
SseEmitter emitter = emitters.get(userId);
if (emitter != null) {
    emitter.send(SseEmitter.event()
        .name("verdict")
        .data(objectMapper.writeValueAsString(verdictEvent)));
    // → Browser receives: event { type: "verdict", data: "{...json...}" }
}
```

### Frontend Receives

```javascript
es.addEventListener("verdict", (e) => {
    const verdict = JSON.parse(e.data);
    // verdict = {
    //   submissionId: 12345,
    //   status: "AC",
    //   testCasesPassed: 6,
    //   totalTestCases: 6,
    //   score: 100,
    //   timeConsumedMs: 245,
    //   testCaseDetails: "[{...}]",
    //   testRun: false,
    //   practice: true,
    //   pointsAwarded: 5,
    //   alreadySolved: false
    // }
    
    showVerdictOnScreen(verdict);
});
```

---

## 7. Polling Fallback — Jab SSE Kaam Na Kare

### Frontend Poll Loop

```javascript
const pollVerdict = async (submissionId, attempts = 0) => {
    if (attempts > 30) {
        showError("Judging timed out — try again");
        return;
    }
    
    const res = await api.get(`/api/submissions/${submissionId}/status`);
    const data = res.data;
    
    if (data.status === 'PENDING' || data.status === 'JUDGING') {
        // Still in progress — wait and retry
        setTimeout(() => pollVerdict(submissionId, attempts + 1), 2000);
        return;
    }
    
    // Final verdict — show result
    showVerdictOnScreen(data);
};
```

### Backend Poll Endpoint

```java
@GetMapping("/{id}/status")
public ResponseEntity<?> getSubmissionStatus(@PathVariable Long id) {
    // Try submissions table first
    Submission sub = submissionRepository.findById(id).orElse(null);
    
    // Not found → try practice_submissions table
    if (sub == null) {
        PracticeSubmission psub = practiceSubmissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException());
        return buildResponse(psub);
    }
    
    return buildResponse(sub);
}
```

**July 2026 Fix:** Pehle sirf `submissions` table check hota tha. Practice submissions `practice_submissions` table me save hote hain — to polling hamesha 404 deta tha. Ab dono tables check hote hain.

---

## 8. Complete Execution Timeline

```
T=0ms      worker: processJob() start
T=1ms      DB: UPDATE submissions SET status='JUDGING'
T=2ms      CacheService.getSnippetHarness() → Valkey HIT (0.5ms)
T=3ms      injectUserCode() → code between markers
T=4ms      DockerJudgeService.execute()
                │
                │  Temporary folder create: /tmp/judge/a3f2b9c1/
                │  File write: Main.java
                │
T=5ms           ProcessBuilder.start() → OS creates process
                │
                │  bwrap starts → sets up namespaces (~10ms)
                │  prlimit starts → sets limits (~2ms)
                │
T=20ms          javac compiles Main.java → Main.class created (~200ms)
                │
T=220ms         java Main runs → harness executes test cases
                │    TC:1:PASS
                │    TC:2:PASS  
                │    TC:3:FAIL:input=...:expected=...:got=...
                │    TC:4:PASS:hidden
                │    ...
                │
T=450ms         Process exits → exit code 0
                │
                │  stdoutReader thread captures all output
                │  elapsed = 445ms
                │
T=455ms    parseOutput() → WA (3/6 passed, score=50)
T=456ms    finalizeAndNotify()
                │
                │  DB UPDATE → 1 row updated
                │  Leaderboard → ZINCRBY +50
                │  Cache eviction
                │
T=458ms         sseRegistry.sendVerdict()
                    │
                    │  emitter.send("verdict", {...json...})
                    │
T=459ms              BROWSER RECEIVES EVENT
                     │
                     │  verdict handler runs
                     │  UI updates: "WA — 3/6 passed (50%)"
```

**Total time: ~460ms** for a typical Java submission. SSE delivery: <1ms after DB update.

---

## 9. Cleanup

```java
finally {
    cleanup(workDir);  // delete /tmp/judge/a3f2b9c1/ folder
}
```

```java
void cleanup(Path dir) {
    try {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())  // files first, then folders
            .forEach(p -> { try { Files.delete(p); } catch (Exception e) {} });
    } catch (Exception e) {
        log.warn("Cleanup failed: {}", e.getMessage());
    }
}
```

Har execution ke baad temporary folder delete ho jata hai. Koi leftover files nahi rehti.

---
