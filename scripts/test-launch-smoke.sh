#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MAX_PARALLEL="${1:-3}"

echo "=== Launch Smoke Test ==="
echo "  Max parallel: $MAX_PARALLEL"
echo ""

VERSIONS=$(grep 'match("[0-9]' settings.gradle.kts | while IFS= read -r line; do
    version=$(echo "$line" | sed 's/.*match("\([^"]*\)".*/\1/')
    loaders=$(echo "$line" | grep -oP '", "\K[^"]*')
    while IFS= read -r loader; do
        echo "$version-$loader"
    done <<< "$loaders"
done)
if [ -z "$VERSIONS" ]; then
    echo "ERROR: no versions found in settings.gradle.kts" >&2
    exit 1
fi

echo "Versions under test:"
echo "$VERSIONS"
echo ""

mapfile -t VERSION_ARRAY <<< "$VERSIONS"

LOG_FILES=()

cleanup() {
    for PID in "${RUNNING_PIDS[@]:-}"; do
        kill "$PID" 2>/dev/null || true
    done
    ./gradlew "Refresh active project" > /dev/null 2>&1 || true
}

trap cleanup EXIT

./gradlew "Refresh active project" -Plive_voice_translate.devtools=true > /dev/null 2>&1

rm -f /tmp/launch-smoke-*.log

run_version() {
    local VERSION="$1" LOG_FILE="$2"

    echo "[$(date +%H:%M:%S)] === Testing $VERSION ==="

    if ./gradlew ":$VERSION:runClient" -Plive_voice_translate.devtools=true --no-daemon 2>&1 | tee "$LOG_FILE"; then
        echo "[$(date +%H:%M:%S)] === BUILD SUCCESSFUL: $VERSION ==="
    else
        EXIT_CODE=$?
        echo "[$(date +%H:%M:%S)] === BUILD FAILED: $VERSION (exit $EXIT_CODE) ==="
    fi

    MC_LOG="versions/$VERSION/run/logs/latest.log"
    if [ -f "$MC_LOG" ]; then
        cp "$MC_LOG" "${LOG_FILE%.log}-minecraft.log"
    fi
}

RUNNING_PIDS=()
for VERSION in "${VERSION_ARRAY[@]}"; do
    LOG_FILE=$(mktemp "/tmp/launch-smoke-${VERSION}-XXXXXX.log")
    LOG_FILES+=("$VERSION:$LOG_FILE")

    run_version "$VERSION" "$LOG_FILE" &
    PID=$!
    RUNNING_PIDS+=("$PID")

    if [ ${#RUNNING_PIDS[@]} -ge "$MAX_PARALLEL" ]; then
        wait -n 2>/dev/null || true
        NEW_PIDS=()
        for P in "${RUNNING_PIDS[@]}"; do
            kill -0 "$P" 2>/dev/null && NEW_PIDS+=("$P")
        done
        RUNNING_PIDS=("${NEW_PIDS[@]}")
    fi
done

for PID in "${RUNNING_PIDS[@]}"; do
    wait "$PID" 2>/dev/null || true
done

echo ""
echo "=========================================="
echo "=== Launch Smoke Test Results           ==="
echo "=========================================="
echo ""

FAILED=0
for ENTRY in "${LOG_FILES[@]}"; do
    VERSION="${ENTRY%%:*}"
    LOG_FILE="${ENTRY#*:}"

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
