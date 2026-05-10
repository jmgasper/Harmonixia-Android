#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
validate_script="${script_dir}/validate-local.sh"

fail() {
    echo "FAIL: $*" >&2
    exit 1
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

assert_line_order() {
    local text="$1"
    local first="$2"
    local second="$3"

    local first_line
    local second_line
    first_line="$(printf '%s\n' "$text" | awk -v needle="$first" 'index($0, needle) > 0 {print NR; exit}')"
    second_line="$(printf '%s\n' "$text" | awk -v needle="$second" 'index($0, needle) > 0 {print NR; exit}')"

    if [[ -z "$first_line" || -z "$second_line" ]]; then
        echo "$text" >&2
        fail "missing expected lines for ordering check: '$first' then '$second'"
    fi

    if (( first_line >= second_line )); then
        echo "$text" >&2
        fail "expected '$first' before '$second'"
    fi
}

sim_root="$(mktemp -d /tmp/harmonixia-validate-local-agp-gate.XXXXXX)"
sim_repo="${sim_root}/repo"
last_output=""
last_events=""
declare -a run_env_overrides=()

cleanup() {
    rm -rf "$sim_root"
}
trap cleanup EXIT

mkdir -p "${sim_repo}/scripts" "${sim_repo}/fake-bin"

cp "$validate_script" "${sim_repo}/scripts/validate-local.sh"
chmod +x "${sim_repo}/scripts/validate-local.sh"

cat > "${sim_repo}/scripts/agp9-phase2-audit.sh" <<'SIM_AUDIT'
#!/usr/bin/env bash
set -euo pipefail
printf 'SIM_AUDIT: %s\n' "$*" >> "${SIM_EVENT_LOG:?}"
if [[ "${SIM_AUDIT_EXIT_CODE:-0}" != "0" ]]; then
    exit "${SIM_AUDIT_EXIT_CODE}"
fi
SIM_AUDIT
chmod +x "${sim_repo}/scripts/agp9-phase2-audit.sh"

cat > "${sim_repo}/scripts/smoke-debug-emulator.sh" <<'SIM_SMOKE'
#!/usr/bin/env bash
set -euo pipefail
printf 'SIM_SMOKE: %s\n' "$*" >> "${SIM_EVENT_LOG:?}"
if [[ "${SIM_SMOKE_EXIT_CODE:-0}" != "0" ]]; then
    exit "${SIM_SMOKE_EXIT_CODE}"
fi
SIM_SMOKE
chmod +x "${sim_repo}/scripts/smoke-debug-emulator.sh"

cat > "${sim_repo}/scripts/test-local-validation-option-regressions.sh" <<'SIM_OPTION_TESTS'
#!/usr/bin/env bash
set -euo pipefail
printf 'SIM_OPTION_TESTS\n' >> "${SIM_EVENT_LOG:?}"
SIM_OPTION_TESTS
chmod +x "${sim_repo}/scripts/test-local-validation-option-regressions.sh"

cat > "${sim_repo}/gradlew" <<'SIM_GRADLEW'
#!/usr/bin/env bash
set -euo pipefail
printf 'SIM_GRADLE: %s\n' "$*" >> "${SIM_EVENT_LOG:?}"
if [[ "${SIM_GRADLE_EXIT_CODE:-0}" != "0" ]]; then
    exit "${SIM_GRADLE_EXIT_CODE}"
fi
SIM_GRADLEW
chmod +x "${sim_repo}/gradlew"

cat > "${sim_repo}/fake-bin/java" <<'SIM_JAVA'
#!/usr/bin/env bash
if [[ "${1:-}" == "-version" ]]; then
    echo 'openjdk version "17.0.99"'
    exit 0
fi
echo "java simulator only supports -version" >&2
exit 1
SIM_JAVA
chmod +x "${sim_repo}/fake-bin/java"

run_expect_exit_with_events() {
    local expected_exit="$1"
    shift

    local event_log
    event_log="$(mktemp /tmp/harmonixia-validate-local-events.XXXXXX)"

    local output
    local status
    set +e
    output="$(
        env \
            "PATH=${sim_repo}/fake-bin:${PATH}" \
            "SIM_EVENT_LOG=${event_log}" \
            "${run_env_overrides[@]}" \
            "${sim_repo}/scripts/validate-local.sh" \
            "$@" 2>&1
    )"
    status=$?
    set -e

    if [[ -f "$event_log" ]]; then
        last_events="$(cat "$event_log")"
    else
        last_events=""
    fi
    rm -f "$event_log"

    if [[ "$status" -ne "$expected_exit" ]]; then
        echo "$output" >&2
        echo "$last_events" >&2
        fail "expected exit ${expected_exit}, got ${status} for args: $*"
    fi

    last_output="$output"
    run_env_overrides=()
}

