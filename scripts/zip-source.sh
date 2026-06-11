#!/usr/bin/env bash
set -euo pipefail

NAME="voice-chat-translate"
DEST="/tmp/${NAME}-source.zip"

# Include tracked + untracked non-ignored files (respects .gitignore)
git ls-files --cached --others --exclude-standard \
  | zip -@ "$DEST" >/dev/null

echo "Created: $DEST ($(du -h "$DEST" | cut -f1))"
