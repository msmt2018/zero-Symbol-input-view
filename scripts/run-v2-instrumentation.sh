#!/usr/bin/env bash
set -euo pipefail

adb wait-for-device

for attempt in $(seq 1 120); do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [[ "$boot_completed" == "1" ]] && adb shell cmd package list packages >/dev/null 2>&1; then
    break
  fi

  if [[ "$attempt" -eq 120 ]]; then
    adb shell getprop
    adb logcat -d -t 500
    exit 1
  fi

  sleep 5
done

DIAGNOSTICS="zero-symbol-input-view-v2/build/diagnostics/instrumentation"
mkdir -p "$DIAGNOSTICS"

if ./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest; then
  exit 0
else
  test_exit_code=$?
  adb logcat -d -v threadtime > "$DIAGNOSTICS/logcat.txt" 2>&1 || true
  adb shell dumpsys activity activities > "$DIAGNOSTICS/activity.txt" 2>&1 || true
  adb shell dumpsys window windows > "$DIAGNOSTICS/windows.txt" 2>&1 || true
  adb shell screencap -p /sdcard/v2-test-failure.png >/dev/null 2>&1 &&
    adb pull /sdcard/v2-test-failure.png "$DIAGNOSTICS/screenshot.png" >/dev/null 2>&1 || true
  echo "Instrumentation tests failed with exit code $test_exit_code. Diagnostics: $DIAGNOSTICS"
  exit "$test_exit_code"
fi
