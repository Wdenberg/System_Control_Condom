#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
ENV_FILE="${ENV_FILE:-.env.prod}"

docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml ps

echo
curl -fsS "https://localhost:${HTTPS_PORT:-443}/" --insecure >/dev/null
echo "Edge HTTPS: OK"

docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T backend \
  wget -qO- http://127.0.0.1:8081/actuator/health/readiness | grep -q 'UP'
echo "Backend readiness: OK"
