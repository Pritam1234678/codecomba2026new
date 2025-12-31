# Code Combat

Enterprise-grade Competitive Programming Platform.

## Features
- **Authentication**: JWT-based secure login/signup.
- **Contest Management**: Admin can create contests, problems, and test cases.
- **Code Execution**: Docker-based sandbox for running user submissions securely.
- **Leaderboard**: Real-time ranking based on score and time.

## Tech Stack
- **Backend**: Spring Boot, Spring Security, JPA, MySQL, Docker.
- **Frontend**: React, Vite, Tailwind CSS, Monaco Editor.
- **Database**: MySQL.

## Setup Instructions

### Backend
1. Ensure Docker is running.
2. Ensure MySQL is running on port 3306 (Update `application.properties` if needed).
3. `mvn spring-boot:run`

### Frontend
1. `cd frontend`
2. `npm install`
3. `npm run dev`

## API Endpoints
- `POST /api/auth/signin` - Login
- `POST /api/auth/signup` - Register
- `GET /api/contests` - List contests
- `POST /api/submissions` - Submit code
# codecombat2026
# codecomba2026new
