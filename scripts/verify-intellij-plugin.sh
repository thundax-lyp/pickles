#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> verify pickles-intellij-plugin"

"${ROOT_DIR}/scripts/run-intellij-gradle.sh" -p "${ROOT_DIR}/pickles-intellij-plugin" build --no-configuration-cache
