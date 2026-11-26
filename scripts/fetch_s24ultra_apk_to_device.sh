#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="${HOME}/spotify-s24ultra-base"
LOG_FILE="${REPO_DIR}/deploy.log"
GH_OWNER_CUR="${GH_OWNER:-ocean244}"
GH_REPO_CUR="${GH_REPO_NAME:-spotify-s24ultra-base}"

if [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
  mkdir -p "/sdcard/Download/SpotifyS24UltraBase"
  OUT_DIR="/sdcard/Download/SpotifyS24UltraBase"
else
  OUT_DIR="${HOME}/apk_downloads"
  mkdir -p "${OUT_DIR}"
fi

log_dl() {
  printf "[%s] [APK-FETCH] %s\n" "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >> "${LOG_FILE}"
}

TOKEN="${GH_TOKEN:-}"
if [ -z "${TOKEN}" ] && command -v gh >/dev/null 2>&1; then
  TOKEN="$(gh auth token 2>/dev/null || true)"
fi
if [ -z "${TOKEN}" ]; then
  log_dl "Brak GH_TOKEN."
  exit 1
fi

RELEASE_JSON="$(curl -fsSL \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${GH_OWNER_CUR}/${GH_REPO_CUR}/releases/tags/s24ultra-latest" || true)"

if [ -z "${RELEASE_JSON}" ]; then
  log_dl "Brak wydania s24ultra-latest."
  exit 1
fi

python3 - "${RELEASE_JSON}" "${OUT_DIR}" "${TOKEN}" << 'PYEOF'
import json, pathlib, sys, urllib.request

data = json.loads(sys.argv[1])
out_dir = pathlib.Path(sys.argv[2])
token = sys.argv[3]

for asset in data.get("assets", []):
    name = asset.get("name", "")
    url = asset.get("browser_download_url", "")
    if not name or not url:
        continue
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/octet-stream",
            "User-Agent": "S24Ultra-Termux-Fetcher",
        },
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        (out_dir / name).write_bytes(resp.read())
PYEOF

(cd "${OUT_DIR}" && sha256sum *.apk > checksums.sha256 2>/dev/null || true)
log_dl "Pobrano zasoby wydania do ${OUT_DIR}."
