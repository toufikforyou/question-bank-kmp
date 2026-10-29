#!/usr/bin/env bash
# Runs a build/test command and, if it fails, turns compiler and test errors from its output into
# workflow annotations so they are readable without opening the raw logs.
#   usage: report-errors.sh <title> <command...>
set -uo pipefail

title="$1"; shift
log="${RUNNER_TEMP:-/tmp}/report-$(date +%s%N).log"

"$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}

if [ "$status" -ne 0 ]; then
  grep -E "(error:|^e: |: error |XCTAssert|failed \(|Test Case .* failed|FAILED|What went wrong|Testing failed)" "$log" \
    | grep -v "^warning" | awk '!seen[$0]++' | head -n 40 \
    | while IFS= read -r line; do
        echo "::error title=${title}::$(echo "$line" | sed -e 's/%/%25/g' -e 's/\r//g' | cut -c1-900)"
      done
fi
exit "$status"
