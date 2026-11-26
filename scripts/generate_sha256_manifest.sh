#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
mkdir -p device_profile
(
  find app device_profile scripts \
    \( -path "*/build/*" -o -path "*/.gradle/*" -o -path "*/artifacts_out/*" \) -prune \
    -o -type f ! -name "artifacts.sha256" ! -name "sha256_sync_status.json" -print0 \
    | sort -z | xargs -0 -r sha256sum
) > device_profile/artifacts.sha256
python3 - << 'PY'
import json, pathlib
from datetime import datetime, timezone
p = pathlib.Path("device_profile/sha256_sync_status.json")
p.write_text(json.dumps({
    "status": "synced",
    "updated_utc": datetime.now(timezone.utc).isoformat()
}, indent=2) + "\n", encoding="utf-8")
PY
