#!/usr/bin/env bash
# Boots a headless Android emulator, runs the given command against it, and reports the
# tail of any failing step as a workflow annotation so failures are readable without raw logs.
#   usage: android-emulator.sh <command...>
set -uo pipefail

API_LEVEL="${API_LEVEL:-34}"
IMAGE="system-images;android-${API_LEVEL};google_apis;x86_64"
LOG_DIR="${RUNNER_TEMP:-/tmp}/android-emulator"
mkdir -p "$LOG_DIR"

SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/emulator:$SDK/platform-tools:$PATH"

annotate() {
  local title="$1" file="$2"
  local body
  body=$(tail -n 80 "$file" | sed -e 's/%/%25/g' -e 's/\r//g' | sed ':a;N;$!ba;s/\n/%0A/g')
  echo "::error title=${title}::${body}"
}

step() {
  local name="$1"; shift
  local log="$LOG_DIR/${name// /_}.log"
  echo "::group::${name}"
  if ! "$@" > >(tee "$log") 2>&1; then
    echo "::endgroup::"
    annotate "$name" "$log"
    exit 1
  fi
  echo "::endgroup::"
}

wait_for_boot() {
  adb wait-for-device
  for _ in $(seq 1 120); do
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      adb shell input keyevent 82 || true
      return 0
    fi
    sleep 5
  done
  echo "Emulator did not finish booting in 10 minutes"
  tail -n 60 "$LOG_DIR/emulator-process.log" || true
  return 1
}

step "Install emulator image" bash -c "yes | sdkmanager --licenses >/dev/null; sdkmanager --install emulator platform-tools '$IMAGE'"
step "Create AVD" bash -c "echo no | avdmanager create avd --force -n ci -k '$IMAGE' -d pixel_6"

nohup emulator -avd ci -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader_indirect -camera-back none -camera-front none \
  > "$LOG_DIR/emulator-process.log" 2>&1 &

step "Boot emulator" wait_for_boot
if [ "${DISABLE_ANIMATIONS:-true}" = "true" ]; then
  adb shell settings put global window_animation_scale 0
  adb shell settings put global transition_animation_scale 0
  adb shell settings put global animator_duration_scale 0
fi

step "Run on emulator" "$@"
