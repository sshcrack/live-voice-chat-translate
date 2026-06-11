#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Launch Smoke Test ==="
echo ""

CLIENT_TIMEOUT=30

# Discover all version-loader pairs from settings.gradle.kts
VERSIONS=$(grep 'match("[0-9]' settings.gradle.kts | sed 's/.*match("\(.*\)", "\(.*\)").*/\1-\2/')
if [ -z "$VERSIONS" ]; then
    echo "ERROR: no versions found in settings.gradle.kts" >&2
    exit 1
fi

echo "Versions under test:"
for VERSION in $VERSIONS; do
    echo "  - $VERSION"
done
echo ""

cleanup() {
    ./gradlew "Refresh active project" > /dev/null 2>&1 || true
}

trap cleanup EXIT

# Set active code with devtools
./gradlew "Refresh active project" -Plive_voice_translate.devtools=true > /dev/null 2>&1

# Clean up stale temp files
rm -f /tmp/launch-smoke-*.log

declare -a LOG_FILES
declare -a VERSION_LIST

I=0
for VERSION in $VERSIONS; do
    LOG_FILE=$(mktemp "/tmp/launch-smoke-${VERSION}-XXXXXX.log")
    LOG_FILES[$I]="$LOG_FILE"
    VERSION_LIST[$I]="$VERSION"

    echo "[$(date +%H:%M:%S)] === Testing $VERSION ==="

    if timeout "$CLIENT_TIMEOUT" ./gradlew ":$VERSION:runClient" -Plive_voice_translate.devtools=true --no-daemon > "$LOG_FILE" 2>&1; then
        echo "[$(date +%H:%M:%S)] === BUILD SUCCESSFUL: $VERSION ==="
    else
        EXIT_CODE=$?
        if [ "$EXIT_CODE" -eq 124 ]; then
            echo "[$(date +%H:%M:%S)] === TIMED OUT: $VERSION (expected) ==="
        else
            echo "[$(date +%H:%M:%S)] === BUILD FAILED: $VERSION (exit $EXIT_CODE) ==="
        fi
    fi

    # Copy Minecraft log alongside gradle output
    MC_LOG="versions/$VERSION/run/logs/latest.log"
    if [ -f "$MC_LOG" ]; then
        cp "$MC_LOG" "${LOG_FILE%.log}-minecraft.log"
    fi

    echo ""
    I=$((I+1))
done

echo "=========================================="
echo "=== Launch Smoke Test Results           ==="
echo "=========================================="
echo ""

FAILED=0
for I in "${!VERSION_LIST[@]}"; do
    VERSION="${VERSION_LIST[$I]}"
    LOG_FILE="${LOG_FILES[$I]}"

    if grep -q "BUILD FAILED" "$LOG_FILE" 2>/dev/null; then
        STATUS="FAIL"
        FAILED=1
    elif grep -q "Minecraft has crashed" "$LOG_FILE" 2>/dev/null; then
        STATUS="FAIL"
        FAILED=1
    elif grep -q "Exception in thread" "$LOG_FILE" 2>/dev/null; then
        STATUS="FAIL"
        FAILED=1
    else
        STATUS="PASS"
    fi

    printf "  [%-4s] %-18s %s\n" "$STATUS" "$VERSION" "$LOG_FILE"
done

echo ""
if [ "$FAILED" -ne 0 ]; then
    echo "FAILED: one or more launch smoke tests did not pass."
    exit 1
fi

echo "All launch smoke tests passed."
