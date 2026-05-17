#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# CodeCombat — Local Development Startup Script
# Loads .env file and starts the Spring Boot server
# Usage: ./start.sh
# ─────────────────────────────────────────────────────────────────────────────

set -e

# Check .env exists
if [ ! -f ".env" ]; then
    echo "❌ .env file not found!"
    echo "   Copy .env.example to .env and fill in your values."
    exit 1
fi

echo "📦 Loading environment from .env..."

# Export all variables from .env (skip comments and empty lines)
set -a
source .env
set +a

echo "✅ Environment loaded"
echo "   DB: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "   Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "   Port: ${SERVER_PORT:-8080}"
echo ""
echo "🚀 Starting CodeCombat backend..."
echo ""

./mvnw spring-boot:run
