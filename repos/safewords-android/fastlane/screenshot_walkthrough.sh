#!/usr/bin/env bash
# Drives the Safewords debug APK through a scripted golden-path walkthrough
# on the connected emulator and captures all listing screenshots.
# Run from repos/safewords-android/fastlane/ on the dev VM (u5).
#
# Output: fastlane/metadata/android/en-US/images/phoneScreenshots/{1..7}_*.png
set -euo pipefail

export PATH="$HOME/android-sdk/platform-tools:$PATH"

APK="$(cd .. && pwd)/app/build/outputs/apk/debug/app-debug.apk"
OUT="$(cd "$(dirname "$0")" && pwd)/metadata/android/en-US/images/phoneScreenshots"

if [[ ! -f "$APK" ]]; then
  echo "Debug APK not found at $APK"
  echo "Run: ./gradlew assembleDebug first"
  exit 1
fi

if ! adb shell true >/dev/null 2>&1; then
  echo "No emulator/device connected. Boot the AVD first:"
  echo "  ~/android-sdk/emulator/emulator -avd pixel -no-window -no-audio &"
  exit 1
fi

mkdir -p "$OUT"
TMP=$(mktemp -d)
snap() { adb exec-out screencap -p > "$TMP/$1.png"; echo "  captured $1"; }

echo "Resetting state..."
adb shell am force-stop app.thc.safewords
adb uninstall app.thc.safewords >/dev/null 2>&1 || true
adb install -r "$APK" | tail -1
adb shell am start -n app.thc.safewords/com.thc.safewords.MainActivity
sleep 4

echo "Walking golden path..."
snap 5_welcome
adb shell input tap 540 2080; sleep 1.5     # Get started
snap 02-paths
adb shell input tap 540 795; sleep 1.5      # Create new group
snap 03-form
adb shell input tap 271 858; sleep 0.5
adb shell input text Family; sleep 0.4
adb shell input tap 271 1057; sleep 0.5
adb shell input text Mom; sleep 0.4
adb shell input keyevent 4; sleep 0.5
adb shell input tap 626 1254; sleep 2.5     # Create
snap 1_home
adb shell input tap 929 194; sleep 2.5      # Invite QR pill
snap 3_qr
adb shell input keyevent 4; sleep 1.0
adb shell input tap 663 2184; sleep 1.5     # Verify tab
adb shell input tap 540 700; sleep 0.5      # Tap input field
adb shell input text wrong; sleep 0.5
adb shell input keyevent 4; sleep 0.5
snap 6_verify
adb shell input tap 909 2184; sleep 1.5     # Settings tab
snap 7_settings
adb shell input swipe 540 1700 540 700 600; sleep 0.6
adb shell input swipe 540 1700 540 700 600; sleep 0.6
adb shell input tap 540 731; sleep 1.8      # Single use word generator
snap 2_generator

echo "Staging captures into $OUT..."
mv "$TMP"/1_home.png        "$OUT/1_home.png"
mv "$TMP"/2_generator.png   "$OUT/2_generator.png"
mv "$TMP"/3_qr.png          "$OUT/3_qr.png"
mv "$TMP"/5_welcome.png     "$OUT/5_welcome.png"
mv "$TMP"/6_verify.png      "$OUT/6_verify.png"
mv "$TMP"/7_settings.png    "$OUT/7_settings.png"

# Plain mode shot — toggle High Visibility from Settings
adb shell input swipe 540 700 540 1700 600; sleep 0.6
adb shell input tap 927 1391; sleep 2.5     # High visibility toggle
adb shell input tap 540 1910; sleep 1.8     # Plain panel 1 Get started
adb shell input tap 540 1910; sleep 1.8     # Plain panel 2 Get started
snap 4_plain
mv "$TMP"/4_plain.png "$OUT/4_plain.png"

echo "Cleaning up..."
rm -rf "$TMP"

echo ""
echo "Captured 7 screenshots → $OUT"
ls -la "$OUT"
