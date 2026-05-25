#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> verify full flow"

(
  cd "${ROOT_DIR}/pickles-runtime"
  npm ci
  node --import tsx --test ../e2e/full-flow/full-flow.test.mjs
)
