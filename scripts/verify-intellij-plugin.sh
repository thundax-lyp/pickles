#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_CMD="${GRADLE_CMD:-gradle}"

echo "==> verify pickles-intellij-plugin"

if ! command -v "${GRADLE_CMD}" >/dev/null 2>&1; then
  cat >&2 <<EOF
Gradle command not found: ${GRADLE_CMD}
Install Gradle 8.13+ or set GRADLE_CMD to an executable Gradle command.
Example: GRADLE_CMD=/path/to/gradle ${ROOT_DIR}/scripts/verify-intellij-plugin.sh
EOF
  exit 127
fi

(
  cd "${ROOT_DIR}/pickles-intellij-plugin"
  "${GRADLE_CMD}" build --no-configuration-cache
)