run_expect_exit_with_events 0
assert_contains "$last_output" "Running AGP 9 Phase 2 static audit gate..."
assert_contains "$last_output" "Running Gradle validation gates: :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug"
assert_contains "$last_output" "Local validation passed."
assert_contains "$last_events" "SIM_AUDIT:"
assert_contains "$last_events" "SIM_GRADLE: --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug"
assert_line_order "$last_events" "SIM_AUDIT:" "SIM_GRADLE:"

run_expect_exit_with_events 0 --agp9-full-path --serial emulator-5554 --no-launch --task :app:installDebug --connect-timeout 15 --boot-timeout 20 --launch-wait 1
assert_contains "$last_output" "Running AGP 9 full-path validation gate (audit + compile/test/lint + smoke)..."
assert_contains "$last_output" "Running AGP 9 Phase 2 static audit gate..."
assert_contains "$last_output" "Running Gradle validation gates: :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug"
assert_contains "$last_output" "Running emulator smoke gate..."
assert_contains "$last_events" "SIM_AUDIT:"
assert_contains "$last_events" "SIM_GRADLE: --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug"
assert_contains "$last_events" "SIM_SMOKE: --serial emulator-5554 --no-launch --connect-timeout 15 --boot-timeout 20 --launch-wait 1 --task :app:installDebug"
assert_line_order "$last_events" "SIM_AUDIT:" "SIM_GRADLE:"
assert_line_order "$last_events" "SIM_GRADLE:" "SIM_SMOKE:"

run_expect_exit_with_events 0 --skip-test --skip-lint
assert_contains "$last_output" "Running Gradle validation gates: :app:compileDebugKotlin"
assert_not_contains "$last_output" ":app:testDebugUnitTest"
assert_not_contains "$last_output" ":app:lintDebug"
assert_contains "$last_events" "SIM_AUDIT:"
assert_contains "$last_events" "SIM_GRADLE: --no-daemon :app:compileDebugKotlin"
assert_not_contains "$last_events" ":app:testDebugUnitTest"
assert_not_contains "$last_events" ":app:lintDebug"
assert_line_order "$last_events" "SIM_AUDIT:" "SIM_GRADLE:"

run_expect_exit_with_events 0 --smoke-help
assert_contains "$last_output" "Showing emulator smoke help..."
assert_contains "$last_events" "SIM_SMOKE: --help"
assert_not_contains "$last_events" "SIM_AUDIT:"
assert_not_contains "$last_events" "SIM_GRADLE:"

chmod -x "${sim_repo}/scripts/agp9-phase2-audit.sh"
run_expect_exit_with_events 1
assert_contains "$last_output" "AGP 9 audit script is missing or not executable"
assert_not_contains "$last_events" "SIM_GRADLE:"
chmod +x "${sim_repo}/scripts/agp9-phase2-audit.sh"

run_env_overrides=("SIM_AUDIT_EXIT_CODE=17")
run_expect_exit_with_events 17
assert_contains "$last_output" "Running AGP 9 Phase 2 static audit gate..."
assert_not_contains "$last_output" "Running Gradle validation gates:"
assert_contains "$last_events" "SIM_AUDIT:"
assert_not_contains "$last_events" "SIM_GRADLE:"

echo "validate-local AGP gate simulator tests passed."
