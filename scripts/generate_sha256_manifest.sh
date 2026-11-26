#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE_DIR="${BASE_DIR}/device_profile"
MANIFEST_PATH="${PROFILE_DIR}/hardware_matrix.json"
PROPS_PATH="${PROFILE_DIR}/device_profile.properties"
SHA_OUT_PATH="${PROFILE_DIR}/artifacts.sha256"
STATUS_OUT_PATH="${PROFILE_DIR}/sha256_sync_status.json"
LOG_PATH="${BASE_DIR}/deploy.log"

mkdir -p "${PROFILE_DIR}"

log_run() {
  local ts
  ts="$(date '+%Y-%m-%d %H:%M:%S')"
  printf "[%s] [SHA256-RUNNER] %s\n" "${ts}" "$*" >> "${LOG_PATH}"
}

if [ ! -f "${MANIFEST_PATH}" ] || [ ! -f "${PROPS_PATH}" ]; then
  log_run "Brak bazowych plików profilu w ${PROFILE_DIR}."
  exit 1
fi

python3 - "${BASE_DIR}" "${PROPS_PATH}" "${MANIFEST_PATH}" "${SHA_OUT_PATH}" "${STATUS_OUT_PATH}" << 'PYEOF'
import hashlib
import json
import pathlib
import sys
from datetime import datetime, timezone

base_dir = pathlib.Path(sys.argv[1])
props_path = pathlib.Path(sys.argv[2])
manifest_path = pathlib.Path(sys.argv[3])
sha_out_path = pathlib.Path(sys.argv[4])
status_out_path = pathlib.Path(sys.argv[5])

excluded_exact = {
    "device_profile/artifacts.sha256",
    "device_profile/hardware_matrix.json",
    "device_profile/sha256_sync_status.json",
    "deploy.log",
}
excluded_prefixes = (
    ".git/",
    ".gradle/",
    "app/build/",
    "build/",
    "__pycache__/",
)

def sha256_file(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1048576), b""):
            h.update(chunk)
    return h.hexdigest()

files_to_hash = []
for fpath in base_dir.rglob("*"):
    if not fpath.is_file():
        continue
    rel = fpath.relative_to(base_dir).as_posix()
    if rel in excluded_exact:
        continue
    if any(rel.startswith(pfx) for pfx in excluded_prefixes):
        continue
    files_to_hash.append(fpath)

records = []
sha_lines = []
for fpath in sorted(files_to_hash, key=lambda p: p.relative_to(base_dir).as_posix()):
    rel = fpath.relative_to(base_dir).as_posix()
    digest = sha256_file(fpath)
    size_bytes = fpath.stat().st_size
    records.append({
        "path": rel,
        "sha256": digest,
        "size_bytes": size_bytes,
    })
    sha_lines.append(f"{digest}  {rel}")

sha_out_path.write_text("\n".join(sha_lines) + ("\n" if sha_lines else ""), encoding="utf-8")

manifest_data = json.loads(manifest_path.read_text(encoding="utf-8"))
manifest_data["target_profile"] = {
    "model": "SM-S928B",
    "codename": "e3q",
    "abi": "arm64-v8a",
    "android_version": 16,
    "api_level": 36,
    "application_id": "com.s24ultra.spotifybase",
}
manifest_data["artifact_integrity"] = {
    "algorithm": "SHA-256",
    "checksums_file": "device_profile/artifacts.sha256",
    "updated_at_utc": datetime.now(timezone.utc).isoformat(),
    "artifacts": records,
}
manifest_path.write_text(
    json.dumps(manifest_data, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)

manifest_digest = sha256_file(manifest_path)
status_payload = {
    "updated_at_utc": datetime.now(timezone.utc).isoformat(),
    "manifest_path": "device_profile/hardware_matrix.json",
    "manifest_sha256": manifest_digest,
    "checksums_file": "device_profile/artifacts.sha256",
    "verified_artifacts_count": len(records),
    "status": "OK",
}
status_out_path.write_text(
    json.dumps(status_payload, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)
PYEOF

log_run "Zaktualizowano ${MANIFEST_PATH}, ${SHA_OUT_PATH} oraz ${STATUS_OUT_PATH}."
