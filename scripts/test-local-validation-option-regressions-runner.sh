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
assert_contains "$help_output" "--dry-run"
assert_contains "$help_output" "Default:"

help_alias_output="$(run_expect_exit 0 -h)"
assert_contains "$help_alias_output" "--syntax-only"
assert_contains "$help_alias_output" "--behavior-only"

dry_run_default_output="$(run_expect_exit 0 --dry-run)"
assert_contains "$dry_run_default_output" "Dry run mode enabled."
assert_contains "$dry_run_default_output" "Would run shell syntax checks."
assert_contains "$dry_run_default_output" "Would run behavioral option regressions."
assert_contains "$dry_run_default_output" "Dry run summary: syntax and behavioral regressions."

dry_run_syntax_output="$(run_expect_exit 0 --dry-run --syntax-only)"
assert_contains "$dry_run_syntax_output" "Dry run mode enabled."
assert_contains "$dry_run_syntax_output" "Would run shell syntax checks."
assert_contains "$dry_run_syntax_output" "Dry run summary: syntax regressions only."
assert_not_contains "$dry_run_syntax_output" "Would run behavioral option regressions."

dry_run_behavior_output="$(run_expect_exit 0 --dry-run --behavior-only)"
assert_contains "$dry_run_behavior_output" "Dry run mode enabled."
assert_contains "$dry_run_behavior_output" "Would run behavioral option regressions."
assert_contains "$dry_run_behavior_output" "Dry run summary: behavioral regressions only."
assert_not_contains "$dry_run_behavior_output" "Would run shell syntax checks."

syntax_only_output="$(run_expect_exit 0 --syntax-only)"
assert_contains "$syntax_only_output" "Running shell syntax checks..."
assert_contains "$syntax_only_output" "Shell syntax checks passed."
assert_contains "$syntax_only_output" "Local validation syntax checks passed."
assert_not_contains "$syntax_only_output" "Running validate-local option regressions..."
assert_not_contains "$syntax_only_output" "Running smoke-debug-emulator option regressions..."

conflict_output="$(run_expect_exit 1 --syntax-only --behavior-only)"
assert_contains "$conflict_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_output" "Usage:"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

echo "local validation option regression runner tests passed."
