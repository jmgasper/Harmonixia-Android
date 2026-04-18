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
assert_contains "$help_alias_output" "--dry-run"

help_with_dry_run_output="$(run_expect_exit 0 --dry-run --help)"
assert_contains "$help_with_dry_run_output" "Usage:"
assert_contains "$help_with_dry_run_output" "--dry-run"
assert_not_contains "$help_with_dry_run_output" "Dry run mode enabled."

help_alias_with_dry_run_output="$(run_expect_exit 0 --dry-run -h)"
assert_contains "$help_alias_with_dry_run_output" "Usage:"
assert_contains "$help_alias_with_dry_run_output" "--dry-run"
assert_not_contains "$help_alias_with_dry_run_output" "Dry run mode enabled."

help_with_mode_output="$(run_expect_exit 0 --syntax-only --help)"
assert_contains "$help_with_mode_output" "Usage:"
assert_contains "$help_with_mode_output" "--syntax-only"
assert_not_contains "$help_with_mode_output" "Would run shell syntax checks."

help_first_with_unknown_output="$(run_expect_exit 0 --help --definitely-unknown-flag)"
assert_contains "$help_first_with_unknown_output" "Usage:"
assert_not_contains "$help_first_with_unknown_output" "Unknown argument: --definitely-unknown-flag"

help_alias_first_with_unknown_output="$(run_expect_exit 0 -h --definitely-unknown-flag)"
assert_contains "$help_alias_first_with_unknown_output" "Usage:"
assert_not_contains "$help_alias_first_with_unknown_output" "Unknown argument: --definitely-unknown-flag"

help_first_with_conflict_flags_output="$(run_expect_exit 0 --help --syntax-only --behavior-only)"
assert_contains "$help_first_with_conflict_flags_output" "Usage:"
assert_not_contains "$help_first_with_conflict_flags_output" "Cannot combine --syntax-only with --behavior-only."

help_alias_first_with_conflict_flags_output="$(run_expect_exit 0 -h --syntax-only --behavior-only)"
assert_contains "$help_alias_first_with_conflict_flags_output" "Usage:"
assert_not_contains "$help_alias_first_with_conflict_flags_output" "Cannot combine --syntax-only with --behavior-only."

dry_run_default_output="$(run_expect_exit 0 --dry-run)"
assert_contains "$dry_run_default_output" "Dry run mode enabled."
assert_contains "$dry_run_default_output" "Would run shell syntax checks."
assert_contains "$dry_run_default_output" "Would run behavioral option regressions."
assert_contains "$dry_run_default_output" "Dry run summary: syntax and behavioral regressions."
assert_not_contains "$dry_run_default_output" "Running shell syntax checks..."
assert_not_contains "$dry_run_default_output" "Running validate-local option regressions..."
assert_not_contains "$dry_run_default_output" "Running smoke-debug-emulator option regressions..."

dry_run_duplicate_flag_output="$(run_expect_exit 0 --dry-run --dry-run)"
assert_contains "$dry_run_duplicate_flag_output" "Dry run mode enabled."
assert_contains "$dry_run_duplicate_flag_output" "Dry run summary: syntax and behavioral regressions."

dry_run_syntax_output="$(run_expect_exit 0 --dry-run --syntax-only)"
assert_contains "$dry_run_syntax_output" "Dry run mode enabled."
assert_contains "$dry_run_syntax_output" "Would run shell syntax checks."
assert_contains "$dry_run_syntax_output" "Dry run summary: syntax regressions only."
assert_not_contains "$dry_run_syntax_output" "Would run behavioral option regressions."
assert_not_contains "$dry_run_syntax_output" "Running shell syntax checks..."

dry_run_syntax_duplicate_flag_output="$(run_expect_exit 0 --dry-run --syntax-only --syntax-only)"
assert_contains "$dry_run_syntax_duplicate_flag_output" "Dry run summary: syntax regressions only."
assert_not_contains "$dry_run_syntax_duplicate_flag_output" "Would run behavioral option regressions."

dry_run_syntax_reversed_output="$(run_expect_exit 0 --syntax-only --dry-run)"
assert_contains "$dry_run_syntax_reversed_output" "Dry run mode enabled."
assert_contains "$dry_run_syntax_reversed_output" "Would run shell syntax checks."
assert_contains "$dry_run_syntax_reversed_output" "Dry run summary: syntax regressions only."
assert_not_contains "$dry_run_syntax_reversed_output" "Would run behavioral option regressions."

