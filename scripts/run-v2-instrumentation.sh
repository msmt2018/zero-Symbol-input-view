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

./gradlew :zero-symbol-input-view-v2:connectedDebugAndroidTest
