#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_CMD="${GRADLE_CMD:-}"
GRADLE_WRAPPER="${ROOT_DIR}/pickles-intellij-plugin/gradlew"
JENV_JBR17_NAME="${JENV_JBR17_NAME:-jetbrains64-17.0.14}"

JAVA_VERSION_OUTPUT="$("${JAVA_HOME:+${JAVA_HOME}/bin/}java" -version 2>&1 || true)"
if ! grep -Eq 'version "(17|1[8-9]|[2-9][0-9])\.' <<<"${JAVA_VERSION_OUTPUT}"; then
  if command -v jenv >/dev/null 2>&1; then
    if JENV_JBR17_HOME="$(jenv prefix "${JENV_JBR17_NAME}" 2>/dev/null)"; then
      export JAVA_HOME="${JENV_JBR17_HOME}"
      JAVA_VERSION_OUTPUT="$("${JAVA_HOME}/bin/java" -version 2>&1 || true)"
    fi
  fi
fi

if ! grep -Eq 'version "(17|1[8-9]|[2-9][0-9])\.' <<<"${JAVA_VERSION_OUTPUT}"; then
  cat >&2 <<EOF
Java 17+ is required for pickles-intellij-plugin Gradle tasks.
Current Java:
${JAVA_VERSION_OUTPUT}
Set JAVA_HOME to a JDK 17+ installation and rerun this script.
EOF
  exit 1
fi

if [[ -z "${GRADLE_CMD}" && -x "${GRADLE_WRAPPER}" ]]; then
  GRADLE_CMD="${GRADLE_WRAPPER}"
fi

if [[ -z "${GRADLE_CMD}" ]]; then
  GRADLE_CMD="gradle"
fi

if ! command -v "${GRADLE_CMD}" >/dev/null 2>&1; then
  cat >&2 <<EOF
Gradle command not found: ${GRADLE_CMD}
Use the project Gradle wrapper at ${GRADLE_WRAPPER}, install Gradle 8.13+, or set GRADLE_CMD to an executable Gradle command.
Example: GRADLE_CMD=/path/to/gradle ${ROOT_DIR}/scripts/run-intellij-gradle.sh -p pickles-intellij-plugin build
EOF
  exit 127
fi

exec "${GRADLE_CMD}" "$@"
