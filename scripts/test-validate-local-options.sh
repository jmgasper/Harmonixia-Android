#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
validate_script="${script_dir}/validate-local.sh"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

run_expect_exit() {
    run_script_expect_exit "$validate_script" "$@"
}

run_script_expect_exit() {
    local script_path="$1"
    local expected_exit="$2"
    shift 2

    local output
    local status
    set +e
    output="$("${script_path}" "$@" 2>&1)"
    status=$?
    set -e

    if [[ "$status" -ne "$expected_exit" ]]; then
        echo "$output" >&2
        fail "expected exit ${expected_exit}, got ${status} for ${script_path} args: $*"
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
assert_contains "$help_output" "--serial"
assert_contains "$help_output" "--smoke-avd"
assert_contains "$help_output" "--smoke-list-avds"
assert_contains "$help_output" "--keep-logs"
assert_contains "$help_output" "--smoke-keep-logs"
assert_contains "$help_output" "--option-tests"
assert_contains "$help_output" "--agp9-full-path"
assert_contains "$help_output" "AGP 9 Phase 2 static audit"
assert_contains "$help_output" "scripts/agp9-phase2-audit.sh"

help_alias_output="$(run_expect_exit 0 -h)"
assert_contains "$help_alias_output" "--serial"
assert_contains "$help_alias_output" "--option-tests"

smoke_help_keep_logs_base_output="$(run_expect_exit 0 --smoke-help --keep-logs)"
assert_contains "$smoke_help_keep_logs_base_output" "Smoke command:"
assert_contains "$smoke_help_keep_logs_base_output" "--help --keep-logs"
assert_not_contains "$smoke_help_keep_logs_base_output" "Running AGP 9 Phase 2 static audit gate..."

smoke_help_keep_logs_output="$(run_expect_exit 0 --smoke-help --smoke-keep-logs)"
assert_contains "$smoke_help_keep_logs_output" "Smoke command:"
assert_contains "$smoke_help_keep_logs_output" "--help --keep-logs"
assert_not_contains "$smoke_help_keep_logs_output" "Running AGP 9 Phase 2 static audit gate..."

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

option_tests_agp9_full_path_conflict_output="$(run_expect_exit 1 --option-tests --agp9-full-path)"
assert_contains "$option_tests_agp9_full_path_conflict_output" "--option-tests cannot be combined with compile/test/lint toggles or smoke execution flags."

option_tests_sim_root="$(mktemp -d /tmp/harmonixia-validate-local-option-tests.XXXXXX)"
trap 'rm -rf "${option_tests_sim_root}"' EXIT

mkdir -p "${option_tests_sim_root}/scripts"
cp "$validate_script" "${option_tests_sim_root}/scripts/validate-local.sh"
chmod +x "${option_tests_sim_root}/scripts/validate-local.sh"
cat > "${option_tests_sim_root}/scripts/test-local-validation-option-regressions.sh" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail

echo "Running shell syntax checks..."
echo "Shell syntax checks passed."
echo "Running AGP 9 phase 2 audit option regressions..."
echo "agp9-phase2-audit option tests passed."
echo "Running validate-local option regressions..."
echo "validate-local option tests passed."
echo "Running validate-local AGP gate simulator regressions..."
echo "validate-local AGP gate simulator tests passed."
echo "Running smoke-debug-emulator option regressions..."
echo "smoke-debug-emulator option tests passed."
echo "Running hardcoded format-template scanner regression..."
echo "PASS: no hardcoded Kotlin format templates found in app/src/main/java."
echo "Running hardcoded UI text-literal scanner regression..."
echo "PASS: no hardcoded UI text literals found in app/src/main/java/com/harmonixia/android/ui."
echo "Running hardcoded UI text-literal scanner self-test..."
echo "check-hardcoded-ui-text-literals tests passed."
echo "All local validation option regressions passed."
STUB
chmod +x "${option_tests_sim_root}/scripts/test-local-validation-option-regressions.sh"

option_tests_success_output="$(run_script_expect_exit "${option_tests_sim_root}/scripts/validate-local.sh" 0 --option-tests)"
assert_contains "$option_tests_success_output" "Running local validation option regression tests..."
assert_contains "$option_tests_success_output" "Running AGP 9 phase 2 audit option regressions..."
assert_contains "$option_tests_success_output" "Running validate-local option regressions..."
assert_contains "$option_tests_success_output" "Running hardcoded UI text-literal scanner self-test..."
assert_contains "$option_tests_success_output" "check-hardcoded-ui-text-literals tests passed."
assert_contains "$option_tests_success_output" "All local validation option regressions passed."
assert_not_contains "$option_tests_success_output" "Running AGP 9 Phase 2 static audit gate..."

agp9_full_path_smoke_help_conflict_output="$(run_expect_exit 1 --agp9-full-path --smoke-help)"
assert_contains "$agp9_full_path_smoke_help_conflict_output" "--agp9-full-path cannot be combined with informational smoke-only modes"

agp9_full_path_skip_conflict_output="$(run_expect_exit 1 --agp9-full-path --skip-lint)"
assert_contains "$agp9_full_path_skip_conflict_output" "--agp9-full-path cannot be combined with compile/test/lint skip flags."

unknown_argument_output="$(run_expect_exit 1 --definitely-unknown-flag)"
assert_contains "$unknown_argument_output" "Unknown argument: --definitely-unknown-flag"

missing_value_output="$(run_expect_exit 1 --smoke-task)"
assert_contains "$missing_value_output" "Missing value for --smoke-task"

echo "validate-local option tests passed."
