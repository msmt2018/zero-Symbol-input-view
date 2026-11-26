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
LOG="${RUNNER_TEMP:-/tmp}/v2-instrumentation-gradle.log"

for attempt in 1 2 3; do
  if ./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest --no-daemon --stacktrace >"$LOG" 2>&1; then
    cat "$LOG"
    exit 0
  else
    test_exit_code=$?
  fi

  cat "$LOG"

  # Retry only repository/network throttling. Test assertion, compilation and
  # device failures remain hard failures and are not hidden by a blind retry.
  if grep -Eqi 'status code 429|Too Many Requests|status code 5[0-9][0-9]|HTTP 5[0-9][0-9]|temporarily unavailable|Connection reset|Read timed out|Connect timed out' "$LOG" &&
    [[ "$attempt" -lt 3 ]]; then
    delay=$((attempt * 30))
    echo "::warning::Transient dependency repository failure; retrying instrumentation in ${delay}s (attempt ${attempt}/3)."
    sleep "$delay"
    continue
  fi

  adb logcat -d -v threadtime > "$DIAGNOSTICS/logcat.txt" 2>&1 || true
  adb shell dumpsys activity activities > "$DIAGNOSTICS/activity.txt" 2>&1 || true
  adb shell dumpsys window windows > "$DIAGNOSTICS/windows.txt" 2>&1 || true
  adb shell screencap -p /sdcard/v2-test-failure.png >/dev/null 2>&1 &&
    adb pull /sdcard/v2-test-failure.png "$DIAGNOSTICS/screenshot.png" >/dev/null 2>&1 || true
  echo "Instrumentation tests failed with exit code $test_exit_code. Diagnostics: $DIAGNOSTICS"
  exit "$test_exit_code"
done

echo "::error::Instrumentation retry budget exhausted."
exit 1