dry_run_behavior_output="$(run_expect_exit 0 --dry-run --behavior-only)"
assert_contains "$dry_run_behavior_output" "Dry run mode enabled."
assert_contains "$dry_run_behavior_output" "Would run behavioral option regressions."
assert_contains "$dry_run_behavior_output" "Dry run summary: behavioral regressions only."
assert_not_contains "$dry_run_behavior_output" "Would run shell syntax checks."
assert_not_contains "$dry_run_behavior_output" "Running validate-local option regressions..."
assert_not_contains "$dry_run_behavior_output" "Running smoke-debug-emulator option regressions..."

dry_run_behavior_reversed_output="$(run_expect_exit 0 --behavior-only --dry-run)"
assert_contains "$dry_run_behavior_reversed_output" "Dry run mode enabled."
assert_contains "$dry_run_behavior_reversed_output" "Would run behavioral option regressions."
assert_contains "$dry_run_behavior_reversed_output" "Dry run summary: behavioral regressions only."
assert_not_contains "$dry_run_behavior_reversed_output" "Would run shell syntax checks."

dry_run_behavior_duplicate_flag_output="$(run_expect_exit 0 --dry-run --behavior-only --behavior-only)"
assert_contains "$dry_run_behavior_duplicate_flag_output" "Dry run summary: behavioral regressions only."
assert_not_contains "$dry_run_behavior_duplicate_flag_output" "Would run shell syntax checks."

conflict_output="$(run_expect_exit 1 --syntax-only --behavior-only)"
assert_contains "$conflict_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_output" "Usage:"

conflict_behavior_first_output="$(run_expect_exit 1 --behavior-only --syntax-only)"
assert_contains "$conflict_behavior_first_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_behavior_first_output" "Usage:"

conflict_duplicate_syntax_output="$(run_expect_exit 1 --syntax-only --syntax-only --behavior-only)"
assert_contains "$conflict_duplicate_syntax_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_duplicate_syntax_output" "Usage:"

conflict_duplicate_behavior_output="$(run_expect_exit 1 --syntax-only --behavior-only --behavior-only)"
assert_contains "$conflict_duplicate_behavior_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_duplicate_behavior_output" "Usage:"

conflict_behavior_first_duplicate_behavior_output="$(run_expect_exit 1 --behavior-only --behavior-only --syntax-only)"
assert_contains "$conflict_behavior_first_duplicate_behavior_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$conflict_behavior_first_duplicate_behavior_output" "Usage:"

dry_run_conflict_output="$(run_expect_exit 1 --dry-run --syntax-only --behavior-only)"
assert_contains "$dry_run_conflict_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$dry_run_conflict_output" "Usage:"

dry_run_conflict_behavior_first_output="$(run_expect_exit 1 --dry-run --behavior-only --syntax-only)"
assert_contains "$dry_run_conflict_behavior_first_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$dry_run_conflict_behavior_first_output" "Usage:"

dry_run_conflict_reversed_output="$(run_expect_exit 1 --syntax-only --behavior-only --dry-run)"
assert_contains "$dry_run_conflict_reversed_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$dry_run_conflict_reversed_output" "Usage:"

dry_run_conflict_mixed_order_output="$(run_expect_exit 1 --syntax-only --dry-run --behavior-only)"
assert_contains "$dry_run_conflict_mixed_order_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$dry_run_conflict_mixed_order_output" "Usage:"

dry_run_conflict_duplicate_behavior_output="$(run_expect_exit 1 --dry-run --syntax-only --behavior-only --behavior-only)"
assert_contains "$dry_run_conflict_duplicate_behavior_output" "Cannot combine --syntax-only with --behavior-only."
assert_contains "$dry_run_conflict_duplicate_behavior_output" "Usage:"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

unknown_before_help_output="$(run_expect_exit 1 --definitely-unknown-flag --help)"
assert_contains "$unknown_before_help_output" "Unknown argument: --definitely-unknown-flag"

unknown_before_help_alias_output="$(run_expect_exit 1 --definitely-unknown-flag -h)"
assert_contains "$unknown_before_help_alias_output" "Unknown argument: --definitely-unknown-flag"

dry_run_unknown_argument_output="$(run_expect_exit 1 --dry-run --definitely-unknown-flag)"
assert_contains "$dry_run_unknown_argument_output" "Unknown argument: --definitely-unknown-flag"
assert_not_contains "$dry_run_unknown_argument_output" "Dry run mode enabled."

dry_run_unknown_before_help_output="$(run_expect_exit 1 --dry-run --definitely-unknown-flag --help)"
assert_contains "$dry_run_unknown_before_help_output" "Unknown argument: --definitely-unknown-flag"
assert_not_contains "$dry_run_unknown_before_help_output" "Dry run mode enabled."

echo "local validation option regression runner tests passed."
