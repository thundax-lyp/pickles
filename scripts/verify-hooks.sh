#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> verify pickles-hooks"

(
  cd "${ROOT_DIR}"
  node --test pickles-hooks/test/hook-http-contract.test.mjs
)
