# "Into the Web" — Full Implementation Plan

---

## Feature Overview

A web-based backend coding contest platform where users write real-world code
(Spring Boot controllers, Express routes, FastAPI endpoints) in a multi-file
editor and get instant feedback via automated JUnit/integration tests.

---

## What User Sees

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Into the Web → Spring Boot Challenge: "Build User CRUD"                │
├────────────────────────┬────────────────────────────────────────────────┤
│  FILE TREE (left)      │  EDITOR (right — tabs)                         │
│                        │                                                │
│  📁 src/main/java/... │  [UserController.java] ✏️                       │
│    📄 User.java      🔒│                                                │
│    📄 UserService.java🔒│  @RestController                              │
│    📄 UserRepo.java  🔒│  @RequestMapping("/api/users")                 │
│    📄 UserController ✏️│  public class UserController {                 │
│                        │      // user writes code here                  │
│  📄 Problem Statement │  }                                             │
│                        │                                                │
├────────────────────────┴────────────────────────────────────────────────┤
│  OUTPUT PANEL                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │ ✅ TC1: GET /api/users → 200 []                         PASS      ││
│  │ ✅ TC2: POST /api/users {name:"John"} → 201             PASS      ││
│  │ ✅ TC3: GET /api/users/1 → 200 {id:1,name:"John"}       PASS      ││
│  │ ❌ TC4: DELETE /api/users/1 → expected 204, got 500      FAIL      ││
│  │ ❌ TC5: GET /api/users/1 after delete → expected 404     FAIL      ││
│  │ ✅ TC6: POST invalid body → 400                          PASS      ││
│  └────────────────────────────────────────────────────────────────────┘│
│  Result: 4/6 passed | Score: 67                                         │
│                                                                         │
│  [Run (7/10)] [Submit (2/5)]                    Time: 02:34 remaining   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Infrastructure

```
┌─── VM1 (92.4.78.195) ── Main Platform ───────────────────────────┐
│  PostgreSQL 18 + Valkey 7 (shared state)                         │
│  Spring Boot API (contests, auth, users, practice)                │
│  nginx (TLS, routing)                                             │
│  ROLE: API server + DB + Cache + queue hub                        │
└───────────────────────────────────┬──────────────────────────────┘
                                    │ Private net 10.0.0.0/24
                                    │
┌───────────────────────────────────┼──────────────────────────────┐
│                                   │                              │
│  ┌────────────────────────────────▼─────────────────────────┐    │
│  │  VM3 (140.245.8.52, 6GB, 1 OCPU ARM)                    │    │
│  │  ROLE: Java/Spring Boot execution engine                 │    │
│  │  • Workers: 4-6 (512MB each, 60s timeout)                │    │
│  │  • Drains: web-contest:queue:JAVA                        │    │
│  │  • Tools: Java 21, Maven, bwrap, prlimit                 │    │
│  │  • Private IP: 10.0.0.228                                │    │
│  │  • Connects to VM1 Valkey + Postgres                     │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  VM4 (140.238.255.239, 1GB, 2 vCPU x86)                 │    │
│  │  ROLE: Node.js execution engine                          │    │
│  │  • Workers: 2-3 (200MB each, 30s timeout)                │    │
│  │  • Drains: web-contest:queue:NODEJS                      │    │
│  │  • Tools: Node 20, npm, bwrap, prlimit                   │    │
│  │  • Private IP: 10.0.0.187                                │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  VM5 (20.194.7.164, 1GB, Azure, 2 vCPU x86)             │    │
│  │  ROLE: Python/FastAPI execution engine                    │    │
│  │  • Workers: 2-3 (150MB each, 30s timeout)                │    │
│  │  • Drains: web-contest:queue:PYTHON                      │    │
│  │  • Tools: Python 3.11, pip, pytest, bwrap                │    │
│  │  • Connects to VM1 via PUBLIC IP (92.4.78.195)           │    │
│  └──────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────┘
```

---

## Queue Architecture

