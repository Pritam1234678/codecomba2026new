# CodeCombat 2026 — Deployment Infrastructure

---

## VM Summary Table

| VM | Public IP | Private IP | SSH Key | User | Cloud | RAM | CPU | OS |
|----|-----------|-----------|---------|------|-------|-----|-----|-----|
| VM1 | 92.4.78.195 | 10.0.0.221 | `oci_vm_key` | ubuntu | Oracle | 6 GB | 1 OCPU ARM | Ubuntu 22.04 |
| VM2 | 140.245.4.112 | 10.0.0.34 | `ssh-key-2026-05-17(7).key` | opc | Oracle | 10 GB | 2 OCPU ARM | Oracle Linux 9.7 |
| VM3 | 140.245.8.52 | 10.0.0.228 | `ssh-key-2026-05-17(3).key` | ubuntu | Oracle | 6 GB | 1 OCPU ARM | Ubuntu 22.04 |
| VM4 | 140.238.255.239 | 10.0.0.187 | `1gbvm.key` | ubuntu | Oracle | 1 GB | 2 vCPU x86 | Ubuntu 24.04 |
| VM5 | 20.194.7.164 | 10.1.1.4 | `cc-vm_key.pem` | azureuser | Azure | 1 GB | 2 vCPU x86 | Ubuntu 24.04 |

> All SSH keys are at `/mnt/hdd/CODE/codecomba2026new/`
> All Oracle VMs (VM1–VM4) are on the same private network `10.0.0.0/24` — sub-ms latency
> VM5 (Azure) is on a different cloud — connects to VM1 via public IP `92.4.78.195`

---

## VM1 — Main Platform Server

**Public IP:** `92.4.78.195`
**Private IP:** `10.0.0.221`
**SSH:** `ssh -i oci_vm_key ubuntu@92.4.78.195`

### What runs here
- **nginx** — TLS termination, reverse proxy for all traffic
  - `api.codecoder.in` → Spring Boot app on port 8080
  - `/api/practice/*` → load-balanced to VM1:8080 + VM2:8080 (round-robin)
  - `*.ide.codecoder.in` → wildcard proxy to VM3/VM4/VM5 code-server ports
- **Spring Boot app** (`~/app.war`) — main API server
  - JWT auth, contests, practice, leaderboard, duel, admin panel
  - Contest submission queue producer (LPUSH to Valkey)
  - SSE verdict delivery (SseEmitterRegistry)
  - Web IDE orchestrator (`/api/web-ide/session/*` → routes to VM3/4/5)
- **PostgreSQL 18** — primary database, listens on `0.0.0.0`
  - pg_hba allows: `10.0.0.0/24` + `20.194.7.164` (Azure VM5)
- **Valkey 7** (Redis fork) — listens on `0.0.0.0`
  - Password protected (see `.env` REDIS_PASSWORD)
  - iptables: only `10.0.0.0/24` + `20.194.7.164` can connect on port 6379
- **JUDGE_WORKERS=2** — contest submission workers
  - `SUBMISSION_QUEUE_MODE=BOTH` (default) — drains both `submission:queue`
    (public contests) and `private:submission:queue` (private contests),
    fair round-robin between the two
- **PRACTICE_WORKERS=2** — synchronous practice execution pool

### Key paths
- App WAR: `~/app.war`
- Env file: `~/.env`
- Repo: `~/codecombat`
- nginx config: `/etc/nginx/sites-enabled/api-codecoder`
- nginx IDE proxy: `/etc/nginx/sites-enabled/ide-codecoder`
- Valkey config: `/etc/valkey/valkey.conf`
- Postgres config: `/etc/postgresql/18/main/`
- TLS certs: `/etc/letsencrypt/live/api.codecoder.in/` and `/etc/letsencrypt/live/ide.codecoder.in/`
- Cloudflare token: `/root/.secrets/cloudflare.ini`

### Important env vars
```
DB_HOST=localhost
REDIS_HOST=localhost
JUDGE_WORKERS=2
SUBMISSION_QUEUE_MODE=BOTH
PRACTICE_WORKERS=2
WEB_CONTEST_WORKERS=1
```

---

## VM2 — Judge/Practice Execution Engine

**Public IP:** `140.245.4.112`
**Private IP:** `10.0.0.34`
**SSH:** `ssh -i "ssh-key-2026-05-17(7).key" opc@140.245.4.112`

