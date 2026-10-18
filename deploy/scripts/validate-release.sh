#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fail=0
if git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  tracked="$(git -C "$ROOT_DIR" ls-files -- '.env' '**/.env' '.env.prod' '**/.env.prod' 'deploy/certs/*.pem')"
  if [[ -n "$tracked" ]]; then printf 'FAIL: protected files in Git index:\n%s\n' "$tracked"; fail=1; fi
else
  echo 'WARN: Git metadata unavailable; secret tracking cannot be verified in this snapshot.'
fi
for required in PHASE19.md docs/API-CONTRACT.md docs/REMEDIATION_PLAN.md docs/SECURITY_HARDENING.md docs/GATE_OPERATION_LIFECYCLE.md docs/HIKVISION_WEBHOOK_SECURITY.md docs/RBAC_MATRIX.md docs/DEPLOYMENT_RUNBOOK.md docs/BACKUP_RESTORE.md docs/TEST_REPORT.md docs/RELEASE_READINESS.md docs/ADR/ADR-WEBHOOK-AUTH.md docs/ADR/ADR-SESSION-REVOCATION.md docs/ADR/ADR-GATE-IDEMPOTENCY.md docs/ADR/ADR-PASSAGE-CONFIRMATION.md backend/build.gradle frontend/package.json e2e/package.json; do
  test -f "$ROOT_DIR/$required" || { echo "FAIL: missing $required"; fail=1; }
done
python3 "$ROOT_DIR/scripts/render-rbac.py" --check || fail=1
[[ "$fail" == 0 ]] || exit 1
echo 'Release structure validated. This does not approve production or physical automation.'
