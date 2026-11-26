#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_VER="8.9"
DIST_ZIP="gradle-${GRADLE_VER}-bin.zip"
DIST_URL="https://services.gradle.org/distributions/${DIST_ZIP}"
CACHE_ROOT="${GRADLE_USER_HOME:-${HOME:-/tmp}/.gradle}/wrapper/dists/gradle-${GRADLE_VER}-bin/custom"

mkdir -p "${CACHE_ROOT}"
GRADLE_HOME_DIR="$(find "${CACHE_ROOT}" -maxdepth 2 -type d -name "gradle-${GRADLE_VER}" | head -n 1 || true)"

if [ -z "${GRADLE_HOME_DIR}" ]; then
  TMP_ZIP="${CACHE_ROOT}/${DIST_ZIP}"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${DIST_URL}" -o "${TMP_ZIP}"
  else
    wget -q "${DIST_URL}" -O "${TMP_ZIP}"
  fi
  unzip -qo "${TMP_ZIP}" -d "${CACHE_ROOT}"
  rm -f "${TMP_ZIP}"
  GRADLE_HOME_DIR="$(find "${CACHE_ROOT}" -maxdepth 2 -type d -name "gradle-${GRADLE_VER}" | head -n 1)"
fi

exec "${GRADLE_HOME_DIR}/bin/gradle" "$@"
