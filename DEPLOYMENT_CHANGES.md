# Deployment Changes — Production Hardening Rollout

This document lists everything **you** (the operator) need to do to get the
Blocker + High fixes live. Code changes are already merged. Items below are
infrastructure, secrets, env vars, database, and verification steps.

Work top to bottom. Each section is self-contained.

---

## 1. Pre-Deployment — Host Setup

### 1.1 Install sandbox tooling on every host that runs the JVM

The judge now wraps every user-code execution in `bwrap` + `prlimit`. The app
fails at startup if either binary is missing.

```bash
# Debian / Ubuntu
sudo apt-get update
sudo apt-get install -y bubblewrap util-linux

# RHEL / Rocky / Alma
sudo dnf install -y bubblewrap util-linux

# Verify
which bwrap prlimit
bwrap --version
```

### 1.2 Verify unprivileged user namespaces are enabled

Most modern distros have this on by default. Check:

```bash
sysctl kernel.unprivileged_userns_clone
# Expected: kernel.unprivileged_userns_clone = 1
```

If it returns `0`, enable it persistently:

```bash
echo 'kernel.unprivileged_userns_clone=1' | sudo tee /etc/sysctl.d/99-userns.conf
sudo sysctl --system
```

### 1.3 Quick sandbox smoke test (run as the JVM user, not root)

```bash
sudo -u <jvm-user> bash -c '
  bwrap --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 \
        --ro-bind /bin /bin --ro-bind /etc /etc --proc /proc --dev /dev \
        --tmpfs /tmp --unshare-all --uid 65534 --gid 65534 \
        --new-session --die-with-parent --cap-drop ALL --clearenv \
        --setenv PATH /usr/bin:/bin -- echo "sandbox OK"
'
```

If you see `sandbox OK`, you're set. If you see `bwrap: Creating new namespace failed`, fix 1.2.

---

## 2. Secrets Rotation

Anything that ever sat in this workspace's `.env` file should be considered
exposed. Rotate before the new build goes live.

### 2.1 JWT secret

Sessions issued with the old secret will be invalidated — every user has to log in again.

```bash
# Generate
openssl rand -hex 32 | base64
```

Update `JWT_SECRET` in production env.

### 2.2 Database password

```bash
# In your Postgres console
ALTER USER codecombat WITH PASSWORD '<new-strong-password>';
```

Update `DB_PASSWORD` in production env.

### 2.3 Valkey / Redis password

If using a managed Valkey/Redis with auth, rotate it through the provider's UI
and update `REDIS_PASSWORD`.

### 2.4 Gmail SMTP app password

1. Sign in to https://myaccount.google.com/
2. Security → App passwords
3. Revoke the old `MAIL_PASSWORD`
4. Generate a new one
5. Update `MAIL_PASSWORD` in production env

### 2.5 SSH / cloud keys still in the workspace

`cc-vm_key.pem`, `mandalp166@gmail.com-2026-05-17T00_55_23.731Z.pem`,
`ssh-key-2026-05-17(7).key` are gitignored but still on disk. Decide:

- **Keep but lock down:** `chmod 600 *.pem *.key && mv *.pem *.key ~/.ssh/`
- **Rotate them entirely** if any of them grant access to live infrastructure.
  In your cloud console: revoke the old public key from `authorized_keys`
  on the VM, generate a new one, replace.

---

## 3. New Required Environment Variables

Add these to your production env (`.env`, Vercel/Render dashboard, k8s
ConfigMap, etc.). Defaults exist for everything **except `APP_ALLOWED_ORIGINS`**
which you must set explicitly in production.

### 3.1 Required

```bash
# CORS — exact origins only, no wildcards
APP_ALLOWED_ORIGINS=https://codecombat.live,https://www.codecombat.live
```

### 3.2 Strongly recommended

