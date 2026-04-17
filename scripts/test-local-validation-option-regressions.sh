#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Running shell syntax checks..."
bash -n "${script_dir}/validate-local.sh"
bash -n "${script_dir}/smoke-debug-emulator.sh"
bash -n "${script_dir}/test-validate-local-options.sh"
bash -n "${script_dir}/test-smoke-debug-emulator-options.sh"
echo "Shell syntax checks passed."

echo "Running validate-local option regressions..."
"${script_dir}/test-validate-local-options.sh"

echo "Running smoke-debug-emulator option regressions..."
"${script_dir}/test-smoke-debug-emulator-options.sh"

echo "All local validation option regressions passed."
