#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runner_script="${script_dir}/test-local-validation-option-regressions.sh"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

run_expect_exit() {
    local expected_exit="$1"
    shift

    local output
    local status
    set +e
    output="$("${runner_script}" "$@" 2>&1)"
    status=$?
    set -e

    if [[ "$status" -ne "$expected_exit" ]]; then
        echo "$output" >&2
        fail "expected exit ${expected_exit}, got ${status} for args: $*"
    fi

    printf '%s' "$output"
}

assert_contains() {
    local text="$1"
    local expected_substring="$2"
    if [[ "$text" != *"$expected_substring"* ]]; then
        echo "$text" >&2
        fail "expected output to contain: $expected_substring"
    fi
}

assert_not_contains() {
    local text="$1"
    local unexpected_substring="$2"
    if [[ "$text" == *"$unexpected_substring"* ]]; then
        echo "$text" >&2
        fail "expected output to not contain: $unexpected_substring"
    fi
}

help_output="$(run_expect_exit 0 --help)"
assert_contains "$help_output" "--syntax-only"
assert_contains "$help_output" "--behavior-only"
assert_contains "$help_output" "Default:"

help_alias_output="$(run_expect_exit 0 -h)"
assert_contains "$help_alias_output" "--syntax-only"
assert_contains "$help_alias_output" "--behavior-only"

default_output="$(run_expect_exit 0)"
assert_contains "$default_output" "Running shell syntax checks..."
assert_contains "$default_output" "Running validate-local option regressions..."
assert_contains "$default_output" "Running smoke-debug-emulator option regressions..."
assert_contains "$default_output" "All local validation option regressions passed."

syntax_only_output="$(run_expect_exit 0 --syntax-only)"
assert_contains "$syntax_only_output" "Running shell syntax checks..."
assert_contains "$syntax_only_output" "Shell syntax checks passed."
assert_contains "$syntax_only_output" "Local validation syntax checks passed."
assert_not_contains "$syntax_only_output" "Running validate-local option regressions..."
assert_not_contains "$syntax_only_output" "Running smoke-debug-emulator option regressions..."

behavior_only_output="$(run_expect_exit 0 --behavior-only)"
assert_contains "$behavior_only_output" "Running validate-local option regressions..."
assert_contains "$behavior_only_output" "validate-local option tests passed."
assert_contains "$behavior_only_output" "Running smoke-debug-emulator option regressions..."
assert_contains "$behavior_only_output" "smoke-debug-emulator option tests passed."
assert_contains "$behavior_only_output" "Local validation behavioral regressions passed."
assert_not_contains "$behavior_only_output" "Running shell syntax checks..."
assert_not_contains "$behavior_only_output" "Shell syntax checks passed."

conflict_output="$(run_expect_exit 1 --syntax-only --behavior-only)"
assert_contains "$conflict_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_output" "Usage:"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

echo "local validation option regression runner tests passed."
