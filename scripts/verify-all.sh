#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

(cd "${ROOT_DIR}" && npm run lint)

"${ROOT_DIR}/scripts/verify-intellij-plugin.sh"
"${ROOT_DIR}/scripts/verify-sample-project.sh"
"${ROOT_DIR}/scripts/verify-hooks.sh"
"${ROOT_DIR}/scripts/verify-runtime-sample-project.sh"
"${ROOT_DIR}/scripts/verify-full-flow.sh"
