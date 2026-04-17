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
assert_contains "$help_output" "--serial"
assert_contains "$help_output" "--smoke-avd"
assert_contains "$help_output" "--smoke-list-avds"
assert_contains "$help_output" "--keep-logs"
assert_contains "$help_output" "--smoke-keep-logs"
assert_contains "$help_output" "--option-tests"

help_alias_output="$(run_expect_exit 0 -h)"
assert_contains "$help_alias_output" "--serial"
assert_contains "$help_alias_output" "--option-tests"

smoke_help_keep_logs_base_output="$(run_expect_exit 0 --smoke-help --keep-logs)"
assert_contains "$smoke_help_keep_logs_base_output" "Smoke command:"
assert_contains "$smoke_help_keep_logs_base_output" "--help --keep-logs"

smoke_help_keep_logs_output="$(run_expect_exit 0 --smoke-help --smoke-keep-logs)"
assert_contains "$smoke_help_keep_logs_output" "Smoke command:"
assert_contains "$smoke_help_keep_logs_output" "--help --keep-logs"

smoke_help_conflict_base_output="$(run_expect_exit 1 --smoke-help --serial emulator-5554)"
assert_contains "$smoke_help_conflict_base_output" "--smoke-help cannot be combined with runtime smoke options."
assert_contains "$smoke_help_conflict_base_output" "--smoke-* aliases."

smoke_help_conflict_output="$(run_expect_exit 1 --smoke-help --smoke-serial emulator-5554)"
assert_contains "$smoke_help_conflict_output" "--smoke-help cannot be combined with runtime smoke options."
assert_contains "$smoke_help_conflict_output" "--smoke-* aliases."

list_avds_conflict_base_output="$(run_expect_exit 1 --list-avds --task help)"
assert_contains "$list_avds_conflict_base_output" "--list-avds cannot be combined with runtime smoke options."
assert_contains "$list_avds_conflict_base_output" "--smoke-* aliases"

list_avds_conflict_output="$(run_expect_exit 1 --smoke-list-avds --smoke-task help)"
assert_contains "$list_avds_conflict_output" "--list-avds cannot be combined with runtime smoke options."
assert_contains "$list_avds_conflict_output" "--smoke-* aliases"

option_tests_conflict_output="$(run_expect_exit 1 --option-tests --with-smoke)"
assert_contains "$option_tests_conflict_output" "--option-tests cannot be combined with compile/test/lint toggles or smoke execution flags."

option_tests_avd_conflict_output="$(run_expect_exit 1 --option-tests --smoke-avd Temp_AVD)"
assert_contains "$option_tests_avd_conflict_output" "--option-tests cannot be combined with compile/test/lint toggles or smoke execution flags."
assert_contains "$option_tests_avd_conflict_output" "(default: Medium_Phone"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

missing_value_output="$(run_expect_exit 1 --smoke-task)"
assert_contains "$missing_value_output" "Missing value for --smoke-task"

echo "validate-local option tests passed."
