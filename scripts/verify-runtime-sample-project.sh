#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> verify pickles-runtime sample project"

(
  cd "${ROOT_DIR}/pickles-runtime"
  npm ci
  npm run typecheck
  npm test
)
