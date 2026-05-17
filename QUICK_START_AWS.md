# AWS Deployment - Quick Start (Hinglish)

## 🎯 Sabse Pehle Ye Karo

### 1. AWS Account Setup
1. AWS Console me login karo: https://aws.amazon.com
2. EC2 Dashboard kholo
3. **Launch Instance** button dabao

### 2. EC2 Configuration
```
Name: CodeCombat-Server
OS: Ubuntu Server 22.04 LTS (Free tier eligible)
Instance Type: t3.medium (2 vCPU, 4GB RAM)
Storage: 30GB gp3
Key Pair: Create new → Download .pem file (IMPORTANT!)
```

**Security Group Rules:**
```
SSH (22) - Your IP only
HTTP (80) - 0.0.0.0/0
HTTPS (443) - 0.0.0.0/0
Custom TCP (8080) - 0.0.0.0/0
```

### 3. Connect to EC2
```bash
# Windows PowerShell se:
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

### 4. Auto Setup Script Chalao
```bash
# EC2 par ye command run karo:
curl -o setup.sh https://raw.githubusercontent.com/your-repo/codecombat2026/main/aws-setup.sh
chmod +x setup.sh
./setup.sh
```

**Ya manually:**
```bash
# 1. Update system
sudo apt update && sudo apt upgrade -y

# 2. Install Java
sudo apt install openjdk-17-jdk -y

# 3. Install MySQL
sudo apt install mysql-server -y
sudo mysql
```
```sql
CREATE DATABASE codecombat;
CREATE USER 'codecombat'@'localhost' IDENTIFIED BY 'YourPassword123';
GRANT ALL PRIVILEGES ON codecombat.* TO 'codecombat'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

```bash
# 4. Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install nodejs -y

# 5. Install Nginx
sudo apt install nginx -y

# 6. Install Docker
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu
```

### 5. Upload Code
```bash
# Local machine (Windows) se:
scp -i "your-key.pem" -r C:\Users\manda_5c4udb0\Desktop\codecombat2026 ubuntu@your-ec2-ip:~/
```

### 6. Build Backend
```bash
# EC2 par:
cd ~/codecombat2026

# application.properties update karo
nano src/main/resources/application.properties
```
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/codecombat
spring.datasource.username=codecombat
spring.datasource.password=YourPassword123
server.port=8080
codecombat.jwt.secret=YourSecretKey123456789012345678901234567890
codecombat.docker.host=unix:///var/run/docker.sock
```

```bash
# Build karo
chmod +x mvnw
./mvnw clean package -DskipTests
```

### 7. Build Frontend
```bash
cd ~/codecombat2026/frontend

# .env.production banao
echo "VITE_API_BASE_URL=http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)/api" > .env.production

npm install
npm run build
sudo mkdir -p /var/www/codecombat
sudo cp -r dist/* /var/www/codecombat/
```

### 8. Setup Services

**Backend Service:**
```bash
sudo nano /etc/systemd/system/codecombat-backend.service
```
```ini
[Unit]
Description=CodeCombat Backend
After=mysql.service

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/codecombat2026
ExecStart=/usr/bin/java -jar -Xmx2G target/codecombat2026-0.0.1-SNAPSHOT.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

**Start Backend:**
```bash
sudo systemctl daemon-reload
sudo systemctl start codecombat-backend
sudo systemctl enable codecombat-backend
```

**Nginx Config:**
```bash
sudo nano /etc/nginx/sites-available/codecombat
```
```nginx
server {
    listen 80;
    server_name _;

    location / {
        root /var/www/codecombat;
        try_files $uri /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /uploads/ {
        alias /home/ubuntu/codecombat2026/uploads/;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/codecombat /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

### 9. Check Everything
```bash
# Backend status
sudo systemctl status codecombat-backend

# Backend logs
sudo journalctl -u codecombat-backend -f

# Test API
curl http://localhost:8080/api/test/all
```

### 10. Access App
```
Frontend: http://your-ec2-public-ip
Backend: http://your-ec2-public-ip/api
```

---

## 🔧 Common Commands

**Restart Services:**
```bash
sudo systemctl restart codecombat-backend
sudo systemctl restart nginx
sudo systemctl restart mysql
```

**View Logs:**
```bash
# Backend
sudo journalctl -u codecombat-backend -n 100

# Nginx
sudo tail -100 /var/log/nginx/error.log
```

**Rebuild & Deploy:**
```bash
# Backend
cd ~/codecombat2026
./mvnw clean package -DskipTests
sudo systemctl restart codecombat-backend

# Frontend
cd ~/codecombat2026/frontend
npm run build
sudo cp -r dist/* /var/www/codecombat/
```

---

## 💰 Cost Breakdown

**Monthly Cost (~$18):**
- EC2 t3.medium: ~$15/month
- EBS 30GB: ~$2/month
- Data Transfer: ~$1/month

**Total for 6 months:** ~$108 (slightly over $100 but manageable)

**Cost Saving Tips:**
1. Stop instance when not in use: `sudo shutdown -h now`
2. Use Spot Instances (50-70% cheaper)
3. Setup auto-shutdown at night (if not 24/7)

---

## 🚨 Troubleshooting

**Backend not starting?**
```bash
sudo journalctl -u codecombat-backend -n 50
# Check MySQL: sudo systemctl status mysql
# Check port: sudo lsof -i :8080
```

**Frontend not loading?**
```bash
sudo nginx -t
ls -la /var/www/codecombat/
```

**Database error?**
```bash
mysql -u codecombat -p
# Test connection
```

---

## ✅ Final Checklist

- [ ] EC2 instance running
- [ ] MySQL database created
- [ ] Backend JAR built
- [ ] Backend service running
- [ ] Frontend built & deployed
- [ ] Nginx configured
- [ ] Can access frontend
- [ ] Can access API
- [ ] Code execution works

---

**Setup Time:** 1-2 hours  
**Max Users:** 500-1000 concurrent  
**Response Time:** < 200ms
