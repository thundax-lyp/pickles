#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> verify e2e/sample-project"

(
  cd "${ROOT_DIR}/e2e/sample-project"
  npm ci
  npm run typecheck
  npm run lint
)
