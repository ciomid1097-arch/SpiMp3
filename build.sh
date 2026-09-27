#!/usr/bin/env bash
# SpiMp3 build helper.
#
# The project sits under a non-ASCII Windows path, which corrupts the Gradle
# test-worker classpath. This script routes build output through an ASCII
# junction (C:\spimp3-out -> ./out) and then runs Gradle.
set -euo pipefail

cd "$(dirname "$0")"

GRADLE="${GRADLE:-$HOME/gradle-dist/gradle-9.8.0/bin/gradle}"
ASCII_OUT='C:\spimp3-out\app'
TASK="${1:-:app:assembleDebug}"

mkdir -p out
if [ ! -d /c/spimp3-out ]; then
  echo "Creating ASCII junction C:\\spimp3-out -> ./out ..."
  powershell -NoProfile -Command "New-Item -ItemType Junction -Path 'C:\spimp3-out' -Target '$(pwd -W)\out' | Out-Null" || true
fi

SPIMP3_OUT_DIR="$ASCII_OUT" "$GRADLE" "$TASK" --console=plain "${@:2}"
