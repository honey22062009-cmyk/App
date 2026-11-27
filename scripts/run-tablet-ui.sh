#!/usr/bin/env bash
# Preserve screenshots even when a test fails, then propagate the actual result.
set -u
gradle :app:connectedDebugAndroidTest --stacktrace --no-daemon
tablet_test_status=$?
adb pull /sdcard/Pictures/StrictBlockerQA/ tablet-ui-screenshots/
tablet_capture_status=$?
if [ "$tablet_test_status" -ne 0 ]; then
  exit "$tablet_test_status"
fi
exit "$tablet_capture_status"
