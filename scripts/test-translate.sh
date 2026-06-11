#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_DIR="$PROJECT_DIR/run/config/live_voice_translate"
DEVTEST_DIR="$CONFIG_DIR/devtest"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo ""
    echo "Clearing up..."
    cd "$PROJECT_DIR" 2>/dev/null
    ./gradlew "Refresh active project" > /dev/null 2>&1 || true
    echo "Done"
}

trap cleanup EXIT

echo "=================================="
echo " Live Voice Chat Translate - Test"
echo "=================================="

# ── Check API key ──────────────────────────────────────
if [ -z "${GEMINI_API_KEY:-}" ]; then
    if [ -f "$PROJECT_DIR/.env" ]; then
        export "$(grep -E '^GEMINI_API_KEY=' "$PROJECT_DIR/.env" | xargs)"
    fi
fi

if [ -z "${GEMINI_API_KEY:-}" ]; then
    echo -e "${RED}ERROR: GEMINI_API_KEY not set${NC}"
    echo "  export GEMINI_API_KEY='your-key-here'"
    echo "  Or create a .env file with: GEMINI_API_KEY=your-key-here"
    exit 1
fi
echo -e "${GREEN}✓ GEMINI_API_KEY set${NC}"

# ── Ensure directories exist ────────────────────────────
mkdir -p "$DEVTEST_DIR"

# ── Read speech_files.txt ──────────────────────────────
SPEECH_FILE="$SCRIPT_DIR/speech_files.txt"
DOWNLOADED=0

