#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_CMD="${GRADLE_CMD:-gradle}"

echo "==> verify pickles-intellij-plugin"

(
  cd "${ROOT_DIR}/pickles-intellij-plugin"
  "${GRADLE_CMD}" build
)
