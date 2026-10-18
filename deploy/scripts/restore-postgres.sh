#!/usr/bin/env bash
set -euo pipefail
umask 077
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKUP_FILE="${1:-}"
[[ -f "$BACKUP_FILE" ]] || { echo 'Usage: restore-postgres.sh /absolute/path/backup.tar.gz' >&2; exit 1; }
[[ "${RESTORE_CONFIRM:-}" == ISOLATED ]] || { echo 'Set RESTORE_CONFIRM=ISOLATED. This procedure creates a separate restore environment only.' >&2; exit 1; }
WORK="$(mktemp -d "${TMPDIR:-/tmp}/garage-restore.XXXXXXXX")"
trap 'rm -rf "$WORK"' EXIT
python3 - "$BACKUP_FILE" "$WORK" <<'PY'
import hashlib,json,sys,tarfile
from pathlib import Path
root=Path(sys.argv[2]);allowed={'database.dump','hikvision.tar','manifest.json'}
with tarfile.open(sys.argv[1],'r:gz') as archive:
 members=archive.getmembers()
 if {m.name for m in members}!=allowed or len(members)!=3 or any(not m.isfile() for m in members):raise SystemExit('Invalid backup structure')
 archive.extractall(root,filter='data')
manifest=json.loads((root/'manifest.json').read_text())
if manifest.get('format')!=1:raise SystemExit('Unsupported backup format')
for name in ['database.dump','hikvision.tar']:
 if hashlib.file_digest((root/name).open('rb'),'sha256').hexdigest()!=manifest['sha256'].get(name):raise SystemExit('Backup checksum mismatch')
with tarfile.open(root/'hikvision.tar') as images:
 for member in images.getmembers():
  if member.name.startswith('/') or '..' in Path(member.name).parts or not (member.isfile() or member.isdir()):raise SystemExit('Unsafe image archive')
PY
PROJECT="garage-restore-$(date -u +%Y%m%d%H%M%S)-${RANDOM}"
export RESTORE_DB_PASSWORD
RESTORE_DB_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
COMPOSE=(docker compose -p "$PROJECT" -f "$ROOT_DIR/deploy/restore-compose.yml")
"${COMPOSE[@]}" up -d --wait postgres files
"${COMPOSE[@]}" exec -T postgres pg_restore --exit-on-error --no-owner --no-privileges -U garage_restore -d garage_restore < "$WORK/database.dump"
"${COMPOSE[@]}" exec -T files tar -xf - -C /restore-images < "$WORK/hikvision.tar"
"${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U garage_restore -d garage_restore -c 'SELECT COUNT(*) AS movements FROM movements; SELECT COUNT(*) AS images FROM hikvision_pictures;'
"${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U garage_restore -d garage_restore -c "COPY (SELECT storage_path, content_sha256 FROM hikvision_pictures WHERE storage_path IS NOT NULL) TO STDOUT WITH CSV" > "$WORK/expected-images.csv"
"${COMPOSE[@]}" exec -T files tar -cf - -C /restore-images . > "$WORK/restored-images.tar"
python3 - "$WORK" <<'PYVERIFY'
import csv,hashlib,sys,tarfile
from pathlib import Path
root=Path(sys.argv[1]);expected=dict(csv.reader((root/'expected-images.csv').open()))
found={}
with tarfile.open(root/'restored-images.tar') as archive:
 for member in archive.getmembers():
  if member.isfile():
   stream=archive.extractfile(member)
   found[member.name.removeprefix('./')]=hashlib.file_digest(stream,'sha256').hexdigest()
for name,digest in expected.items():
 if not digest or found.get(name)!=digest:raise SystemExit('Restored image missing or checksum mismatch: '+name)
print('Verified database image references and restored SHA-256 values:',len(expected))
PYVERIFY
printf 'Isolated restore completed. Project: %s. Validate row/file counts and image hashes before approval. Production was not modified.\n' "$PROJECT"
printf 'Inspect with: docker compose -p %s -f deploy/restore-compose.yml ps\n' "$PROJECT"
