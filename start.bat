@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM CodeCombat — Windows Startup Script
REM Loads .env file and starts the Spring Boot server
REM Usage: start.bat
REM ─────────────────────────────────────────────────────────────────────────────

if not exist ".env" (
    echo ERROR: .env file not found!
    echo Copy .env.example to .env and fill in your values.
    exit /b 1
)

echo Loading environment from .env...

REM Read each line from .env and set as environment variable
for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    REM Skip comment lines starting with #
    set "line=%%A"
    if not "!line:~0,1!"=="#" (
        if not "%%A"=="" (
            set "%%A=%%B"
        )
    )
)

echo Environment loaded.
echo Starting CodeCombat backend...
echo.

mvnw.cmd spring-boot:run
