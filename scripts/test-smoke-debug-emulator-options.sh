#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
smoke_script="${script_dir}/smoke-debug-emulator.sh"

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
    output="$("${smoke_script}" "$@" 2>&1)"
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
assert_contains "$help_output" "--list-avds"
assert_contains "$help_output" "--task <gradle-task>"
assert_contains "$help_output" "--keep-logs"
assert_contains "$help_output" "--option-tests"

list_conflict_output="$(run_expect_exit 1 --list-avds --task help)"
assert_contains "$list_conflict_output" "--list-avds cannot be combined with runtime smoke options."
assert_contains "$list_conflict_output" "(default: :app:installDebug)"

target_conflict_output="$(run_expect_exit 1 --avd Medium_Phone --serial emulator-5554)"
assert_contains "$target_conflict_output" "Cannot combine --avd with --serial. Choose one target selector."

no_launch_avd_conflict_output="$(run_expect_exit 1 --no-launch --avd Medium_Phone)"
assert_contains "$no_launch_avd_conflict_output" "--no-launch cannot be combined with --avd unless --serial is also provided."

invalid_serial_output="$(run_expect_exit 1 --serial not-an-emulator)"
assert_contains "$invalid_serial_output" "Invalid value for --serial: not-an-emulator"
assert_contains "$invalid_serial_output" "Expected an emulator adb serial like emulator-5554."

invalid_timeout_output="$(run_expect_exit 1 --connect-timeout 0)"
assert_contains "$invalid_timeout_output" "Invalid value for --connect-timeout: 0"
assert_contains "$invalid_timeout_output" "Expected a positive integer."

option_tests_conflict_output="$(run_expect_exit 1 --option-tests --task help)"
assert_contains "$option_tests_conflict_output" "--option-tests cannot be combined with smoke execution flags."
assert_contains "$option_tests_conflict_output" "(default: :app:installDebug)"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

missing_value_output="$(run_expect_exit 1 --task)"
assert_contains "$missing_value_output" "Missing value for --task"

echo "smoke-debug-emulator option tests passed."