### What runs here
- **Spring Boot app** (`/opt/codecombat/app.war`) — same WAR as VM1
- **JUDGE_WORKERS=6** — drains shared queues from VM1's Valkey
  - `SUBMISSION_QUEUE_MODE=BOTH` (default) — same fair round-robin as VM1,
    covers both public and private contest submissions
  - Contest submissions: LMOVE (atomic claim) → sandbox execute → write verdict DB → SSE
  - Janitor: reclaims stuck jobs every 60s
- **PRACTICE_WORKERS=4** — receives practice runs via nginx load-balance
- **WEB_CONTEST_WORKERS=2** — drains `web-contest:queue`
- **Sandbox:** bwrap + prlimit (same as VM1)
- **Tools installed:** Java 21, javac, gcc, g++, python3, node, npm, bwrap, prlimit

### Key paths
- App WAR: `/opt/codecombat/app.war`
- Env file: `/opt/codecombat/codecombat.env`
- Repo: `~/codecombat`
- Workspaces (IDE): `~/ide-workspaces/`
- Temp execution: `/tmp/web-exec/`

### Important env vars
```
DB_HOST=10.0.0.221
REDIS_HOST=10.0.0.221
JUDGE_WORKERS=6
SUBMISSION_QUEUE_MODE=BOTH
PRACTICE_WORKERS=4
WEB_CONTEST_WORKERS=2
```

---

## VM3 — "Into the Web" Java Engine + Private Contest Judge

**Public IP:** `140.245.8.52`
**Private IP:** `10.0.0.228`
**SSH:** `ssh -i "ssh-key-2026-05-17(3).key" ubuntu@140.245.8.52`

VM3 does **two independent jobs** on the same instance — they share nothing
(different Valkey queues, different worker pools) so they never interfere
with each other:

1. **"Into the Web" Java execution engine** (original role — unchanged)
2. **Dedicated private-contest code judge** (new — added for Private Contest
   Hosting feature, see below)

### What runs here

**Into the Web (Java problems):**
- **WEB_CONTEST_WORKERS=8** — drains `web-contest:queue` (Java problems)
  - WebContestWorkerPool: LMOVE → copy template → inject code → mvn test → TC parse
  - Janitor: reclaims stuck jobs every 60s
- **code-server** (VS Code in browser) — spawned on-demand per user session
  - Ports: 9001–9050 (one port per active session)
  - WebIdeService manages spawn/kill/test
  - Java extensions installed: redhat.java, vscjava.vscode-java-debug, vscjava.vscode-java-test, vscjava.vscode-maven
- **Maven .m2 cache** at `~/.m2/repository` (133MB, pre-warmed, offline mode)

**Private contest judge (dedicated, isolated execution):**
- **JUDGE_WORKERS=3**, **SUBMISSION_QUEUE_MODE=PRIVATE_ONLY** — this instance's
  `SubmissionWorkerPool` drains **only** `private:submission:queue`. It never
  touches the public `submission:queue`, so private-contest code execution
  (used by `PrivateContestSubmissionService`) never competes with public
  contest load on VM1/VM2, and vice versa.
  - Same sandbox (bwrap + prlimit), same DockerJudgeService, same leaderboard
    logic as VM1/VM2 — only the queue selection differs.
  - Leaderboard: writes to `private:leaderboard:{contestId}` (Valkey ZSET) via
    `SubmissionWorkerPool.finalizeAndNotify()`, same as the public-contest path
    but under the `private:` key prefix. Points are awarded once per
    (user, problem, contest) via `private:points:awarded:*` idempotency keys.
  - Janitor (`reclaimStuckJobs`, every 60s) reclaims stuck private jobs back
    onto `private:submission:queue` — routing is keyed off
    `job.privateContestId`, so it works correctly regardless of queue mode.
  - **Fallback**: VM1 and VM2 stay in `BOTH` mode, so if VM3 is ever down,
    private contest submissions still get judged (just alongside public load)
    instead of stalling. VM3 is dedicated capacity, not a single point of failure.
