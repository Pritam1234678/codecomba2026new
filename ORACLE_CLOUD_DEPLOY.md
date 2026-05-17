# CodeCombat — Oracle Cloud Free Tier Deployment Guide

> **Free forever** — Oracle Cloud Always Free tier gives 4 CPU + 24GB RAM ARM VM.
> Sab ek hi machine pe: Spring Boot + MySQL + Redis + Docker judge.
> Expected latency: MySQL ~0.5ms, Redis ~0.5ms (localhost).

---

## What You Get (Free Forever)

| Resource | Amount |
|---|---|
| CPU | 4 OCPU (ARM Ampere A1) |
| RAM | 24 GB |
| Storage | 200 GB block storage |
| Network | 10 TB/month outbound |
| Cost | **₹0 forever** |

---

## Step 1 — Oracle Cloud Account Banao

1. **oracle.com/cloud/free** pe jao
2. "Start for free" click karo
3. **Home Region select karo: `India South (Hyderabad)` ya `India West (Mumbai)`**
   - ⚠️ Home region baad mein change nahi hota — soch ke choose karo
4. Credit card dena padega (verification ke liye, charge nahi hoga)
5. Account verify karo

---

## Step 2 — ARM VM Create Karo

> ⚠️ **"Out of Capacity" error aayega** — ye common hai. Niche fix hai.

### Normal way:
1. OCI Console → **Compute** → **Instances** → **Create Instance**
2. Name: `codecombat-server`
3. **Image:** Ubuntu 22.04 (ARM compatible)
4. **Shape:** `VM.Standard.A1.Flex`
   - OCPU: **4**
   - Memory: **24 GB**
5. **Networking:** Default VCN, public subnet
6. **SSH Key:** Generate karo ya apna public key paste karo
7. Create

### Agar "Out of Capacity" aaye:
**Option A — Keep trying (manual):**
- Alag Availability Domain try karo (AD-1, AD-2, AD-3)
- Raat ko try karo (off-peak hours)

**Option B — Auto-retry script (recommended):**
```bash
# GitHub: hitrov/oci-arm-host-capacity
# Ye script automatically retry karta hai jab capacity available ho
git clone https://github.com/hitrov/oci-arm-host-capacity
# README follow karo — OCI config setup karna padega
```

**Option C — x86 VM use karo (instant availability):**
- Shape: `VM.Standard.E2.1.Micro` (1 OCPU, 1GB RAM) — always available
- Ye sirf lightweight apps ke liye theek hai, judge ke liye slow hoga

---

## Step 3 — VM Setup (SSH se connect karo)

```bash
# SSH connect
ssh -i your-private-key.pem ubuntu@<VM_PUBLIC_IP>

# System update
sudo apt update && sudo apt upgrade -y

# Java 21 install
sudo apt install -y openjdk-21-jdk
java -version  # verify

# Maven install (build ke liye)
sudo apt install -y maven

# MySQL install
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# MySQL secure setup
sudo mysql_secure_installation
# Root password set karo, anonymous users remove karo

# MySQL database create
sudo mysql -u root -p
```

```sql
CREATE DATABASE railway;
CREATE USER 'codecombat'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON railway.* TO 'codecombat'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

```bash
# Redis install
sudo apt install -y redis-server
sudo systemctl start redis
sudo systemctl enable redis

# Redis password set karo (optional but recommended)
sudo nano /etc/redis/redis.conf
# requirepass YourRedisPassword123 uncomment karo

sudo systemctl restart redis

# Docker install (code judge ke liye)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu
newgrp docker
docker --version  # verify

# Compiler tools install (ProcessBuilder judge ke liye)
sudo apt install -y gcc g++ python3 nodejs npm default-jdk
```

---

## Step 4 — Firewall Rules (OCI Security List)

OCI Console mein:
1. **Networking** → **Virtual Cloud Networks** → apna VCN
2. **Security Lists** → Default Security List
3. **Add Ingress Rules:**

| Protocol | Port | Source | Purpose |
|---|---|---|---|
| TCP | 22 | 0.0.0.0/0 | SSH |
| TCP | 8080 | 0.0.0.0/0 | Spring Boot API |
| TCP | 80 | 0.0.0.0/0 | HTTP (Nginx) |
| TCP | 443 | 0.0.0.0/0 | HTTPS (Nginx) |

```bash
# VM ke andar bhi firewall open karo
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## Step 5 — Application Deploy Karo

### Local machine pe JAR build karo:
```bash
# Project root mein
./mvnw clean package -DskipTests
# target/codecombat2026-0.0.1-SNAPSHOT.war ban jaayega
```

### VM pe copy karo:
```bash
scp -i your-key.pem target/codecombat2026-0.0.1-SNAPSHOT.war ubuntu@<VM_IP>:/home/ubuntu/
```

