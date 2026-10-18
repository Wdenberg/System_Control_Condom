#!/usr/bin/env bash
set -euo pipefail
umask 077
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
ENV_FILE="${ENV_FILE:-.env.prod}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
[[ -r "$ENV_FILE" ]] || { echo 'Environment file is missing or unreadable.' >&2; exit 1; }
[[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && (( RETENTION_DAYS >= 1 )) || { echo 'RETENTION_DAYS must be a positive integer.' >&2; exit 1; }
[[ "${BACKUP_QUIESCED:-no}" == yes ]] || { echo 'Pause ingress and writes, then set BACKUP_QUIESCED=yes. A consistent database/files snapshot requires a maintenance window.' >&2; exit 1; }
COMPOSE=(docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml)
"${COMPOSE[@]}" config --quiet
mkdir -p "$BACKUP_DIR"
WORK="$(mktemp -d "$BACKUP_DIR/.backup.XXXXXXXX")"
trap 'rm -rf "$WORK"' EXIT
# Variables are expanded inside the container; .env is never sourced as shell code.
"${COMPOSE[@]}" exec -T postgres sh -eu -c 'test -n "$POSTGRES_USER"; test -n "$POSTGRES_DB"; exec pg_dump --format=custom --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$WORK/database.dump"
"${COMPOSE[@]}" exec -T backend tar -C /app/data/hikvision -cf - . > "$WORK/hikvision.tar"
python3 - "$WORK" <<'PY'
import hashlib,json,sys
from pathlib import Path
root=Path(sys.argv[1]); names=['database.dump','hikvision.tar']
if any((root/name).stat().st_size==0 for name in names):raise SystemExit('Empty backup member')
(root/'manifest.json').write_text(json.dumps({'format':1,'sha256':{name:hashlib.file_digest((root/name).open('rb'),'sha256').hexdigest() for name in names}},indent=2))
PY
OUTPUT="$BACKUP_DIR/garage_control_$(date -u +%Y%m%dT%H%M%SZ)_${RANDOM}.tar.gz"
tar -czf "$WORK/bundle.tar.gz" -C "$WORK" database.dump hikvision.tar manifest.json
mv "$WORK/bundle.tar.gz" "$OUTPUT"
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'garage_control_*.tar.gz' -mtime "+$RETENTION_DAYS" -delete
printf 'Backup created (restore verification still required): %s\n' "$OUTPUT"