```
Same as existing contest system — per-language queue keys:

  web-contest:queue:JAVA      → VM3 workers drain (6GB, heavy)
  web-contest:queue:NODEJS    → VM4 workers drain (1GB, light)
  web-contest:queue:PYTHON    → VM5 workers drain (1GB, light)

Job JSON:
{
  "submissionId": 5001,
  "userId": 42,
  "contestId": 15,     // "Into the Web" contest ID
  "problemId": 3,
  "language": "JAVA",  // determines which queue
  "editableFiles": {
    "UserController.java": "user's code..."
  },
  "templateId": "spring-crud-users",
  "timeLimit": 60.0,
  "memoryLimit": 512,
  "isTestRun": false
}

Flow:
  1. User clicks Run/Submit
  2. VM1 API: save to DB (PENDING) + LPUSH to web-contest:queue:{lang}
  3. 202 returned immediately
  4. Worker on VM3/4/5: LMOVE (atomic claim)
  5. Write template + user code to temp dir
  6. bwrap sandbox: mvn test / npm test / pytest
  7. Parse TC output → verdict
  8. DB update + leaderboard + SSE push
  9. Cleanup
```

---

## Database Schema (New Tables)

```sql
-- Problem templates for "Into the Web"
CREATE TABLE web_contest_templates (
    id              BIGSERIAL PRIMARY KEY,
    problem_id      BIGINT NOT NULL REFERENCES problems(id),
    language        VARCHAR(20) NOT NULL,  -- JAVA, NODEJS, PYTHON
    template_zip    BYTEA NOT NULL,        -- complete project as ZIP
    manifest_json   TEXT NOT NULL,          -- file visibility rules
    test_count      INT NOT NULL DEFAULT 6,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- manifest_json example:
-- {
--   "editableFiles": ["src/main/java/.../controller/UserController.java"],
--   "readonlyFiles": ["src/main/java/.../entity/User.java", 
--                     "src/main/java/.../service/UserService.java"],
--   "hiddenFiles": ["src/test/java/.../ControllerTest.java", "pom.xml"],
--   "entryCommand": "mvn test -q -Dsurefire.useFile=false",
--   "timeoutSeconds": 60,
--   "memoryMB": 512
-- }

-- Reuse existing tables:
--   problems (title, description, level, time_limit, memory_limit)
--   submissions (user_id, problem_id, code, status, score, test_cases_passed)
--   contests (for "Into the Web" contests)
--   leaderboard (same ZSET approach in Valkey)
```

---

## API Design

```
── EXISTING (reuse as-is) ───────────────────────────────────────
POST /api/submissions          → submit code (queue-based)
POST /api/submissions/test     → run code (queue-based)
POST /api/submissions/sse-ticket → SSE ticket
GET  /api/submissions/stream    → SSE verdict delivery
GET  /api/submissions/{id}/status → polling fallback
GET  /api/contests              → list contests
GET  /api/leaderboard/{contestId} → contest leaderboard

── NEW ENDPOINTS ────────────────────────────────────────────────
GET  /api/web-contest/problems/{id}/template
     → Returns: { 
         editableFiles: { "UserController.java": "// write here" },
         readonlyFiles: { "User.java": "public class User {...}", ... },
         manifest: { testCount: 6, language: "JAVA" }
       }
     → NOTE: hiddenFiles NOT returned (user never sees them)

POST /api/web-contest/run
     → Body: { problemId, editableFiles: {"UserController.java": "code..."} }
     → Same as /submissions/test but for multi-file
     → Pushes to web-contest:queue:{lang}
     → Returns: 202 { submissionId }

POST /api/web-contest/submit
     → Body: same as /run
     → Same as /submissions but for multi-file
     → Returns: 202 { submissionId }

GET  /api/web-contest/problems/{id}/files
     → Returns visible files for the editor (readonly + editable)
```

---

## Execution Flow (Worker Side)

```
Step 1: Worker claims job from web-contest:queue:JAVA

Step 2: Load template
  - Fetch template_zip from DB (or cached on disk)
  - Unzip to /tmp/web-exec/{uuid}/

Step 3: Inject user code
  - For each file in job.editableFiles:
    - Overwrite the file in the unzipped project
  - Template's hidden test files remain untouched

Step 4: Execute in sandbox
  JAVA (Spring Boot):
    bwrap ... -- mvn test -q -Dsurefire.useFile=false 2>&1
    (compiles project + runs JUnit tests)
    Output: standard surefire console output

  NODEJS (Express):
    bwrap ... -- npm test 2>&1
    (runs jest/mocha tests against user's routes)

  PYTHON (FastAPI):
    bwrap ... -- pytest -v 2>&1
    (runs pytest against user's endpoints)

Step 5: Parse test output
  JUnit output → parse into TC:N:PASS/FAIL format
  OR: test files already print TC:N:PASS (we control the test code)

Step 6: Standard finalize (same as contest)
  - DB update (verdict, score)
  - Leaderboard delta update
  - SSE push to user
  - Cleanup temp dir
```

