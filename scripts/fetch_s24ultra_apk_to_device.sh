#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${HOME}/spotify-s24ultra-base"
LOG_FILE="${REPO_DIR}/deploy.log"
OUT_DIR="/sdcard/Download/SpotifyS24UltraBase"
GH_OWNER_CUR="${GH_OWNER:-ocean244}"
GH_REPO_CUR="${GH_REPO_NAME:-spotify-s24ultra-base}"

mkdir -p "${OUT_DIR}" 2>/dev/null || OUT_DIR="${HOME}/apk_downloads"
mkdir -p "${OUT_DIR}"

log_dl() {
  local ts
  ts="$(date '+%Y-%m-%d %H:%M:%S')"
  printf "[%s] [APK-FETCH] %s\n" "${ts}" "$*" >> "${LOG_FILE}"
}

if [ -z "${GH_TOKEN:-}" ]; then
  log_dl "Brak GH_TOKEN — pominięto pobieranie APK."
  exit 1
fi

RELEASE_JSON="$(curl -fsSL \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GH_OWNER_CUR}/${GH_REPO_CUR}/releases/tags/s24ultra-latest" || true)"

if [ -z "${RELEASE_JSON}" ]; then
  log_dl "Release s24ultra-latest jeszcze nie opublikowany na GitHub."
  exit 0
fi

python3 - "${RELEASE_JSON}" "${OUT_DIR}" "${GH_TOKEN}" << 'PYEOF'
import json
import pathlib
import sys
import urllib.request

data = json.loads(sys.argv[1])
out_dir = pathlib.Path(sys.argv[2])
token = sys.argv[3]

for asset in data.get("assets", []):
    name = asset.get("name", "")
    url = asset.get("browser_download_url", "")
    if not url:
        continue
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/octet-stream",
            "User-Agent": "S24Ultra-Termux-Fetcher",
        },
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        (out_dir / name).write_bytes(resp.read())
print(f"Zapisano pliki APK w: {out_dir}")
PYEOF

log_dl "Pobrano pliki APK do ${OUT_DIR}."
ls -lh "${OUT_DIR}" >> "${LOG_FILE}" 2>&1 || true
