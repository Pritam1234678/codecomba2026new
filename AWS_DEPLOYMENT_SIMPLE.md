# AWS Deployment Guide - CodeCombat2026
## $100 Free Tier ke saath Maximum Load Handle karne ka Plan

---

## 🎯 Architecture Overview

**Services Used (Free Tier Optimized):**
1. **EC2 t3.medium** (1 instance) - Backend + Database
2. **S3** - Static file storage (profile photos)
3. **CloudFront** - CDN for frontend (React)
4. **Route 53** - DNS (optional, agar domain hai)
5. **Elastic IP** - Static IP for EC2

**Cost Estimate:** ~$15-20/month (well within $100 for 6 months)

---

## 📋 Step-by-Step Deployment

### **Step 1: EC2 Instance Setup**

#### 1.1 Launch EC2 Instance
```bash
# AWS Console se:
# 1. EC2 Dashboard > Launch Instance
# 2. Select: Ubuntu Server 22.04 LTS
# 3. Instance Type: t3.medium (2 vCPU, 4GB RAM)
# 4. Storage: 30GB gp3 (free tier allows 30GB)
# 5. Security Group:
#    - SSH (22) - Your IP only
#    - HTTP (80) - Anywhere
#    - HTTPS (443) - Anywhere
#    - Custom TCP (8080) - Anywhere (Spring Boot)
#    - Custom TCP (3306) - Localhost only (MySQL)
```

#### 1.2 Connect to EC2
```bash
# Download .pem file aur connect karo
chmod 400 your-key.pem
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

---

### **Step 2: Server Setup (EC2 par)**

#### 2.1 Update System
```bash
sudo apt update && sudo apt upgrade -y
```

#### 2.2 Install Java 17
```bash
sudo apt install openjdk-17-jdk -y
java -version  # Verify
```

#### 2.3 Install MySQL
```bash
# MySQL install karo
sudo apt install mysql-server -y

# MySQL secure karo
sudo mysql_secure_installation
# Password set karo: CodeCombat@2026

# MySQL me login karo
sudo mysql -u root -p

# Database create karo
CREATE DATABASE codecombat;
CREATE USER 'codecombat'@'localhost' IDENTIFIED BY 'CodeCombat@2026';
GRANT ALL PRIVILEGES ON codecombat.* TO 'codecombat'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 2.4 Install Node.js (Frontend build ke liye)
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install nodejs -y
node -v  # Verify
npm -v   # Verify
```

#### 2.5 Install Nginx (Reverse Proxy)
```bash
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

#### 2.6 Install Docker (Code execution ke liye)
```bash
# Docker install karo
sudo apt install docker.io -y
sudo systemctl start docker
sudo systemctl enable docker

# User ko docker group me add karo
sudo usermod -aG docker ubuntu
newgrp docker  # Refresh group

# Verify
docker --version
```

---

### **Step 3: Deploy Backend (Spring Boot)**

#### 3.1 Upload Code to EC2
```bash
# Local machine se (Windows PowerShell):
scp -i "your-key.pem" -r C:\Users\manda_5c4udb0\Desktop\codecombat2026 ubuntu@your-ec2-ip:~/
```

#### 3.2 Configure Application Properties
```bash
# EC2 par
cd ~/codecombat2026
nano src/main/resources/application.properties
```

**Update these values:**
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/codecombat
spring.datasource.username=codecombat
spring.datasource.password=CodeCombat@2026

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Server
server.port=8080

# JWT
codecombat.jwt.secret=YourBase64SecretKeyHere123456789012345678901234567890
codecombat.jwt.expiration=86400000

# Docker
codecombat.docker.host=unix:///var/run/docker.sock

# File Upload
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

#### 3.3 Build Backend
```bash
cd ~/codecombat2026

# Maven wrapper ko executable banao
chmod +x mvnw

# Build karo (tests skip kar sakte ho speed ke liye)
./mvnw clean package -DskipTests

# JAR file check karo
ls -lh target/*.jar
```