### VM pe `.env` file banao:
```bash
nano /home/ubuntu/.env
```

```bash
# Database — localhost (same machine, ~0.5ms)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=railway
DB_USERNAME=codecombat
DB_PASSWORD=YourStrongPassword123!

# JWT
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=86400000

# Redis — localhost (same machine, ~0.5ms)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USERNAME=default
REDIS_PASSWORD=YourRedisPassword123
# Redis localhost pe SSL nahi chahiye
REDIS_SSL=false

# Docker
DOCKER_HOST=unix:///var/run/docker.sock

# App
APP_URL=https://your-domain.com
SERVER_PORT=8080

# Judge workers
JUDGE_WORKERS=8
```

### application.properties mein Redis SSL conditional banao:

`application.properties` mein ye line update karo:
```properties
spring.data.redis.ssl.enabled=${REDIS_SSL:false}
```

### Application start karo:
```bash
# .env load karke start karo
set -a && source /home/ubuntu/.env && set +a
java -jar /home/ubuntu/codecombat2026-0.0.1-SNAPSHOT.war
```

---

## Step 6 — Systemd Service (Auto-start on reboot)

```bash
sudo nano /etc/systemd/system/codecombat.service
```

```ini
[Unit]
Description=CodeCombat Spring Boot Application
After=network.target mysql.service redis.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu
EnvironmentFile=/home/ubuntu/.env
ExecStart=/usr/bin/java -jar /home/ubuntu/codecombat2026-0.0.1-SNAPSHOT.war
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable codecombat
sudo systemctl start codecombat

# Logs check karo
sudo journalctl -u codecombat -f
```

---

## Step 7 — Nginx Reverse Proxy (Optional but recommended)

```bash
sudo apt install -y nginx

sudo nano /etc/nginx/sites-available/codecombat
```

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # SSE support (for real-time verdicts)
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }

    location /uploads {
        proxy_pass http://localhost:8080;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/codecombat /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# HTTPS (free SSL with Let's Encrypt)
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

---

## Step 8 — Frontend Deploy (Vercel — Free)

```bash
# Local machine pe
cd frontend
npm run build

# Vercel CLI se deploy
npm install -g vercel
vercel --prod
```

Vercel dashboard mein environment variable set karo:
```
VITE_API_URL=https://your-domain.com/api
```

---

## Expected Performance After Deploy

| Operation | Before (local + remote DBs) | After (Oracle VM, all local) |
|---|---|---|
| MySQL query | ~150ms | **~0.5ms** |
| Redis GET | ~400ms | **~0.5ms** |
| API response (cache hit) | ~700ms | **~10-50ms** |
| API response (cache miss) | ~1.5s | **~100-200ms** |
| Page load | ~2.5s | **~200-400ms** |

---

## Troubleshooting

### "Out of Capacity" for ARM VM
- Try different availability domains (AD-1, AD-2, AD-3)
- Try at odd hours (2-5 AM IST)
- Use the auto-retry script: github.com/hitrov/oci-arm-host-capacity
- Alternatively use x86 micro VM (less powerful but always available)

### Redis SSL error on localhost
```properties
# application.properties mein
spring.data.redis.ssl.enabled=false
```

### Docker permission denied
```bash
sudo usermod -aG docker ubuntu
newgrp docker
# Ya logout/login karo
```

### MySQL connection refused
```bash
sudo systemctl status mysql
sudo mysql -u root -p  # test connection
```

### Port 8080 not accessible
```bash
# OCI Security List mein port 8080 add kiya?
# VM ke andar iptables check karo:
sudo iptables -L INPUT | grep 8080
```

---

## Quick Deploy Script

```bash
#!/bin/bash
# deploy.sh — local machine pe run karo

VM_IP="your-vm-ip"
KEY="your-key.pem"

echo "Building JAR..."
./mvnw clean package -DskipTests

echo "Copying to VM..."
scp -i $KEY target/*.war ubuntu@$VM_IP:/home/ubuntu/app.war

echo "Restarting service..."
ssh -i $KEY ubuntu@$VM_IP "sudo systemctl restart codecombat"

echo "Done! Checking status..."
ssh -i $KEY ubuntu@$VM_IP "sudo systemctl status codecombat --no-pager"
```

---

## Summary

```
Oracle Cloud VM (Free Forever)
├── Ubuntu 22.04 ARM
├── Java 21
├── Spring Boot (port 8080)
├── MySQL (localhost:3306)    ← 0.5ms latency
├── Redis (localhost:6379)    ← 0.5ms latency  
├── Docker daemon             ← code execution
└── Nginx (port 80/443)       ← reverse proxy

Frontend: Vercel (free CDN)
Domain: Freenom / Cloudflare (free)
```

**Total cost: ₹0/month, forever.**
