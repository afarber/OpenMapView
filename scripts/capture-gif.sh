#!/bin/bash

# Record the screen of the connected emulator and save it as the screenshot.gif of an example
#
# Usage: ./scripts/capture-gif.sh <example dir> [seconds] [crop]
#   e.g. ./scripts/capture-gif.sh examples/compose/Pan 10
#
# The optional crop is width:height:x:y in screen pixels. On the 1408x792 automotive
# emulator, 1408:620:0:76 removes the status bar and the car's system bar.
#
# Start the example on a landscape automotive emulator first, then run this script and
# use the app while it records. The GIF must stay below 2 MB so the README loads quickly:
# if it is larger, the script retries with fewer frames and colors and then a smaller size.

set -euo pipefail

MAX_BYTES=$((2 * 1024 * 1024))

if [ $# -lt 1 ] || [ ! -d "$1" ]; then
    echo "Usage: $0 <example dir> [seconds]"
    exit 1
fi

OUTPUT="$1/screenshot.gif"
SECONDS_TO_RECORD="${2:-10}"
CROP="${3:+crop=$3,}"
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Recording for $SECONDS_TO_RECORD seconds..."
adb shell screenrecord --time-limit "$SECONDS_TO_RECORD" /sdcard/capture.mp4
adb pull /sdcard/capture.mp4 "$WORK_DIR/capture.mp4" > /dev/null
adb shell rm /sdcard/capture.mp4

# Convert with a palette computed from the whole video, which keeps map tiles sharp
# at a small file size. The longer side of the GIF is scaled to the given size.
convert() {
    local size=$1 fps=$2 colors=$3
    local filters="${CROP}fps=$fps,scale=$size:$size:force_original_aspect_ratio=decrease:flags=lanczos"
    ffmpeg -loglevel error -y -i "$WORK_DIR/capture.mp4" \
        -vf "$filters,palettegen=max_colors=$colors" -update 1 "$WORK_DIR/palette.png"
    ffmpeg -loglevel error -y -i "$WORK_DIR/capture.mp4" -i "$WORK_DIR/palette.png" \
        -lavfi "$filters [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
        "$OUTPUT"
}

for setting in "800 10 128" "800 8 96" "720 8 64" "640 6 64"; do
    read -r size fps colors <<< "$setting"
    convert "$size" "$fps" "$colors"
    bytes=$(wc -c < "$OUTPUT" | tr -d ' ')
    echo "$OUTPUT: ${size}px, $fps fps, $colors colors, $((bytes / 1024)) KB"
    if [ "$bytes" -lt "$MAX_BYTES" ]; then
        exit 0
    fi
done

echo "ERROR: $OUTPUT is still 2 MB or larger, record a shorter clip"
exit 1