---

## Test File Design (Hidden from User)

```java
// src/test/java/com/example/ControllerTest.java (HIDDEN)

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ControllerTest {

    @Autowired TestRestTemplate rest;

    @Test @Order(1)
    void testGetAllUsersEmpty() {
        ResponseEntity<String> res = rest.getForEntity("/api/users", String.class);
        if (res.getStatusCode().value() == 200 && "[]".equals(res.getBody())) {
            System.out.println("TC:1:PASS");
        } else {
            System.out.println("TC:1:FAIL:expected=200 []:got=" + res.getStatusCode() + " " + res.getBody());
        }
    }

    @Test @Order(2)
    void testCreateUser() {
        HttpEntity<String> req = new HttpEntity<>("{\"name\":\"John\"}", jsonHeaders());
        ResponseEntity<String> res = rest.postForEntity("/api/users", req, String.class);
        if (res.getStatusCode().value() == 201) {
            System.out.println("TC:2:PASS");
        } else {
            System.out.println("TC:2:FAIL:expected=201:got=" + res.getStatusCode());
        }
    }

    // ... TC:3 through TC:6
    // ALL output same TC:N:PASS/FAIL format as existing system!
}
```

**Key:** Tests print `TC:N:PASS/FAIL` to stdout — same protocol as existing judge. Existing `parseOutput()` works unchanged!

---

## Frontend Components

```
src/pages/WebContest.jsx (new)
├── File tree (left sidebar)
│   ├── Shows: readonlyFiles (🔒 icon, click opens in read-only tab)
│   ├── Shows: editableFiles (✏️ icon, click opens in editable tab)
│   └── Does NOT show: hiddenFiles
│
├── Monaco Editor (center)
│   ├── Multi-tab support
│   ├── readOnly mode per-tab (based on manifest)
│   ├── Syntax highlighting (Java/JS/Python)
│   └── localStorage auto-save (same as ProblemSolve)
│
├── Problem Statement (top or toggle panel)
│   └── Markdown rendered description
│
├── Output Panel (bottom)
│   ├── TC results: ✅/❌ per test case
│   ├── Description of what each TC tested
│   └── Compilation errors if any
│
└── Action Bar (bottom-right)
    ├── Run button (x/10 counter)
    ├── Submit button (x/5 counter)
    └── Timer (if timed contest)
```

---

## Resource Limits Per Language

| Language | RAM per execution | Timeout | Workers | VM |
|----------|------------------|---------|---------|-----|
| Java/Spring Boot | 512MB | 60s | 4-6 | VM3 (6GB) |
| Node.js/Express | 200MB | 30s | 2-3 | VM4 (1GB) |
| Python/FastAPI | 150MB | 30s | 2-3 | VM5 (1GB) |

```
Concurrent capacity:
  VM3: 6GB - 800MB(OS) = 5.2GB available → 8-10 Java executions
  VM4: 1GB - 400MB(OS) = 600MB available → 2-3 Node executions
  VM5: 1GB - 450MB(OS) = 550MB available → 2-3 Python executions

Total: ~13-16 concurrent users across all languages
```

---

## Implementation Timeline

