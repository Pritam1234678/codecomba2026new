# CodeCombat 🚀

[![Live Demo](https://img.shields.io/badge/Live-Demo-brightgreen?style=for-the-badge)](http://codecombat.live)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-F27309?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactjs.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

**CodeCombat** is a high-performance Online Judge and Competitive Programming platform. It provides a robust infrastructure for hosting coding contests, solving algorithmic problems, and tracking real-time performance through a dynamic leaderboard.

## 🌟 Key Features

### 🖥️ Advanced Code Execution
- **Multi-Language Support**: Execute code in **Java, C++, Python, JavaScript, and C**.
- **Secure Sandbox**: Docker-based execution ensures user submissions are run in an isolated and secure environment.
- **Robust Judge**: Automatically validates submissions against multiple test cases with strict Time Limit (TLE) and Memory Limit (MLE) checks.

### 🏆 Contest & Problem Management
- **Dynamic Contests**: Schedule and manage coding contests with automated start/end times.
- **Problem Bank**: Comprehensive problem sets with varying difficulty levels (Easy, Medium, Hard).
- **Admin Dashboard**: Full control over problem creation, test case management, and user oversight.

### 📊 Real-time User Engagement
- **Live Leaderboard**: Watch rankings change in real-time as users submit solutions.
- **User Dashboard**: Track personal progress, success rates, and recent submission history.
- **Modern UI/UX**: Clean, responsive, and glassmorphism-inspired design optimized for both desktop and mobile devices.

## 🛠️ Tech Stack

- **Frontend**: React.js, Vite, Tailwind CSS, Framer Motion, Monaco Editor.
- **Backend**: Spring Boot, Spring Security (JWT), Spring Data JPA.
- **Core Engine**: Docker (for code sandboxing), Java Process-based execution.
- **Database**: MySQL.
- **Deployment**: AWS EC2, Nginx.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Node.js & npm
- MySQL Server
- Docker (Required for code execution)

### Backend Setup
1. Clone the repository and navigate to the root directory.
2. Update `src/main/resources/application.properties` with your MySQL credentials.
3. Build and run the application:
   ```bash
   mvn clean spring-boot:run
   ```

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

## 🌐 Live Deployment
The platform is live and accessible at: [http://codecombat.live](http://codecombat.live)

---
Developed with ❤️ by [Your Name/Team]