if [ -f "$SPEECH_FILE" ]; then
    echo "Reading: $SPEECH_FILE"
    while IFS= read -r URL; do
        # Trim whitespace
        URL="$(echo "$URL" | xargs)"

        # Skip comments and empty lines
        [[ -z "$URL" ]] && continue
        [[ "$URL" == \#* ]] && continue

        # Derive filename from URL basename, ensure uniqueness
        OUTPUT_NAME="$(basename "$URL")"
        OUTPUT_NAME="${OUTPUT_NAME%.*}.wav"
        OUTPUT_PATH="$DEVTEST_DIR/$OUTPUT_NAME"
        if [ -f "$OUTPUT_PATH" ]; then
            BASE="${OUTPUT_NAME%.wav}"
            N=1
            while [ -f "$DEVTEST_DIR/${BASE}_${N}.wav" ]; do
                N=$((N + 1))
            done
            OUTPUT_NAME="${BASE}_${N}.wav"
            OUTPUT_PATH="$DEVTEST_DIR/$OUTPUT_NAME"
        fi

        echo "  Downloading: $URL -> $OUTPUT_NAME"

        TMPFILE=$(mktemp)
        if command -v curl &>/dev/null; then
            curl -sL -o "$TMPFILE" "$URL" || { echo -e "${RED}  Failed to download${NC}"; rm -f "$TMPFILE"; continue; }
        elif command -v wget &>/dev/null; then
            wget -q -O "$TMPFILE" "$URL" || { echo -e "${RED}  Failed to download${NC}"; rm -f "$TMPFILE"; continue; }
        else
            echo -e "${RED}  Need curl or wget${NC}"
            rm -f "$TMPFILE"
            continue
        fi

        if command -v ffmpeg &>/dev/null; then
            ffmpeg -y -i "$TMPFILE" -ar 48000 -ac 1 -sample_fmt s16 "$OUTPUT_PATH" 2>/dev/null
            echo -e "${GREEN}  ✓ Saved: $OUTPUT_PATH${NC}"
            DOWNLOADED=$((DOWNLOADED + 1))
        else
            if [[ "$URL" == *.wav ]]; then
                cp "$TMPFILE" "$OUTPUT_PATH"
                echo -e "${YELLOW}  ⚠ No ffmpeg — copied as-is (must be 48kHz 16-bit mono)${NC}"
                DOWNLOADED=$((DOWNLOADED + 1))
            else
                echo -e "${RED}  Need ffmpeg to convert non-WAV files${NC}"
            fi
        fi
        rm -f "$TMPFILE"
    done < "$SPEECH_FILE"
fi

# ── Handle single URL argument (legacy) ──────────────────
if [ $# -ge 1 ] && [ "$DOWNLOADED" -eq 0 ]; then
    ADD_SOURCE="$1"

    OUTPUT_NAME="$(basename "$ADD_SOURCE")"
    OUTPUT_NAME="${OUTPUT_NAME%.*}.wav"
    OUTPUT_PATH="$DEVTEST_DIR/$OUTPUT_NAME"
    if [ -f "$OUTPUT_PATH" ]; then
        BASE="${OUTPUT_NAME%.wav}"
        OUTPUT_NAME="${BASE}_1.wav"
        OUTPUT_PATH="$DEVTEST_DIR/$OUTPUT_NAME"
    fi

    TMPFILE=$(mktemp)
    if [[ "$ADD_SOURCE" =~ ^https?:// ]]; then
        echo "Downloading: $ADD_SOURCE"
        if command -v curl &>/dev/null; then
            curl -sL -o "$TMPFILE" "$ADD_SOURCE"
        elif command -v wget &>/dev/null; then
            wget -q -O "$TMPFILE" "$ADD_SOURCE"
        else
            echo -e "${RED}Need curl or wget${NC}"
            rm -f "$TMPFILE"
            exit 1
        fi
        ADD_SOURCE="$TMPFILE"
    fi

    if command -v ffmpeg &>/dev/null; then
        echo "Converting to 48kHz 16-bit mono WAV..."
        ffmpeg -y -i "$ADD_SOURCE" -ar 48000 -ac 1 -sample_fmt s16 "$OUTPUT_PATH" 2>/dev/null
        echo -e "${GREEN}✓ Saved: $OUTPUT_PATH${NC}"
        DOWNLOADED=$((DOWNLOADED + 1))
    else
        if [[ "$ADD_SOURCE" == *.wav ]]; then
            cp "$ADD_SOURCE" "$OUTPUT_PATH"
            echo -e "${YELLOW}⚠ No ffmpeg — copied as-is${NC}"
            DOWNLOADED=$((DOWNLOADED + 1))
        else
            echo -e "${RED}Need ffmpeg to convert non-WAV files${NC}"
            exit 1
        fi
    fi

    if [[ "$1" =~ ^https?:// ]] && [ -f "$TMPFILE" ]; then
        rm -f "$TMPFILE"
    fi
fi

# ── Show current test files ─────────────────────────────
WAV_COUNT=$(find "$DEVTEST_DIR" -name '*.wav' 2>/dev/null | wc -l)

if [ "$WAV_COUNT" -eq 0 ]; then
    echo ""
    echo -e "${YELLOW}No WAV files in $DEVTEST_DIR${NC}"
    echo "  Create a speech list at:"
    echo "    $SPEECH_FILE"
    echo "  Format (one URL per line):"
    echo "    https://example.com/audio_fr.wav"
    echo "    https://example.com/speech_de.wav"
    echo ""
    echo "  Or place .wav files directly in:"
    echo "    $DEVTEST_DIR"
else
    echo ""
    echo -e "${GREEN}✓ $WAV_COUNT test file(s) ready:${NC}"
    for f in "$DEVTEST_DIR"/*.wav; do
        [ -f "$f" ] || continue
        BASENAME=$(basename "$f" .wav)
        DURATION="?"
        if command -v ffprobe &>/dev/null; then
            DURATION=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$f" 2>/dev/null || echo "?")
        fi
        echo "    $BASENAME  (${DURATION}s)"
    done
fi

# ── Launch client with devtools ─────────────────────────
echo ""
echo "=================================="
echo " Launching Minecraft with devtools"
echo "=================================="
echo ""
echo "  Audio sources will play around you in-game"
echo "  Connect to a server with Simple Voice Chat"
echo ""

cd "$PROJECT_DIR"

# Run Refresh active project with devtools to set active code
./gradlew "Refresh active project" -Plive_voice_translate.devtools=true > /dev/null 2>&1

# Run client
./gradlew runActiveClient -Plive_voice_translate.devtools=true