```bash
# Sandbox — fail-fast checks at startup
SANDBOX_ENABLED=true
SANDBOX_BWRAP_PATH=/usr/bin/bwrap
SANDBOX_PRLIMIT_PATH=/usr/bin/prlimit

# Worker tuning
JUDGE_WORKERS=8
JUDGE_STUCK_JOB_TIMEOUT_MINUTES=5
JUDGE_RECLAIM_INTERVAL_MS=60000

# Public compiler tuning
COMPILER_WORKERS=6
COMPILER_QUEUE_SIZE=100
COMPILER_WS_MAX_SESSIONS=20
COMPILER_WS_MAX_RUNTIME_SEC=120
COMPILER_WS_RATE_WINDOW_SEC=60
COMPILER_WS_RATE_LIMIT_ANON=5

# Practice tuning
PRACTICE_WORKERS=4
PRACTICE_QUEUE_SIZE=50

# JPA — keep at validate in prod, never `update`
JPA_DDL_AUTO=validate

# Logging — leave at WARN; bump to INFO for incident triage
LOG_LEVEL_APP=WARN
```

### 3.3 Vercel / frontend

Nothing changes. `VITE_API_URL` continues to drive the frontend's API base.

---

## 4. Database — Flyway Baseline

The first deploy of this build runs Flyway. Behavior depends on whether the
target DB already has the schema:

**Existing DB (the live prod DB created by `ddl-auto=update` before this
build existed):**
1. Flyway sees no `flyway_schema_history` table and the schema is non-empty,
   so it stamps the DB as already at **V1** (because
   `spring.flyway.baseline-on-migrate=true` + `spring.flyway.baseline-version=1`).
   `V1__baseline.sql` is recorded as `type=BASELINE` and **never re-run**
   against the existing schema.
2. `V2__drop_dead_scores.sql` runs and drops the unused `scores` table.
3. `ddl-auto=validate` then verifies every JPA entity matches the live columns
   and refuses to boot if anything drifts.

**Fresh DB (CI, brand-new prod, disaster-recovery target):**
1. Flyway sees an empty schema and no history table, runs `V1__baseline.sql`
   from empty (recorded as `type=SQL`).
2. Runs `V2__drop_dead_scores.sql` (dropping the table V1 just created — kept
   in V1 only so the migration log is honest about what was there originally).
3. Final schema is bit-for-bit identical to the existing-DB path, so JPA
   `validate` passes both ways.

This was verified end-to-end:
- Cloned live schema into a scratch DB → booted Spring → Flyway recorded
  `V1=BASELINE, V2=SQL`, scores dropped, no JPA validation errors.
- Wiped scratch DB to empty → booted Spring → Flyway recorded
  `V1=SQL, V2=SQL`, schema diff vs live is empty (modulo dropped scores).
- Booted against the actual live DB → 3 users / 11 submissions preserved,
  scores dropped cleanly, Flyway history populated as expected.

### 4.1 Backup first

```bash
pg_dump -U codecombat -h <db-host> codecombat > backup-pre-flyway.sql
```

### 4.2 Optional dry-run

If you want to see what Flyway is about to do without applying:

```bash
# Run the WAR with Flyway info instead of migrate (one-shot).
# In normal app boot, Flyway runs automatically.
java -jar target/codecombat2026.war \
  --spring.flyway.enabled=true \
  --spring.flyway.locations=classpath:db/migration \
  --spring.flyway.cleanDisabled=true \
  --spring.profiles.active=migrate-info
```

### 4.3 Confirm post-deploy

After the app boots, check the Flyway history table:

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history ORDER BY installed_rank;
```

Existing-DB upgrade path:
```
 1 | 1 | Schema captured from ddl-auto=update production state | BASELINE | t
 2 | 2 | drop dead scores                                      | SQL      | t
```

Fresh-install path:
```
 1 | 1 | baseline                                              | SQL      | t
 2 | 2 | drop dead scores                                      | SQL      | t