- **PRACTICE_WORKERS=1** (minimum, for ThreadPool init — practice mode isn't used here)

### Key paths
- App WAR: `/opt/codecombat/app.war`
- Env file: `/opt/codecombat/codecombat.env`
- Repo: `~/codecombat`
- IDE workspaces: `~/ide-workspaces/`
- Problem templates: `~/templates/`
  - `~/templates/spring-crud-users/` — Spring Boot CRUD challenge template
- Session logs: `~/ide-workspaces/{sessionId}.log`

### Important env vars
```
DB_HOST=10.0.0.221
REDIS_HOST=10.0.0.221
JUDGE_WORKERS=3
SUBMISSION_QUEUE_MODE=PRIVATE_ONLY
PRACTICE_WORKERS=1
WEB_CONTEST_WORKERS=8
WEB_IDE_WORKSPACE_BASE=/home/ubuntu/ide-workspaces
WEB_IDE_CODE_SERVER=code-server
WEB_IDE_PORT_MIN=9001
WEB_IDE_PORT_MAX=9050
```

### Verifying private-only mode
```bash
ssh -i "ssh-key-2026-05-17(3).key" ubuntu@140.245.8.52 \
  'sudo grep -E "JUDGE_WORKERS|SUBMISSION_QUEUE_MODE" /opt/codecombat/codecombat.env'
# JUDGE_WORKERS=3
# SUBMISSION_QUEUE_MODE=PRIVATE_ONLY
```
To confirm at runtime, check the startup log line on VM3 (requires `LOG_LEVEL_APP=INFO`
or `DEBUG` temporarily, default is WARN):
```
✅ Started 3 judge workers (instance=..., mode=PRIVATE_ONLY, queues=[private:submission:queue]) — ~30 jobs/min max
```

---

## VM4 — "Into the Web" Node.js Execution Engine

**Public IP:** `140.238.255.239`
**Private IP:** `10.0.0.187`
**SSH:** `ssh -i "1gbvm.key" ubuntu@140.238.255.239`

### What runs here
- **Spring Boot app** (`/opt/codecombat/app.war`)
- **WEB_CONTEST_WORKERS=2** — drains `web-contest:queue` (Node.js problems)
- **JUDGE_WORKERS=1**, **PRACTICE_WORKERS=1** (minimum)
- **code-server** — on-demand VS Code sessions for Node.js challenges
- **Swap:** 2GB swapfile at `/swapfile` (prevents OOM on 1GB RAM)
- **Tools:** Node 16, npm, code-server

### Key paths
- App WAR: `/opt/codecombat/app.war`
- Env file: `/opt/codecombat/codecombat.env`
- Workspaces: `~/ide-workspaces/`

### Important env vars
```
DB_HOST=10.0.0.221
REDIS_HOST=10.0.0.221
WEB_CONTEST_WORKERS=2
```

---

## VM5 — "Into the Web" Python Execution Engine (Azure)

**Public IP:** `20.194.7.164`
**Private IP:** `10.1.1.4` ← Azure, different cloud, NOT on Oracle private network
**SSH:** `ssh -i "cc-vm_key.pem" azureuser@20.194.7.164`

### What runs here
- **Spring Boot app** (`/opt/codecombat/app.war`)
- **WEB_CONTEST_WORKERS=2** — drains `web-contest:queue` (Python problems)
- **JUDGE_WORKERS=1**, **PRACTICE_WORKERS=1** (minimum)
- **code-server** — on-demand VS Code sessions for Python challenges
- **Tools:** Python 3.11, pip, pytest, code-server
- **ms-python extension** installed in code-server

### Important note
VM5 connects to VM1 via **public IP** (`92.4.78.195`), NOT private network.
VM1's iptables allows `20.194.7.164` on ports 5432 (Postgres) and 6379 (Valkey).
VM1's pg_hba.conf has entry for `20.194.7.164/32`.

### Key paths
- App WAR: `/opt/codecombat/app.war`
- Env file: `/opt/codecombat/codecombat.env`
- Workspaces: `~/ide-workspaces/`

### Important env vars
```
DB_HOST=92.4.78.195    ← public IP (Azure can't reach Oracle private net)
REDIS_HOST=92.4.78.195 ← public IP
WEB_CONTEST_WORKERS=2
```

---

## Network Architecture

```
                     INTERNET
                         │
               api.codecoder.in (TLS)
               *.ide.codecoder.in (TLS wildcard)
                         │
                   ┌─────▼──────┐
                   │    VM1     │  nginx reverse proxy
                   │ 92.4.78.195│  + Spring Boot API
                   │ 10.0.0.221 │  + PostgreSQL + Valkey
                   └─────┬──────┘
                         │ Private network 10.0.0.0/24 (<1ms)
          ┌──────────────┼──────────────┐
          │              │              │
   ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
   │    VM2      │ │    VM3      │ │    VM4      │
   │10.0.0.34    │ │10.0.0.228   │ │10.0.0.187   │
   │Judge workers│ │Java IDE +   │ │Node IDE     │
   │(6 pub+priv, │ │private judge│ │(2 workers + │
   │ 4 practice) │ │(8 web-cont +│ │ code-server)│
   │             │ │ 3 priv-only)│ │             │
   └─────────────┘ └─────────────┘ └─────────────┘
                         │
                   (public internet)
                         │
                   ┌─────▼──────┐
                   │    VM5     │  Azure cloud
                   │20.194.7.164│  Python IDE
                   │            │  (2 workers +
                   │            │   code-server)
                   └────────────┘
```

---

## Shared Valkey Queue Keys

| Queue Key | Drained By | Purpose |
|-----------|-----------|---------|
| `submission:queue` | VM1 (2, BOTH), VM2 (6, BOTH) | Public contest submissions + test runs |
| `private:submission:queue` | VM1 (2, BOTH), VM2 (6, BOTH), **VM3 (3, PRIVATE_ONLY)** | Private contest submissions (Private Contest Hosting feature) |
| `web-contest:queue` | VM2 (2), VM3 (8), VM4 (2), VM5 (2) | Into the Web run/submit |

`SUBMISSION_QUEUE_MODE` controls which of the first two rows a given
`SubmissionWorkerPool` instance drains (see `SubmissionWorkerPool.java`):
- `BOTH` (VM1, VM2) — fair round-robin across both queues.
- `PRIVATE_ONLY` (VM3) — drains only `private:submission:queue`, dedicating
  VM3's CPU/sandbox slots to private contests so they never compete with
  public contest load, and vice versa. VM3 is *additional* capacity — VM1/VM2
  still service the private queue too, so a VM3 outage degrades to
  "private and public share workers" rather than "private contests stop".

---

## Wildcard IDE Subdomain Routing

```
https://java-9001.ide.codecoder.in   → VM3 (10.0.0.228) port 9001
https://java-9002.ide.codecoder.in   → VM3 (10.0.0.228) port 9002
https://python-9001.ide.codecoder.in → VM4 (10.0.0.187) port 9001
https://node-9001.ide.codecoder.in   → VM5 (20.194.7.164) port 9001
```

nginx regex: `~^(?<cs_vmkey>[a-z]+)-(?<cs_port>[0-9]+)\.ide\.codecoder\.in$`
TLS cert: `/etc/letsencrypt/live/ide.codecoder.in/` (wildcard, auto-renew via Cloudflare DNS-01)

---

## Deploy Commands

### Deploy VM1 + VM2 (main platform)
```bash
./deploy_all.sh              # both VM1 + VM2
./deploy_all.sh --vm1        # VM1 only
./deploy_all.sh --vm2        # VM2 only
```

### Deploy VM3 (Java IDE engine + private-contest judge)
```bash
ssh -i "ssh-key-2026-05-17(3).key" ubuntu@140.245.8.52 \
  'cd ~/codecombat && git pull origin main && \
   ./mvnw -q -Dmaven.test.skip=true clean package && \
   sudo cp target/*.war /opt/codecombat/app.war && \
   sudo systemctl restart codecombat'
```
> Note: use `-Dmaven.test.skip=true` (not `-DskipTests`) — it skips test
> *compilation* too, which matters if test sources are temporarily out of
> sync with a service signature.
>
> `SUBMISSION_QUEUE_MODE=PRIVATE_ONLY` and `JUDGE_WORKERS=3` live in
> `/opt/codecombat/codecombat.env` and are **not** touched by this deploy
> command — they only need to be set once (see VM3 section above) and persist
> across redeploys since the WAR is replaced but the env file is not.

### Deploy VM4 (Node IDE engine)
```bash
# Build locally (1GB RAM too small for Maven) then SCP
./mvnw -q -DskipTests clean package
scp -i "1gbvm.key" target/*.war ubuntu@140.238.255.239:/opt/codecombat/app.war
ssh -i "1gbvm.key" ubuntu@140.238.255.239 'sudo systemctl restart codecombat'
```

### Deploy VM5 (Python IDE engine)
```bash
ssh -i "cc-vm_key.pem" azureuser@20.194.7.164 \
  'cd ~/codecombat && git pull origin main && \
   ./mvnw -q -DskipTests clean package && \
   sudo cp target/*.war /opt/codecombat/app.war && \
   sudo systemctl restart codecombat'
```

---

## Quick Health Check
```bash
# VM1 (public)
curl https://api.codecoder.in/api/health

# VM1 local
ssh -i oci_vm_key ubuntu@92.4.78.195 'curl -s localhost:8080/api/health'

# VM2
ssh -i "ssh-key-2026-05-17(7).key" opc@140.245.4.112 'curl -s localhost:8080/api/health'

# VM3
ssh -i "ssh-key-2026-05-17(3).key" ubuntu@140.245.8.52 'curl -s localhost:8080/api/health'

# VM4
ssh -i "1gbvm.key" ubuntu@140.238.255.239 'curl -s localhost:8080/api/health'

# VM5
ssh -i "cc-vm_key.pem" azureuser@20.194.7.164 'curl -s localhost:8080/api/health'

# Test wildcard IDE
curl -s -o /dev/null -w "%{http_code}" https://java-9001.ide.codecoder.in/
```

---

## Services on Each VM

All VMs run: `sudo systemctl status codecombat`

| Service | VM | Notes |
|---------|-----|-------|
| `codecombat` | All VMs | Spring Boot WAR |
| `valkey` | VM1 | Valkey 7 cache + queue |
| `postgresql` | VM1 | PostgreSQL 18 |
| `nginx` | VM1 | Reverse proxy + TLS |

---

## Private Contest Hosting Feature

Added on top of the existing public contest / practice / Into-the-Web
infrastructure. No new VMs — reuses VM1's PostgreSQL and Valkey, and VM3 is
repurposed to also run a dedicated private-contest judge (see VM3 section).

### Data model (all on VM1's PostgreSQL, same DB as everything else)
- `contest_hosting_requests` — admin-approved requests to become a host (V15)
- `private_contests` — 1:1 extension of `contests` with host + proctoring flag (V15)
- `private_contest_invitations` — per-contest invite tokens (V15)
- `private_contest_participants` — who joined which private contest (V15)
- `audit_logs` — compliance log for hosting/contest/participant lifecycle events (V17)

Migrations V15–V17 are Flyway-managed like everything else — they run
automatically on VM1's first boot after a deploy (VM1 owns the schema; VM2/VM3
only validate against it, per the existing `deploy_all.sh` convention).

