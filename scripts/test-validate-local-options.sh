#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
validate_script="${script_dir}/validate-local.sh"

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
    output="$("${validate_script}" "$@" 2>&1)"
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

help_output="$(run_expect_exit 0 --help)"
assert_contains "$help_output" "--smoke-avd"
assert_contains "$help_output" "--smoke-list-avds"
assert_contains "$help_output" "--smoke-keep-logs"

smoke_help_keep_logs_output="$(run_expect_exit 0 --smoke-help --smoke-keep-logs)"
assert_contains "$smoke_help_keep_logs_output" "Smoke command:"
assert_contains "$smoke_help_keep_logs_output" "--help --keep-logs"

smoke_help_conflict_output="$(run_expect_exit 1 --smoke-help --smoke-serial emulator-5554)"
assert_contains "$smoke_help_conflict_output" "--smoke-help cannot be combined with runtime smoke options."
assert_contains "$smoke_help_conflict_output" "--smoke-* aliases."

list_avds_conflict_output="$(run_expect_exit 1 --smoke-list-avds --smoke-task help)"
assert_contains "$list_avds_conflict_output" "--list-avds cannot be combined with runtime smoke options."
assert_contains "$list_avds_conflict_output" "--smoke-* aliases"

echo "validate-local option tests passed."
