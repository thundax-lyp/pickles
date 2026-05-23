#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_CMD="${GRADLE_CMD:-gradle}"

verify_intellij_plugin() {
  echo "==> verify pickles-intellij-plugin"
  (
    cd "${ROOT_DIR}/pickles-intellij-plugin"
    "${GRADLE_CMD}" build
  )
}

verify_sample_project() {
  echo "==> verify e2e/sample-project"
  (
    cd "${ROOT_DIR}/e2e/sample-project"
    npm ci
    npm run typecheck
    npm run lint
  )
}

verify_intellij_plugin
verify_sample_project