### Submission flow (private contest)
```
Participant submits code
        │
        ▼
PrivateContestSubmissionController.submitCode()
        │  (rate limit + 5-submit cap checked here)
        ▼
PrivateContestSubmissionService.submitCode()
        │  - validates participant + LIVE status + problem-in-contest
        │  - creates Submission row (status=PENDING)
        │  - builds SubmissionJob with privateContestId set
        ▼
LPUSH private:submission:queue   (Valkey, on VM1)
        │
        ├── VM1 workers (BOTH mode, shared with public queue)
        ├── VM2 workers (BOTH mode, shared with public queue)
        └── VM3 workers (PRIVATE_ONLY mode, dedicated)  ← primary consumer
                │
                ▼
        SubmissionWorkerPool.processJob()
                │  - DockerJudgeService.execute() under bwrap+prlimit sandbox
                │  - writes verdict to `submissions` table
                │  - finalizeAndNotify(): private:leaderboard:{contestId} ZSET update,
                │    private:points:awarded:* idempotent point award, SSE verdict push
                ▼
        Participant sees verdict via SSE (/api/submissions/stream)
        Leaderboard visible via GET /api/contests/private/{id}/leaderboard
```

### Key files
- `SubmissionWorkerPool.java` — queue-mode-aware worker pool (`SUBMISSION_QUEUE_MODE`)
- `PrivateContestSubmissionService.java` — validates + queues private submissions
- `PrivateContestLeaderboardService.java` — reads `private:leaderboard:{contestId}` ZSET
- `db/migration/V15__create_private_contest_tables.sql`
- `db/migration/V17__create_audit_logs_table.sql`

