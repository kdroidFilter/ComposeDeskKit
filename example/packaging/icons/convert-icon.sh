#!/usr/bin/env bash
#
# Converts a source PNG icon into .icns (macOS) and .ico (Windows) formats.
#
# Requirements:
#   - macOS (uses sips + iconutil for .icns)
#   - Python 3 with Pillow (pip3 install Pillow) for .ico
#
# Usage:
#   ./convert-icon.sh [source.png]
#
# If no argument is given, defaults to Icon.png in the same directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="${1:-$SCRIPT_DIR/Icon.png}"

if [[ ! -f "$SRC" ]]; then
  echo "Error: source file not found: $SRC" >&2
  exit 1
fi

echo "Source: $SRC"

# ── .icns (macOS) ────────────────────────────────────────────────────────────
ICONSET="$(mktemp -d)/Icon.iconset"
mkdir -p "$ICONSET"

for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$SRC" --out "$ICONSET/icon_${size}x${size}.png" >/dev/null 2>&1
  double=$((size * 2))
  sips -z "$double" "$double" "$SRC" --out "$ICONSET/icon_${size}x${size}@2x.png" >/dev/null 2>&1
done

ICNS_OUT="$SCRIPT_DIR/Icon.icns"
iconutil -c icns "$ICONSET" -o "$ICNS_OUT"
rm -rf "$(dirname "$ICONSET")"
echo "Created: $ICNS_OUT ($(du -h "$ICNS_OUT" | cut -f1 | xargs))"

# ── .ico (Windows) ───────────────────────────────────────────────────────────
ICO_OUT="$SCRIPT_DIR/Icon.ico"
python3 -c "
from PIL import Image
img = Image.open('$SRC')
sizes = [(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)]
img.save('$ICO_OUT', format='ICO', sizes=sizes)
"
echo "Created: $ICO_OUT ($(du -h "$ICO_OUT" | cut -f1 | xargs))"
