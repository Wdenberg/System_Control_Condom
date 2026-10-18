#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

export POSTGRES_DB="garage_control_e2e"
export POSTGRES_USER="garage"
export POSTGRES_PASSWORD="garage-e2e-password"
export JWT_SECRET_BASE64="ZTJlLWdhcmFnZS1jb250cm9sLXNlY3JldC1rZXktMDE="
export JWT_ISSUER="garage-control-backend-e2e"
export JWT_ACCESS_TOKEN_TTL="PT30M"
export APP_INITIAL_ADMIN_ENABLED="true"
export APP_INITIAL_ADMIN_USERNAME="admin"
export APP_INITIAL_ADMIN_PASSWORD="E2E@GarageControl#2026"
export APP_INITIAL_ADMIN_FULL_NAME="Administrador E2E"
export APP_INITIAL_ADMIN_ROLE="SUPER_ADMIN"
export HIKVISION_ENTRY_WEBHOOK_TOKEN="e2e-entry-secret-at-least-32-characters"
export HIKVISION_EXIT_WEBHOOK_TOKEN="e2e-exit-secret-at-least-32-characters"
export HIKVISION_ENFORCE_REMOTE_IP="false"
export HIKVISION_COOLDOWN="PT30S"
export GATE_ENABLED="false"
export GATE_DRIVER="SIMULATED"
export CORS_ALLOWED_ORIGINS="http://127.0.0.1:5173"
export SECURITY_RATE_LIMIT_ENABLED="false"
export E2E_ADMIN_USERNAME="admin"
export E2E_ADMIN_PASSWORD="E2E@GarageControl#2026"
export E2E_HIKVISION_WEBHOOK_TOKEN="$HIKVISION_ENTRY_WEBHOOK_TOKEN"

COMPOSE_FILE="docker-compose.e2e.yml"
cleanup() {
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans || true
}
trap cleanup EXIT

echo "==> Starting backend + PostgreSQL + Redis"
docker compose -f "$COMPOSE_FILE" up -d --build

echo "==> Waiting for backend health"
for i in $(seq 1 90); do
  if curl -fsS "${E2E_MANAGEMENT_URL:-http://127.0.0.1:8081}/actuator/health/readiness" >/dev/null; then
    break
  fi
  sleep 2
done
curl -fsS "${E2E_MANAGEMENT_URL:-http://127.0.0.1:8081}/actuator/health/readiness" >/dev/null

echo "==> Installing E2E dependencies"
cd e2e
npm install
npx playwright install chromium

echo "==> Running Playwright E2E"
npm test