### Verifying the deploy end-to-end
```bash
# 1. Confirm migrations landed on VM1
ssh -i oci_vm_key ubuntu@92.4.78.195 \
  "PGPASSWORD=postgres psql -h localhost -U postgres -d codecombat -tAc \
   \"SELECT version, success FROM flyway_schema_history WHERE version IN ('15','16','17');\""

# 2. Confirm VM3 is in PRIVATE_ONLY mode
ssh -i "ssh-key-2026-05-17(3).key" ubuntu@140.245.8.52 \
  'sudo grep -E "JUDGE_WORKERS|SUBMISSION_QUEUE_MODE" /opt/codecombat/codecombat.env'

# 3. Push a synthetic job onto the private queue and confirm it drains
#    (submissionId won't exist in DB, so the worker will no-op gracefully —
#    this only proves queue drain, not full end-to-end judging)
ssh -i oci_vm_key ubuntu@92.4.78.195 '
  PW=$(grep -E "^REDIS_PASSWORD=" ~/.env | cut -d= -f2)
  redis-cli -a "$PW" --no-auth-warning LPUSH private:submission:queue \
    "{\"submissionId\":1,\"userId\":1,\"problemId\":1,\"contestId\":1,\"code\":\"x\",\"language\":\"JAVA\",\"timeLimit\":5.0,\"memoryLimit\":256,\"testRun\":false,\"privateContestId\":1}"
  sleep 3
  redis-cli -a "$PW" --no-auth-warning LLEN private:submission:queue   # expect 0
'
```
