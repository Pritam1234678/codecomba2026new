#!/bin/bash
# CodeCombat2026 - AWS EC2 Auto Setup Script
# Run this on your EC2 Ubuntu instance after connecting via SSH

set -e  # Exit on any error

echo "🚀 CodeCombat2026 AWS Deployment Starting..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration Variables - UPDATE THESE
DB_PASSWORD="CodeCombat@2026"
DB_NAME="codecombat"
DB_USER="codecombat"
JWT_SECRET="YourBase64SecretKeyHere123456789012345678901234567890"
EC2_PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

echo -e "${GREEN}EC2 Public IP: $EC2_PUBLIC_IP${NC}"

# Step 1: Update System
echo -e "${YELLOW}Step 1: Updating system...${NC}"
sudo apt update && sudo apt upgrade -y

# Step 2: Install Java 17
echo -e "${YELLOW}Step 2: Installing Java 17...${NC}"
sudo apt install openjdk-17-jdk -y
java -version

# Step 3: Install MySQL
echo -e "${YELLOW}Step 3: Installing MySQL...${NC}"
sudo debconf-set-selections <<< "mysql-server mysql-server/root_password password $DB_PASSWORD"
sudo debconf-set-selections <<< "mysql-server mysql-server/root_password_again password $DB_PASSWORD"
sudo apt install mysql-server -y

# Configure MySQL
echo -e "${YELLOW}Configuring MySQL database...${NC}"
sudo mysql -u root -p$DB_PASSWORD <<MYSQL_SCRIPT
CREATE DATABASE IF NOT EXISTS $DB_NAME;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
MYSQL_SCRIPT

# Step 4: Install Node.js
echo -e "${YELLOW}Step 4: Installing Node.js...${NC}"
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install nodejs -y
node -v
npm -v

# Step 5: Install Nginx
echo -e "${YELLOW}Step 5: Installing Nginx...${NC}"
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx

# Step 6: Install Docker
echo -e "${YELLOW}Step 6: Installing Docker...${NC}"
sudo apt install docker.io -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

# Step 7: Install Maven
echo -e "${YELLOW}Step 7: Installing Maven...${NC}"
sudo apt install maven -y
mvn -version

# Step 8: Create application.properties
echo -e "${YELLOW}Step 8: Creating application.properties...${NC}"
mkdir -p ~/codecombat2026/src/main/resources
cat > ~/codecombat2026/src/main/resources/application.properties <<EOF
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/$DB_NAME
spring.datasource.username=$DB_USER
spring.datasource.password=$DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Server Configuration
server.port=8080

# JWT Configuration
codecombat.jwt.secret=$JWT_SECRET
codecombat.jwt.expiration=86400000

# Docker Configuration
codecombat.docker.host=unix:///var/run/docker.sock

# File Upload Configuration
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
spring.servlet.multipart.enabled=true

# Logging
logging.level.root=INFO
logging.level.com.example.codecombat2026=DEBUG
EOF

# Step 9: Create systemd service for backend
echo -e "${YELLOW}Step 9: Creating systemd service...${NC}"
sudo tee /etc/systemd/system/codecombat-backend.service > /dev/null <<EOF
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
EOF

# Step 10: Configure Nginx
echo -e "${YELLOW}Step 10: Configuring Nginx...${NC}"
sudo tee /etc/nginx/sites-available/codecombat > /dev/null <<EOF
server {
    listen 80;
    server_name $EC2_PUBLIC_IP;

    client_max_body_size 10M;

    # Frontend
    location / {
        root /var/www/codecombat;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    # Backend API Proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # Timeouts for long-running code execution
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }

    # Uploaded files
    location /uploads/ {
        alias /home/ubuntu/codecombat2026/uploads/;
        autoindex off;
    }

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
EOF

sudo ln -sf /etc/nginx/sites-available/codecombat /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t

# Step 11: Setup firewall
echo -e "${YELLOW}Step 11: Configuring firewall...${NC}"
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

# Step 12: Optimize MySQL
echo -e "${YELLOW}Step 12: Optimizing MySQL...${NC}"
sudo tee -a /etc/mysql/mysql.conf.d/mysqld.cnf > /dev/null <<EOF

# Performance Optimization
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
query_cache_type = 1
EOF

sudo systemctl restart mysql

# Step 13: Create directories
echo -e "${YELLOW}Step 13: Creating directories...${NC}"
mkdir -p ~/codecombat2026/uploads/profile-photos
sudo mkdir -p /var/www/codecombat

echo -e "${GREEN}✅ Base setup complete!${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Upload your project code to ~/codecombat2026/"
echo "   scp -i your-key.pem -r /path/to/codecombat2026 ubuntu@$EC2_PUBLIC_IP:~/"
echo ""
echo "2. Build backend:"
echo "   cd ~/codecombat2026"
echo "   ./mvnw clean package -DskipTests"
echo ""
echo "3. Build frontend:"
echo "   cd ~/codecombat2026/frontend"
echo "   echo 'VITE_API_BASE_URL=http://$EC2_PUBLIC_IP/api' > .env.production"
echo "   npm install"
echo "   npm run build"
echo "   sudo cp -r dist/* /var/www/codecombat/"
echo ""
echo "4. Start services:"
echo "   sudo systemctl daemon-reload"
echo "   sudo systemctl start codecombat-backend"
echo "   sudo systemctl enable codecombat-backend"
echo "   sudo systemctl reload nginx"
echo ""
echo "5. Check status:"
echo "   sudo systemctl status codecombat-backend"
echo "   sudo journalctl -u codecombat-backend -f"
echo ""
echo -e "${GREEN}Access your app at: http://$EC2_PUBLIC_IP${NC}"
