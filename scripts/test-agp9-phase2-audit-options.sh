#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
audit_script="${script_dir}/agp9-phase2-audit.sh"

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
    output="$(${audit_script} "$@" 2>&1)"
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
assert_contains "$help_output" "--with-gradle-checks"
assert_contains "$help_output" "--help"
assert_contains "$help_output" "Static checks:"
assert_contains "$help_output" "Root plugin versions"
assert_contains "$help_output" "Removal of temporary AGP 9 opt-out flags"

help_alias_output="$(run_expect_exit 0 -h)"
assert_contains "$help_alias_output" "--with-gradle-checks"
assert_contains "$help_alias_output" "Static checks:"

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"
assert_contains "$unknown_argument_output" "Usage:"

static_audit_output="$(run_expect_exit 0)"
assert_contains "$static_audit_output" "AGP 9 Phase 2 Audit"
assert_contains "$static_audit_output" "PASS: Root AGP plugin version"
assert_contains "$static_audit_output" "PASS: Temporary AGP opt-out flag android.builtInKotlin=false absent"
assert_contains "$static_audit_output" "Summary: PASS="
assert_contains "$static_audit_output" "FAIL=0"
assert_not_contains "$static_audit_output" "Running Phase 2 Gradle validation gates..."

echo "agp9-phase2-audit option tests passed."
