#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Running validate-local option regressions..."
"${script_dir}/test-validate-local-options.sh"

echo "Running smoke-debug-emulator option regressions..."
"${script_dir}/test-smoke-debug-emulator-options.sh"

echo "All local validation option regressions passed."