```

Either is correct. Both produce the same final schema.

### 4.4 Verify schema validation

Spring will refuse to boot if entities and DB columns disagree. If startup
fails with `SchemaManagementException`, the DB drift needs an explicit
migration. Add `V3__<your_change>.sql` and redeploy.

---

## 5. Reverse Proxy / CDN Settings

If you're behind nginx, Cloudflare, or a load balancer, double-check these.

### 5.1 SSE — disable buffering on `/api/submissions/stream`

```nginx
location /api/submissions/stream {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_buffering off;
    proxy_cache off;
    chunked_transfer_encoding off;
    proxy_read_timeout 600s;
}
```

Cloudflare: turn off "Auto Minify" + caching for that path; consider exempting
it from the WAF if you see false positives on `text/event-stream`.

### 5.2 WebSocket — preserve upgrade headers

```nginx
location /api/compiler/ws {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout 300s;
    proxy_send_timeout 300s;
}
```

### 5.3 Mask SSE / WS tickets in access logs (optional but nice)

The single-use ticket pattern means a leaked URL is useless past the first
open. Still, scrub them out of long-term log retention.

```nginx
log_format scrubbed '$remote_addr - $remote_user [$time_local] '
                    '"$request" $status $body_bytes_sent '
                    '"$http_referer" "$http_user_agent" '
                    'rt=$request_time';
# In server block:
access_log /var/log/nginx/access.log scrubbed;

# Strip ticket=... from logged request lines
map $request $request_no_ticket {
    ~*(.*?)(ticket=)[^&]+(.*) "$1$2REDACTED$3";
    default $request;
}
```

---

## 6. Frontend Deployment

### 6.1 Rebuild and deploy together with backend

The frontend now uses `POST /api/submissions/sse-ticket` and
`POST /api/compiler/ws-ticket`. If you deploy the new backend before the new
frontend, old browser sessions will see 401s on `/api/submissions/stream`
(old `?token=` flow no longer accepted).

```bash
cd frontend
npm ci
npm run build
# deploy dist/ to Vercel / Netlify / your CDN
```

### 6.2 Force users to reload (recommended)

After deploy, bump the cache key on `index.html` or send a server push that
clears stale tabs. Otherwise, users with a tab open from before the deploy
will hit 401 on their next submission until they refresh.

---

## 7. Post-Deploy Verification

Run these from a clean browser session.

### 7.1 CORS — only listed origins reach the API

```bash
# Should succeed
curl -i -H "Origin: https://codecombat.live" https://api.codecombat.live/api/health
# Look for: Access-Control-Allow-Origin: https://codecombat.live

# Should be rejected (no allow-origin header)
curl -i -H "Origin: https://attacker.example.com" https://api.codecombat.live/api/health
```

### 7.2 SSE auth invariant

```bash
# Without ticket → 401
curl -i https://api.codecombat.live/api/submissions/stream
# HTTP/1.1 401

# With invalid ticket → 401
curl -i "https://api.codecombat.live/api/submissions/stream?ticket=garbage"
# HTTP/1.1 401
```

### 7.3 Submit a test problem and confirm sandbox is active

Submit any problem with a malicious payload (in any language):

```python
# Python
import socket
s = socket.socket()
s.connect(('1.1.1.1', 80))   # should fail with sandbox
print(open('/etc/passwd').read())  # should show only the sandbox /etc
print(open('/home/<jvm-user>/.env').read())  # should fail FileNotFoundError
```

You should see `OSError`/`FileNotFoundError` in the verdict, not actual
network/host data.

### 7.4 Verify Flyway ran

```sql
SELECT version, description, type, success FROM flyway_schema_history;
-- Expected on existing-DB upgrade:
--   1 | Schema captured from ddl-auto=update production state | BASELINE | t
--   2 | drop dead scores                                      | SQL      | t
--
-- Expected on fresh install (CI / new prod):
--   1 | baseline                                              | SQL      | t
--   2 | drop dead scores                                      | SQL      | t