```
Week 1: Backend
  Day 1: VM3 setup (Java 21 + Maven + bwrap + Spring Boot worker app)
  Day 2: VM4 setup (Node 20 + npm + bwrap + worker)
         VM5 setup (Python 3.11 + pip + pytest + bwrap + worker)
  Day 3: DB schema (web_contest_templates table + Flyway migration)
  Day 4: WebContestController (template fetch, run, submit endpoints)
  Day 5: WebContestWorkerPool (queue drain, template unzip, sandbox exec)
  Day 6: Test output parser (JUnit stdout → TC:N:PASS)
  Day 7: Admin: create problem template (upload ZIP + manifest)

Week 2: Frontend
  Day 1: WebContest page layout (file tree + Monaco + output panel)
  Day 2: Monaco multi-file (readonly/editable tabs from manifest)
  Day 3: File tree component (icons, click-to-open)
  Day 4: Run/Submit buttons + SSE verdict handling (reuse existing)
  Day 5: Output panel (TC results display, error formatting)
  Day 6: Contest list page ("Into the Web" section)
  Day 7: localStorage code persistence (same pattern as ProblemSolve)

Week 3: Polish + Content
  Day 1: Create first problem template (Spring Boot CRUD)
  Day 2: Create Node.js problem template (Express API)
  Day 3: Create Python problem template (FastAPI endpoints)
  Day 4: Admin UI for template management
  Day 5: Testing + edge cases (compile errors, timeouts, etc.)
  Day 6-7: Deploy to production, end-to-end test
```

---

## Problem Examples (Day 1 Content)

```
── Problem 1: Spring Boot CRUD (JAVA) ─────────────────────────
Title: "Build a User REST API"
Difficulty: EASY
Given: User entity, UserRepository, UserService
Task: Create UserController with:
  - GET /api/users → list all
  - POST /api/users → create (validate name not blank)
  - GET /api/users/{id} → get by id (404 if not found)
  - DELETE /api/users/{id} → delete (204 on success)
Tests: 6 test cases (CRUD + validation + 404)

── Problem 2: Express.js Routes (NODEJS) ──────────────────────
Title: "Build a Todo API"
Difficulty: EASY
Given: todoStore (in-memory array), validator middleware
Task: Create routes in routes/todos.js:
  - GET /todos → list all
  - POST /todos → add (validate title required)
  - PATCH /todos/:id → toggle complete
  - DELETE /todos/:id → remove
Tests: 6 test cases

── Problem 3: FastAPI Endpoints (PYTHON) ──────────────────────
Title: "Build a Book API"
Difficulty: EASY
Given: Book model (Pydantic), books_db (dict), dependencies
Task: Write endpoints in routers/books.py:
  - GET /books → list all
  - POST /books → create (validate ISBN format)
  - GET /books/{id} → get by id
  - PUT /books/{id} → update
Tests: 6 test cases
```

---

## Security Considerations

```
1. User code is ALWAYS sandboxed (bwrap):
   - No network (can't call external APIs)
   - No filesystem access outside project dir
   - UID 65534 (no privileges)
   - prlimit enforces RAM/CPU/process limits

2. Test files are NEVER exposed to user:
   - manifest.json marks them as hidden
   - API never returns hidden file content
   - Template ZIP is server-side only

3. User cannot modify non-editable files:
   - Backend only accepts files listed in editableFiles
   - Even if user sends extra files, they're ignored

4. Maven/npm dependency downloads:
   - Templates are PRE-BUILT (dependencies already in ZIP)
   - OR: shared Maven local repo on VM3 (cached .m2)
   - No internet in sandbox → must pre-cache deps
```

---

## Maven Dependency Caching Strategy

```
Problem: mvn test downloads internet dependencies. Sandbox has no internet.

Solution: Pre-warm .m2 cache on VM3:

  1. First time a template is uploaded:
     - Admin runs: mvn dependency:go-offline (outside sandbox)
     - This populates ~/.m2/repository/ with all JARs

  2. During execution:
     - bwrap: --ro-bind /home/ubuntu/.m2 /root/.m2
     - Maven sees cached deps → no download needed → offline mode works
     - Add: mvn test -o (offline flag)

  Similarly:
  - VM4: npm ci with cached node_modules in template ZIP
  - VM5: pip install into template's venv (pre-built)
```

---

## Summary

```
What's being built:
  Web-based backend coding contests where users write real API code
  (controllers, routes, endpoints) and get tested via hidden JUnit/Jest/pytest.

Architecture:
  Same queue system as existing contests. Language-specific queues.
  VM3 = Java, VM4 = Node, VM5 = Python. All connect to shared Valkey + Postgres.

User experience:
  Multi-file Monaco editor with locked/editable files.
  Hidden test files validate user's implementation.
  Same Run/Submit buttons, same TC:N:PASS scoring.

Timeline: 3 weeks (backend → frontend → content)
Cost: ₹0 (all VMs already available)
```
