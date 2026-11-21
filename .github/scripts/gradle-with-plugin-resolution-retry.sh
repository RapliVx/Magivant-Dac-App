#!/usr/bin/env bash

set -uo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "Usage: $0 <Gradle arguments...>" >&2
  exit 2
fi

log_dir="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"

for attempt in 1 2 3; do
  log_file="$log_dir/gradle-attempt-$attempt.log"
  gradle_args=("$@")
  if [[ "$attempt" -gt 1 ]]; then
    gradle_args=(--refresh-dependencies "${gradle_args[@]}")
  fi

  echo "Gradle attempt $attempt/3"
  ./gradlew "${gradle_args[@]}" 2>&1 | tee "$log_file"
  gradle_status=${PIPESTATUS[0]}

  if [[ "$gradle_status" -eq 0 ]]; then
    exit 0
  fi

  if ! grep -Eiq 'plugin .* was not found in any of the following sources|could not resolve plugin artifact' "$log_file"; then
    exit "$gradle_status"
  fi

  if [[ "$attempt" -lt 3 ]]; then
    echo "Plugin resolution failed; refreshing dependencies and retrying in $((attempt * 5)) seconds." >&2
    sleep "$((attempt * 5))"
  fi
done

exit 1
