#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEVTEST_DIR="$PROJECT_DIR/run/config/live_voice_translate/devtest"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

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

# ── Handle URL/file argument ────────────────────────────
ADD_SOURCE=""
if [ $# -ge 1 ]; then
    ADD_SOURCE="$1"
fi

if [ -n "$ADD_SOURCE" ]; then
    mkdir -p "$DEVTEST_DIR"

    # Extract target language from second arg, or from URL name
    TARGET_LANG="${2:-}"
    if [ -z "$TARGET_LANG" ]; then
        BASENAME="$(basename "$ADD_SOURCE")"
        BASENAME="${BASENAME%.*}"
        # Try to extract language code (e.g., "speech_fr.mp3" -> "fr")
        if [[ "$BASENAME" =~ _([a-z]{2}(-[a-zA-Z0-9]+)?)$ ]]; then
            TARGET_LANG="${BASH_REMATCH[1]}"
        else
            TARGET_LANG="en"
        fi
    fi

    # Determine output filename
    OUTPUT_NAME="speech_${TARGET_LANG}.wav"
    OUTPUT_PATH="$DEVTEST_DIR/$OUTPUT_NAME"

    if [[ "$ADD_SOURCE" =~ ^https?:// ]]; then
        echo "Downloading: $ADD_SOURCE"
        TMPFILE=$(mktemp)
        if command -v curl &>/dev/null; then
            curl -sL -o "$TMPFILE" "$ADD_SOURCE"
        elif command -v wget &>/dev/null; then
            wget -q -O "$TMPFILE" "$ADD_SOURCE"
        else
            echo -e "${RED}ERROR: need curl or wget to download URLs${NC}"
            rm -f "$TMPFILE"
            exit 1
        fi
        ADD_SOURCE="$TMPFILE"
        # We'll clean up after conversion
    fi

    if command -v ffmpeg &>/dev/null; then
        echo "Converting to 48kHz 16-bit mono WAV..."
        ffmpeg -y -i "$ADD_SOURCE" -ar 48000 -ac 1 -sample_fmt s16 "$OUTPUT_PATH" 2>/dev/null
        echo -e "${GREEN}✓ Saved: $OUTPUT_PATH${NC}"
    else
        # No ffmpeg: hope it's already a 48kHz WAV
        if [[ "$ADD_SOURCE" == *.wav ]]; then
            cp "$ADD_SOURCE" "$OUTPUT_PATH"
            echo -e "${YELLOW}⚠ No ffmpeg found — copied as-is (must be 48kHz 16-bit mono WAV)${NC}"
        else
            echo -e "${RED}ERROR: need ffmpeg to convert non-WAV files${NC}"
            echo "  Install ffmpeg or convert manually:"
            echo "  ffmpeg -i input.mp3 -ar 48000 -ac 1 -sample_fmt s16 output.wav"
            exit 1
        fi
    fi

    # Clean up temp file
    if [[ "$1" =~ ^https?:// ]] && [ -f "${ADD_SOURCE:-}" ]; then
        rm -f "$ADD_SOURCE"
    fi
fi

# ── Create devtest directory and show current files ────
mkdir -p "$DEVTEST_DIR"
WAV_COUNT=$(find "$DEVTEST_DIR" -name '*.wav' 2>/dev/null | wc -l)

if [ "$WAV_COUNT" -eq 0 ]; then
    echo ""
    echo -e "${YELLOW}No WAV files found in devtest directory.${NC}"
    echo "  Place .wav files (48kHz 16-bit mono PCM) in:"
    echo "    $DEVTEST_DIR"
    echo ""
    echo "  Naming convention for language detection:"
    echo "    speech_fr.wav  → translates to French"
    echo "    speech_de.wav  → translates to German"
    echo "    speech.wav     → translates to English (default)"
    echo ""
    echo "  Or use this script to add a source:"
    echo "    $0 <url_or_file> [language_code]"
else
    echo -e "${GREEN}✓ $WAV_COUNT test file(s) ready${NC}"
    ls -1 "$DEVTEST_DIR"/*.wav 2>/dev/null | while read f; do
        BASENAME=$(basename "$f" .wav)
        LANG=""
        if [[ "$BASENAME" =~ _([a-z]{2}(-[a-zA-Z0-9]+)?)$ ]]; then
            LANG="${BASH_REMATCH[1]}"
        else
            LANG="en"
        fi
        DUR=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$f" 2>/dev/null || echo "?")
        echo "    $BASENAME → ${LANG} (${DUR}s)"
    done
fi

# ── Launch client with devtools ─────────────────────────
echo ""
echo "=================================="
echo " Launching Minecraft with devtools"
echo "=================================="
echo ""

cd "$PROJECT_DIR"
exec ./gradlew runActiveClient -Plive_voice_translate.devtools=true