SELECT * FROM scores;
-- ERROR: relation "scores" does not exist
```

### 7.5 Worker pool durability

Submit something, then immediately `kill -9` the JVM mid-run. Restart.
Within `JUDGE_RECLAIM_INTERVAL_MS` (default 60s), `StartupRecoveryConfig`
should drain the orphan processing list back to the main queue and a
worker should finalise the verdict.

### 7.6 Multi-tab SSE

Open two tabs as the same user, submit on tab A. Both tabs should display the
verdict (multi-tab fan-out from the new `SseEmitterRegistry`).

### 7.7 Rate limiter fallback

Stop Valkey for 30 seconds while logged in, try to submit 10 times rapidly.
After ~5 in a row you should see "Too many submissions" — proves the local
fallback engaged. Restart Valkey after.

---

## 8. Optional — Tighten Further

These are not required to ship but worth doing soon.

### 8.1 Move PEM/SSH keys out of the project tree

```bash
mkdir -p ~/.codecombat-keys
chmod 700 ~/.codecombat-keys
mv cc-vm_key.pem mandalp166@*.pem ssh-key-*.key ~/.codecombat-keys/
chmod 600 ~/.codecombat-keys/*
```

The `.gitignore` is already aggressive (any `*.pem`, `*.key`, etc.) but if you
have a future contributor zipping the project to share, the keys go with the zip.

### 8.2 Add a Content-Security-Policy header

Reduces the blast radius of any XSS:

```nginx
add_header Content-Security-Policy "
  default-src 'self';
  script-src 'self' 'unsafe-inline' 'unsafe-eval';
  worker-src 'self' blob:;
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
  font-src 'self' https://fonts.gstatic.com;
  img-src 'self' data: https:;
  connect-src 'self' https://api.codecombat.live wss://api.codecombat.live;
  frame-ancestors 'none';
" always;
```

`unsafe-inline` and `unsafe-eval` are required by Monaco; can be removed if
you switch to Monaco's external-worker mode.

### 8.3 Set up a Prometheus scrape

The app already counts queue depth, active jobs, SSE connection count. To
expose them:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.prometheus.enabled=true
```

Then alert when `submission_queue_depth > 50 for 60s`.

---

## 9. Rollback Plan

If something goes wrong:

1. **App fails to start:** revert the WAR. Old build does NOT understand
   the new processing-list keys, but it'll keep BRPOP'ing the main queue,
   so in-flight is preserved.
2. **Sandbox blocks legitimate code:** flip `SANDBOX_ENABLED=false`,
   restart. Acknowledge the security tradeoff and investigate (likely a
   compiler binary in a non-default path; set `SANDBOX_BWRAP_PATH`).
3. **Flyway migration fails:** restore from `backup-pre-flyway.sql`. V1
   only ever runs against an empty schema (existing DBs see it as
   `BASELINE`), so the only meaningful risk is V2 dropping `scores`. The
   live DB has 0 rows in that table (entity was orphaned), so even V2 is
   non-destructive in practice. If V2 fails partway, you can manually
   `DELETE FROM flyway_schema_history WHERE version='2'` and re-run.
4. **CORS rejecting valid origins:** add the missing origin to
   `APP_ALLOWED_ORIGINS` and restart. No code change needed.

---

## 10. Quick Pre-Flight Checklist

Tick before you press deploy:

- [ ] `bwrap` and `prlimit` installed on every JVM host
- [ ] `kernel.unprivileged_userns_clone=1` confirmed
- [ ] Sandbox smoke test passes as the JVM user
- [ ] JWT secret rotated
- [ ] DB password rotated
- [ ] Valkey password rotated
- [ ] Gmail app password rotated
- [ ] SSH/cloud keys reviewed (rotated or moved out of workspace)
- [ ] `APP_ALLOWED_ORIGINS` set in production
- [ ] All other env vars from §3.2 set
- [ ] Database backup taken
- [ ] Reverse proxy SSE buffering disabled
- [ ] Reverse proxy WebSocket headers preserved
- [ ] Frontend rebuilt and ready to deploy alongside backend
- [ ] Monitoring on `/api/queue-status` (or Prometheus once §8.3 done)
- [ ] Rollback plan understood

Once all are ticked, deploy backend and frontend together. Verify §7. Done.
