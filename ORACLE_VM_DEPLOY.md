# Oracle A1 Flex VM — Complete Deployment Guide
# CodeCombat 2026 — Spring Boot + React + PostgreSQL + Valkey

**VM Spec:** Oracle A1 Flex — 1 OCPU (2 ARM64 cores) / 6 GB RAM / Ubuntu 22.04 ARM64  
**Architecture:** aarch64 (ARM) — sab kuch ARM wala install karna hai  
**Domain assumption:** `codecoder.in` (apna domain daalo)

---

## Table of Contents

1. [Oracle Console Setup](#1-oracle-console-setup)
2. [VM me SSH karo](#2-vm-me-ssh-karo)
3. [System Update + Swap](#3-system-update--swap)
4. [Java 21 Install (ARM)](#4-java-21-install-arm)
5. [Language Runtimes Install](#5-language-runtimes-install)
6. [Sandbox Tools Install](#6-sandbox-tools-install)
7. [PostgreSQL Install](#7-postgresql-install)
8. [Valkey Install](#8-valkey-install)
9. [Node.js Install (Build ke liye)](#9-nodejs-install-build-ke-liye)
10. [Nginx Install + Config](#10-nginx-install--config)
11. [Project Deploy karo](#11-project-deploy-karo)
12. [Frontend Build + Deploy](#12-frontend-build--deploy)
13. [Backend WAR Build + Deploy](#13-backend-war-build--deploy)
14. [Environment Variables Setup](#14-environment-variables-setup)
15. [Systemd Service (Auto-start)](#15-systemd-service-auto-start)
16. [SSL Certificate (HTTPS)](#16-ssl-certificate-https)
17. [Oracle Firewall Rules](#17-oracle-firewall-rules)
18. [Verification](#18-verification)
19. [Useful Commands](#19-useful-commands)

---

## 1. Oracle Console Setup

Oracle Cloud Console me jaao:

### 1.1 Firewall Rules (Security List)
**Networking → Virtual Cloud Networks → apna VCN → Security Lists → Default Security List**

"Add Ingress Rules" me ye add karo:

| Source CIDR | Protocol | Port | Description |
|---|---|---|---|
| 0.0.0.0/0 | TCP | 22 | SSH |
| 0.0.0.0/0 | TCP | 80 | HTTP |
| 0.0.0.0/0 | TCP | 443 | HTTPS |

> **Note:** Port 8080 (Spring Boot) ko public mat karo — nginx ke through hi jaayega.

---

## 2. VM me SSH karo

```bash
# Apni local machine se
ssh -i /path/to/your-key.pem ubuntu@92.4.78.195

# Example (tera key file)
ssh -i cc-vm_key.pem ubuntu@92.4.78.195
```

---

## 3. System Update + Swap

```bash
# System update
sudo apt update && sudo apt upgrade -y

# Swap add karo (MUST — Oracle VM pe default swap nahi hota)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Verify swap
free -h
# Swap: 2.0G dikhna chahiye

# Unprivileged user namespaces enable karo (sandbox ke liye MUST)
echo 'kernel.unprivileged_userns_clone=1' | sudo tee /etc/sysctl.d/99-userns.conf
sudo sysctl --system

# Verify
sysctl kernel.unprivileged_userns_clone
# Output: kernel.unprivileged_userns_clone = 1
```

---

## 4. Java 21 Install (ARM)

**pom.xml me `<java.version>21</java.version>` hai — Java 21 chahiye.**  
Spring Boot 3.5.9 requires Java 17+ (using 21).

```bash
# Java 21 install (Ubuntu apt automatically ARM64 version deta hai)
sudo apt install -y openjdk-21-jdk

# Verify
java -version
# Output: openjdk version "17.x.x" ... aarch64

javac -version
# Output: javac 17.x.x

# Java path note karo (SandboxRunner ko chahiye)
which javac
# Output: /usr/bin/javac (symlink hai)

readlink -f $(which javac)
# Output: /usr/lib/jvm/java-21-openjdk-arm64/bin/javac
```

> **Important:** `/usr/lib/jvm/java-21-openjdk-arm64` — ye path `.env` me `SANDBOX_BWRAP_PATH` ke saath related hai. SandboxRunner automatically resolve kar leta hai.

---

## 5. Language Runtimes Install

**Judge in languages ko support karta hai: Java, C++, C, Python, JavaScript**

```bash
# C++ compiler
sudo apt install -y g++

# C compiler
sudo apt install -y gcc

# Python 3
sudo apt install -y python3

# Verify sab
g++ --version    # g++ (Ubuntu) 11.x.x aarch64
gcc --version    # gcc (Ubuntu) 11.x.x aarch64
python3 --version # Python 3.10.x

# Node.js (judge ke liye — JavaScript submissions)
# Ubuntu 22.04 pe Node 18 LTS install karo
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

node --version   # v18.x.x
```

---

## 6. Sandbox Tools Install

**SandboxRunner bwrap + prlimit use karta hai — ye MUST hai.**

```bash
# bubblewrap (bwrap)
sudo apt install -y bubblewrap

# prlimit util-linux me hota hai (usually pre-installed)
sudo apt install -y util-linux

# Verify
which bwrap
# /usr/bin/bwrap

which prlimit
# /usr/bin/prlimit

bwrap --version
# bubblewrap 0.x.x

# Sandbox smoke test (ubuntu user se run karo, root se nahi)
bwrap \
  --ro-bind /usr /usr --ro-bind /lib /lib --ro-bind /lib64 /lib64 \
  --ro-bind /bin /bin --ro-bind /etc /etc \
  --proc /proc --dev /dev --tmpfs /tmp \
  --unshare-all --uid 65534 --gid 65534 \
  --new-session --die-with-parent --cap-drop ALL --clearenv \
  --setenv PATH /usr/bin:/bin \
  -- echo "sandbox OK"

# Output: sandbox OK
```

---

## 7. PostgreSQL Install

```bash
# PostgreSQL 14 install
sudo apt install -y postgresql postgresql-contrib

# Service start + enable
sudo systemctl start postgresql
sudo systemctl enable postgresql

# postgres user ka password set karo
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'your_strong_password_here';"

# Database create karo
sudo -u postgres psql -c "CREATE DATABASE codecombat;"

# Verify
sudo -u postgres psql -c "\l"
# codecombat database dikhna chahiye

# PostgreSQL config — localhost connections allow karo
sudo nano /etc/postgresql/14/main/pg_hba.conf
# Ye line dhundo aur check karo:
# local   all   postgres   peer
# Isko change karo:
# local   all   postgres   md5

# Restart
sudo systemctl restart postgresql

# Test connection
psql -h localhost -U postgres -d codecombat -c "SELECT 1;"
# Password maangega — apna password daalo
```

---

## 8. Valkey Install

**App Valkey use karta hai submission queue ke liye. Valkey Redis-compatible hai — same protocol, same CLI.**

```bash
# Valkey install karo (official Valkey repo se)
curl -fsSL https://packages.valkey.io/valkey-signing.gpg | sudo gpg --dearmor -o /usr/share/keyrings/valkey-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/valkey-archive-keyring.gpg] https://packages.valkey.io/apt $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/valkey.list
sudo apt update
sudo apt install -y valkey

# Service start + enable
sudo systemctl start valkey
sudo systemctl enable valkey

# Verify
valkey-cli ping
# Output: PONG

# Config check — localhost only bind hona chahiye
grep "^bind" /etc/valkey/valkey.conf
# Output: bind 127.0.0.1 -::1
```

---

## 9. Node.js Install (Build ke liye)

**Frontend build karne ke liye Node.js chahiye. package.json me Vite 7 hai.**

```bash
# Node 18 already install kiya step 5 me
node --version   # v18.x.x
npm --version    # 9.x.x

# Agar nahi kiya:
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
```

---

## 10. Nginx Install + Config

**Nginx reverse proxy ka kaam karega — frontend serve karega + backend proxy karega.**

```bash
# Install
sudo apt install -y nginx

# Start + enable
sudo systemctl start nginx
sudo systemctl enable nginx

# Config file banao
sudo nano /etc/nginx/sites-available/codecombat
```

**Nginx config (paste karo):**

```nginx
server {
    listen 80;
    server_name codecoder.in www.codecoder.in;
    # SSL ke baad ye redirect ho jaayega — abhi HTTP se start karo

    # Frontend static files
    root /var/www/codecombat;
    index index.html;

    # React Router — sab routes index.html pe jaayein
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }

    # SSE endpoint — buffering band karo (CRITICAL)
    location /api/submissions/stream {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding off;
        proxy_read_timeout 600s;
    }

    # WebSocket — interactive compiler
    location /api/compiler/ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
```

```bash
# Site enable karo
sudo ln -s /etc/nginx/sites-available/codecombat /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default  # default site remove karo

# Config test karo
sudo nginx -t
# Output: syntax is ok / test is successful

# Reload
sudo systemctl reload nginx

# Frontend ke liye directory banao
sudo mkdir -p /var/www/codecombat
sudo chown ubuntu:ubuntu /var/www/codecombat
```

---

## 11. Project Deploy karo

**Do options hain:**

### Option A: Git se clone (Recommended)

```bash
# VM pe
cd ~
git clone https://github.com/your-username/codecombat2026.git
cd codecombat2026
```

### Option B: SCP se copy karo (local machine se)

```bash
# Local machine se run karo
scp -i cc-vm_key.pem -r /mnt/hdd/CODE/codecomba2026new ubuntu@92.4.78.195:~/codecombat2026
```

---

## 12. Frontend Build + Deploy

**Build local machine pe karo (VM pe Node memory kam hai), ya VM pe bhi ho sakta hai.**

### VM pe build karo:

```bash
cd ~/codecombat2026/frontend

# .env.production update karo — apna domain daalo
nano .env.production
```

```bash
# .env.production content:
VITE_API_URL=https://codecoder.in/api
```

```bash
# Dependencies install
npm ci

# Production build
npm run build

# dist/ folder ban jaayega
ls dist/
# index.html, assets/ dikhna chahiye

# Nginx ke liye copy karo
cp -r dist/* /var/www/codecombat/

# Verify
ls /var/www/codecombat/
# index.html, assets/ dikhna chahiye
```

---

## 13. Backend WAR Build + Deploy

**Build local machine pe karo (Maven + Java 21 chahiye), phir WAR copy karo.**

### Local machine pe (teri Kali Linux):

```bash
cd /mnt/hdd/CODE/codecomba2026new

# Tests skip karke build karo
./mvnw -DskipTests clean package

# WAR file ban jaayega
ls target/codecombat2026-0.0.1-SNAPSHOT.war
```

### WAR VM pe copy karo:

```bash
# Local machine se
scp -i cc-vm_key.pem \
  target/codecombat2026-0.0.1-SNAPSHOT.war \
  ubuntu@92.4.78.195:~/codecombat2026/app.war
```

---

## 14. Environment Variables Setup

```bash
# VM pe .env file banao
nano ~/codecombat2026/.env
```

```bash
# ─── Database ────────────────────────────────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=codecombat
DB_USERNAME=postgres
DB_PASSWORD=your_strong_postgres_password_here   # Step 7 me set kiya tha

# ─── JWT ─────────────────────────────────────────────────────────────────────
# Naya secret generate karo:
# openssl rand -hex 32
JWT_SECRET=<openssl rand -hex 32 se generate karo>
JWT_EXPIRATION=86400000

# ─── Valkey / Redis ───────────────────────────────────────────────────────────
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USERNAME=
REDIS_PASSWORD=

# ─── Email ───────────────────────────────────────────────────────────────────
MAIL_USERNAME=mandalpritam756@gmail.com
MAIL_PASSWORD=ttmj uuwm gswx tdcu

# ─── App ─────────────────────────────────────────────────────────────────────
APP_URL=https://codecoder.in
SERVER_PORT=8080

# ─── CORS ────────────────────────────────────────────────────────────────────
APP_ALLOWED_ORIGINS=https://codecoder.in,https://www.codecoder.in

# ─── Judge Workers (1 OCPU ke liye 2 workers) ────────────────────────────────
JUDGE_WORKERS=2
JUDGE_STUCK_JOB_TIMEOUT_MINUTES=5
JUDGE_RECLAIM_INTERVAL_MS=60000

# ─── Compiler ────────────────────────────────────────────────────────────────
COMPILER_WORKERS=2
COMPILER_WS_MAX_SESSIONS=10
COMPILER_WS_MAX_RUNTIME_SEC=120

# ─── Practice ────────────────────────────────────────────────────────────────
PRACTICE_WORKERS=1

# ─── Sandbox ─────────────────────────────────────────────────────────────────
SANDBOX_ENABLED=true
SANDBOX_BWRAP_PATH=/usr/bin/bwrap
SANDBOX_PRLIMIT_PATH=/usr/bin/prlimit

# ─── JPA ─────────────────────────────────────────────────────────────────────
JPA_DDL_AUTO=validate

# ─── Logging ─────────────────────────────────────────────────────────────────
LOG_LEVEL_APP=WARN
```

```bash
# .env file secure karo
chmod 600 ~/codecombat2026/.env
```

---

## 15. Systemd Service (Auto-start)

**App ko systemd service ke roop me chalao — VM restart pe auto-start hoga.**

```bash
sudo nano /etc/systemd/system/codecombat.service
```

```ini
[Unit]
Description=CodeCombat 2026 Spring Boot Application
After=network.target postgresql.service valkey.service
Requires=postgresql.service valkey.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/codecombat2026
EnvironmentFile=/home/ubuntu/codecombat2026/.env
ExecStart=/usr/bin/java \
  -Xms256m \
  -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar /home/ubuntu/codecombat2026/app.war
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=codecombat

# Security hardening
NoNewPrivileges=yes
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
```

```bash
# Service enable + start karo
sudo systemctl daemon-reload
sudo systemctl enable codecombat
sudo systemctl start codecombat

# Status check karo
sudo systemctl status codecombat

# Logs dekho
sudo journalctl -u codecombat -f
# "Started Codecombat2026Application" dikhna chahiye
```

---

## 16. SSL Certificate (HTTPS)

```bash
# Certbot install karo
sudo apt install -y certbot python3-certbot-nginx

# Certificate lo (apna domain daalo)
sudo certbot --nginx -d codecoder.in -d www.codecoder.in

# Email maangega — apna email daalo
# Terms accept karo
# Redirect HTTP to HTTPS — Yes choose karo

# Auto-renewal test
sudo certbot renew --dry-run
```

Certbot automatically nginx config update kar deta hai HTTPS ke liye.

---

## 17. Oracle Firewall Rules

**Oracle VM pe iptables bhi hota hai — ye bhi open karna padta hai.**

```bash
# HTTP + HTTPS allow karo
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT

# Rules save karo (reboot ke baad bhi rahe)
sudo netfilter-persistent save

# Agar netfilter-persistent nahi hai:
sudo apt install -y iptables-persistent
sudo netfilter-persistent save
```

---

## 18. Verification

### 18.1 Health check

```bash
# Backend health
curl http://localhost:8080/api/health
# Output: {"status":"UP"}

# Nginx through
curl https://codecoder.in/api/health
# Output: {"status":"UP"}
```

### 18.2 Database check

```bash
# Flyway migration verify karo
psql -h localhost -U postgres -d codecombat -c \
  "SELECT version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;"

# Expected:
# 1 | Schema captured... | BASELINE | t
# 2 | drop dead scores   | SQL      | t
```

### 18.3 Sandbox test

```bash
# Java submission test karo — browser me login karke ek problem submit karo
# Error nahi aana chahiye: "prlimit: failed to execute javac"
```

### 18.4 Services status

```bash
sudo systemctl status codecombat
sudo systemctl status postgresql
sudo systemctl status valkey
sudo systemctl status nginx
```

---

## 19. Useful Commands

```bash
# App restart
sudo systemctl restart codecombat

# App logs (live)
sudo journalctl -u codecombat -f

# App logs (last 100 lines)
sudo journalctl -u codecombat -n 100

# Memory usage check
free -h
htop

# Disk usage
df -h

# PostgreSQL me jaao
psql -h localhost -U postgres -d codecombat

# Valkey check
valkey-cli ping
valkey-cli info memory

# Nginx reload (config change ke baad)
sudo nginx -t && sudo systemctl reload nginx

# SSL certificate renew
sudo certbot renew

# App update karna ho toh:
# 1. Naya WAR copy karo
scp -i cc-vm_key.pem target/codecombat2026-0.0.1-SNAPSHOT.war ubuntu@92.4.78.195:~/codecombat2026/app.war
# 2. Restart karo
sudo systemctl restart codecombat
```

---

## Summary — Kya Kya Install Karna Hai

| Software | Version | Kyun |
|---|---|---|
| OpenJDK 21 | 21.x ARM64 | Spring Boot 3.5.9 requires Java 21 (pom.xml) |
| g++ | 11.x | C++ submissions judge karne ke liye |
| gcc | 11.x | C submissions judge karne ke liye |
| python3 | 3.10.x | Python submissions ke liye |
| Node.js | 18.x LTS | JavaScript submissions + frontend build |
| bubblewrap | latest | Sandbox (bwrap) — user code isolation |
| util-linux | latest | prlimit — memory/CPU limits |
| PostgreSQL | 14.x | Main database |
| Valkey | 8.x | Submission queue — native Valkey |
| Nginx | latest | Reverse proxy + static files serve |
| Certbot | latest | SSL/HTTPS |

**Total install time: ~20-30 minutes**  
**Pehli baar deploy time: ~45-60 minutes**

---

## Quick Checklist

- [ ] Oracle Console me ports 80, 443 open kiye
- [ ] VM me SSH ho gaya
- [ ] System update + 2GB swap add kiya
- [ ] `kernel.unprivileged_userns_clone=1` set kiya
- [ ] Java 21 ARM install kiya — `java -version` check kiya
- [ ] g++, gcc, python3, node install kiye
- [ ] bwrap, prlimit install kiye — sandbox smoke test pass kiya
- [ ] PostgreSQL install + `codecombat` database banaya
- [ ] Valkey install kiya — `valkey-cli ping` → PONG
- [ ] Nginx install + config kiya
- [ ] Frontend build kiya + `/var/www/codecombat/` me copy kiya
- [ ] WAR build kiya + VM pe copy kiya
- [ ] `.env` file banaya (chmod 600)
- [ ] Systemd service banaya + start kiya
- [ ] SSL certificate liya (certbot)
- [ ] Oracle iptables rules add kiye
- [ ] Health check pass kiya: `curl https://codecoder.in/api/health`
- [ ] Ek Java submission test kiya — verdict aaya