#### 3.4 Create Systemd Service
```bash
sudo nano /etc/systemd/system/codecombat-backend.service
```

**Add this:**
```ini
[Unit]
Description=CodeCombat Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/codecombat2026
ExecStart=/usr/bin/java -jar -Xmx2G /home/ubuntu/codecombat2026/target/codecombat2026-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

**Start Service:**
```bash
sudo systemctl daemon-reload
sudo systemctl start codecombat-backend
sudo systemctl enable codecombat-backend

# Check status
sudo systemctl status codecombat-backend

# Logs dekho
sudo journalctl -u codecombat-backend -f
```

---

### **Step 4: Deploy Frontend (React)**

#### 4.1 Build Frontend
```bash
cd ~/codecombat2026/frontend

# Install dependencies
npm install

# Create production env file
nano .env.production
```

**Add this:**
```env
VITE_API_BASE_URL=http://your-ec2-public-ip:8080/api
```

**Build:**
```bash
npm run build
# Build files will be in dist/ folder
```

#### 4.2 Setup Nginx for Frontend
```bash
# Copy build files to nginx directory
sudo mkdir -p /var/www/codecombat
sudo cp -r dist/* /var/www/codecombat/

# Create nginx config
sudo nano /etc/nginx/sites-available/codecombat
```

**Add this configuration:**
```nginx
server {
    listen 80;
    server_name your-ec2-public-ip;  # Ya domain name

    # Frontend
    location / {
        root /var/www/codecombat;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Backend API Proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Uploaded files
    location /uploads/ {
        alias /home/ubuntu/codecombat2026/uploads/;
        autoindex off;
    }

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

**Enable site:**
```bash
sudo ln -s /etc/nginx/sites-available/codecombat /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default  # Remove default site
sudo nginx -t  # Test configuration
sudo systemctl reload nginx
```

---

### **Step 5: Setup S3 for Profile Photos (Optional but Recommended)**

#### 5.1 Create S3 Bucket
```bash
# AWS Console se:
# 1. S3 > Create Bucket
# 2. Name: codecombat-uploads-<random-number>
# 3. Region: ap-south-1 (Mumbai - closest to India)
# 4. Block all public access: OFF
# 5. Enable versioning: NO (save cost)
```

#### 5.2 Update Backend Code
```java
// application.properties me add karo:
aws.s3.bucket-name=codecombat-uploads-<your-bucket>
aws.s3.region=ap-south-1
aws.access-key-id=YOUR_ACCESS_KEY
aws.secret-access-key=YOUR_SECRET_KEY
```

---

### **Step 6: Setup CloudFront (CDN) - Optional**

```bash
# AWS Console:
# 1. CloudFront > Create Distribution
# 2. Origin Domain: Your EC2 public IP or domain
# 3. Viewer Protocol Policy: Redirect HTTP to HTTPS
# 4. Allowed HTTP Methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE
# 5. Cache Policy: CachingOptimized
# 6. Create Distribution
```

---

### **Step 7: Database Optimization**

```bash
# MySQL config optimize karo
sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
```

**Add these optimizations:**
```ini
[mysqld]
# Performance
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
query_cache_type = 1

# Slow query log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 2
```

**Restart MySQL:**
```bash
sudo systemctl restart mysql
```

---

### **Step 8: Setup Auto-Scaling (Load Handle karne ke liye)**

#### 8.1 Create AMI (Image) of your EC2
```bash
# AWS Console:
# EC2 > Instances > Select your instance > Actions > Image and templates > Create image
# Name: codecombat-v1
# Description: CodeCombat production image
```

#### 8.2 Create Launch Template
```bash
# EC2 > Launch Templates > Create launch template
# Use your AMI
# Instance type: t3.medium
# Security group: Same as before
```

#### 8.3 Create Auto Scaling Group
```bash
# EC2 > Auto Scaling Groups > Create
# Min: 1, Desired: 1, Max: 3
# Scaling Policy:
#   - Target tracking: Average CPU > 70% -> Add instance
#   - Target tracking: Average CPU < 30% -> Remove instance
```

---

### **Step 9: Monitoring & Logging**

#### 9.1 CloudWatch Setup
```bash
# Install CloudWatch agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb

# Configure
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

#### 9.2 Application Logs
```bash
# Backend logs
sudo journalctl -u codecombat-backend -f

# Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# MySQL logs
sudo tail -f /var/log/mysql/error.log
```

---

### **Step 10: Security Hardening**

#### 10.1 Setup Firewall
```bash
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw enable
```

#### 10.2 SSL Certificate (Free - Let's Encrypt)
```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get certificate (domain chahiye)
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal test
sudo certbot renew --dry-run
```

---

## 🚀 Quick Commands Reference

### Start/Stop Services
```bash
# Backend
sudo systemctl start codecombat-backend
sudo systemctl stop codecombat-backend
sudo systemctl restart codecombat-backend

# Nginx
sudo systemctl restart nginx

# MySQL
sudo systemctl restart mysql
```

### Check Logs
```bash
# Backend logs
sudo journalctl -u codecombat-backend -n 100 --no-pager

# Nginx access logs
sudo tail -100 /var/log/nginx/access.log

# MySQL slow queries
sudo tail -100 /var/log/mysql/slow-query.log
```

### Rebuild & Deploy
```bash
# Backend
cd ~/codecombat2026
git pull  # If using git
./mvnw clean package -DskipTests
sudo systemctl restart codecombat-backend

# Frontend
cd ~/codecombat2026/frontend
npm run build
sudo cp -r dist/* /var/www/codecombat/
```

---

## 💰 Cost Optimization Tips

1. **Use Reserved Instances** - 1 year commitment = 40% savings
2. **Stop instances at night** - If not 24/7 needed
3. **Use S3 Intelligent-Tiering** - Auto move old files to cheaper storage
4. **Enable CloudFront caching** - Reduce backend load
5. **Use RDS Free Tier** - Instead of self-managed MySQL (first 12 months)

---

## 📊 Expected Performance

- **Concurrent Users**: 500-1000 users
- **Submissions/min**: 100-200 submissions
- **Response Time**: < 200ms (API calls)
- **Code Execution**: 1-5 seconds (depending on language)

---

## 🔧 Troubleshooting

### Backend not starting?
```bash
# Check logs
sudo journalctl -u codecombat-backend -n 50

# Common issues:
# 1. MySQL not running: sudo systemctl start mysql
# 2. Port 8080 in use: sudo lsof -i :8080
# 3. Out of memory: Check with 'free -h'
```

### Frontend not loading?
```bash
# Check nginx
sudo nginx -t
sudo systemctl status nginx

# Check permissions
ls -la /var/www/codecombat/
```

### Database connection failed?
```bash
# Test MySQL connection
mysql -u codecombat -p codecombat

# Check MySQL status
sudo systemctl status mysql

# Reset password if needed
sudo mysql -u root -p
ALTER USER 'codecombat'@'localhost' IDENTIFIED BY 'NewPassword';
```

---

## ✅ Final Checklist

- [ ] EC2 instance running
- [ ] MySQL database created and accessible
- [ ] Backend JAR built successfully
- [ ] Backend service running (port 8080)
- [ ] Frontend built and deployed to nginx
- [ ] Nginx configured and running
- [ ] API accessible from browser
- [ ] File uploads working
- [ ] Code execution working (Docker)
- [ ] SSL certificate installed (if domain available)
- [ ] CloudWatch monitoring enabled
- [ ] Auto-scaling configured
- [ ] Backups scheduled

---

## 🎉 Access Your Application

**Frontend:** http://your-ec2-public-ip  
**Backend API:** http://your-ec2-public-ip/api  
**Health Check:** http://your-ec2-public-ip/api/test/all

---

**Total Setup Time:** 2-3 hours  
**Monthly Cost:** ~$15-20 (within $100 for 6 months)  
**Max Load:** 500-1000 concurrent users
