#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

with_smoke="false"
avd_name="Medium_Phone"
smoke_avd_explicit="false"
smoke_serial=""
smoke_no_launch="false"
smoke_connect_timeout=""
smoke_boot_timeout=""
smoke_launch_wait=""
smoke_app_id=""
smoke_task=""
smoke_list_avds="false"
smoke_options_used="false"
run_compile="true"
run_test="true"
run_lint="true"

usage() {
    cat <<USAGE
Usage: $(basename "$0") [options]

Run local validation gates before committing:
  1. :app:compileDebugKotlin
  2. :app:testDebugUnitTest
  3. :app:lintDebug
Optional:
  4. emulator smoke test via scripts/smoke-debug-emulator.sh

Options:
  --with-smoke         Include emulator smoke validation
  --avd <name>         AVD name for smoke validation (default: ${avd_name})
  --serial <id>        adb emulator serial for smoke validation (example: emulator-5554)
  --no-launch          Forward --no-launch to smoke validation
  --connect-timeout <s> Forward smoke adb connect timeout in seconds
  --boot-timeout <s>   Forward smoke boot completion timeout in seconds
  --launch-wait <s>    Forward post-launch wait seconds to smoke validation
  --smoke-app-id <id>  Forward app id to smoke validation
  --smoke-task <task>  Forward Gradle install task to smoke validation
  --list-avds          List AVDs via smoke validation (implies smoke-only)
  --smoke-only         Disable compile/test/lint gates and run smoke only
  --skip-compile       Skip :app:compileDebugKotlin gate
  --skip-test          Skip :app:testDebugUnitTest gate
  --skip-lint          Skip :app:lintDebug gate
  --help               Show this help

Documentation:
  docs/local-validation-workflow.md
USAGE
}

require_option_value() {
    local option="$1"
    local value="${2:-}"
    if [[ -z "$value" || "$value" == --* ]]; then
        echo "Missing value for ${option}" >&2
        usage >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

require_positive_integer() {
    local option="$1"
    local value="$2"
    if ! [[ "$value" =~ ^[1-9][0-9]*$ ]]; then
        echo "Invalid value for ${option}: ${value}" >&2
        echo "Expected a positive integer." >&2
        usage >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

require_emulator_serial() {
    local option="$1"
    local value="$2"
    if ! [[ "$value" =~ ^emulator-[0-9]+$ ]]; then
        echo "Invalid value for ${option}: ${value}" >&2
        echo "Expected an emulator adb serial like emulator-5554." >&2
        usage >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --with-smoke)
            with_smoke="true"
            shift
            ;;
        --avd)
            avd_name="$(require_option_value "$1" "${2:-}")"
            smoke_avd_explicit="true"
            smoke_options_used="true"
            shift 2
            ;;
        --serial)
            smoke_serial="$(require_emulator_serial "$1" "$(require_option_value "$1" "${2:-}")")"
            smoke_options_used="true"
            shift 2
            ;;
        --no-launch)
            smoke_no_launch="true"
            smoke_options_used="true"
            shift
            ;;
        --connect-timeout)
            smoke_connect_timeout="$(require_positive_integer "$1" "$(require_option_value "$1" "${2:-}")")"
            smoke_options_used="true"
            shift 2
            ;;
        --boot-timeout)
            smoke_boot_timeout="$(require_positive_integer "$1" "$(require_option_value "$1" "${2:-}")")"
            smoke_options_used="true"
            shift 2
            ;;
        --launch-wait)
            smoke_launch_wait="$(require_positive_integer "$1" "$(require_option_value "$1" "${2:-}")")"
            smoke_options_used="true"
            shift 2
            ;;
        --smoke-app-id)
            smoke_app_id="$(require_option_value "$1" "${2:-}")"
            smoke_options_used="true"
            shift 2
            ;;
        --smoke-task)
            smoke_task="$(require_option_value "$1" "${2:-}")"
            smoke_options_used="true"
            shift 2
            ;;
        --list-avds)
            smoke_list_avds="true"
            smoke_options_used="true"
            with_smoke="true"
            run_compile="false"
            run_test="false"
            run_lint="false"
            shift
            ;;
        --smoke-only)
            with_smoke="true"
            run_compile="false"
            run_test="false"
            run_lint="false"
            shift
            ;;
        --skip-compile)
            run_compile="false"
            shift
            ;;
        --skip-test)
            run_test="false"
            shift
            ;;
        --skip-lint)
            run_lint="false"
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [[ "$with_smoke" != "true" && "$smoke_options_used" == "true" ]]; then
    echo "Smoke-specific flags require --with-smoke or --smoke-only." >&2
    usage >&2
    exit 1
fi

if [[ "$smoke_list_avds" != "true" && "$smoke_avd_explicit" == "true" && -n "$smoke_serial" ]]; then
    echo "Cannot combine --avd with --serial. Choose one target selector." >&2
    usage >&2
    exit 1
fi

if [[ "$smoke_list_avds" != "true" && "$smoke_no_launch" == "true" && "$smoke_avd_explicit" == "true" && -z "$smoke_serial" ]]; then
    echo "--no-launch cannot be combined with --avd unless --serial is also provided." >&2
    usage >&2
    exit 1
fi

if [[ "$smoke_list_avds" == "true" ]]; then
    if [[ "$smoke_avd_explicit" == "true" || -n "$smoke_serial" || "$smoke_no_launch" == "true" || -n "$smoke_connect_timeout" || -n "$smoke_boot_timeout" || -n "$smoke_launch_wait" || -n "$smoke_app_id" || -n "$smoke_task" ]]; then
        echo "--list-avds cannot be combined with runtime smoke options." >&2
        echo "Remove --avd/--serial/--no-launch/--connect-timeout/--boot-timeout/--launch-wait/--smoke-app-id/--smoke-task when listing AVDs." >&2
        usage >&2
        exit 1
    fi
fi

skip_java_preflight="false"
if [[ "$with_smoke" == "true" && "$smoke_list_avds" == "true" && "$run_compile" == "false" && "$run_test" == "false" && "$run_lint" == "false" ]]; then
    skip_java_preflight="true"
fi

if [[ "$skip_java_preflight" != "true" ]]; then
    if [[ -z "${JAVA_HOME:-}" ]]; then
        if [[ -d "$HOME/.jdks/jdk-17.0.17+10" ]]; then
            export JAVA_HOME="$HOME/.jdks/jdk-17.0.17+10"
            export PATH="$JAVA_HOME/bin:$PATH"
        fi
    fi

    if ! command -v java >/dev/null 2>&1; then
        echo "Java is required. Install JDK 17 and ensure java is on PATH." >&2
        exit 1
    fi

    java_version_line="$(java -version 2>&1 | head -n 1)"
    java_major="$(echo "${java_version_line}" | sed -E 's/.*version "([0-9]+).*/\1/')"
    if [[ "${java_major}" != "17" ]]; then
        echo "JDK 17 is required for local validation." >&2
        echo "Detected: ${java_version_line}" >&2
        echo "Set JAVA_HOME to a JDK 17 installation and retry." >&2
        exit 1
    fi
fi

gradle_tasks=()
if [[ "$run_compile" == "true" ]]; then
    gradle_tasks+=(":app:compileDebugKotlin")
fi
if [[ "$run_test" == "true" ]]; then
    gradle_tasks+=(":app:testDebugUnitTest")
fi
if [[ "$run_lint" == "true" ]]; then
    gradle_tasks+=(":app:lintDebug")
fi

if [[ "${#gradle_tasks[@]}" -gt 0 ]]; then
    echo "Running Gradle validation gates: ${gradle_tasks[*]}"
    (
        cd "$repo_root"
        ./gradlew --no-daemon "${gradle_tasks[@]}"
    )
elif [[ "$with_smoke" != "true" ]]; then
    echo "No validation gates selected. Enable at least one gate or use --with-smoke/--smoke-only." >&2
    echo "See docs/local-validation-workflow.md for examples." >&2
    exit 1
fi

if [[ "$with_smoke" == "true" ]]; then
    echo "Running emulator smoke gate..."
    smoke_args=()
    if [[ "$smoke_list_avds" == "true" ]]; then
        smoke_args+=(--list-avds)
    else
        if [[ -n "$smoke_serial" ]]; then
            smoke_args+=(--serial "$smoke_serial")
        else
            smoke_args+=(--avd "$avd_name")
        fi
        if [[ "$smoke_no_launch" == "true" ]]; then
            smoke_args+=(--no-launch)
        fi
        if [[ -n "$smoke_connect_timeout" ]]; then
            smoke_args+=(--connect-timeout "$smoke_connect_timeout")
        fi
        if [[ -n "$smoke_boot_timeout" ]]; then
            smoke_args+=(--boot-timeout "$smoke_boot_timeout")
        fi
        if [[ -n "$smoke_launch_wait" ]]; then
            smoke_args+=(--launch-wait "$smoke_launch_wait")
        fi
        if [[ -n "$smoke_app_id" ]]; then
            smoke_args+=(--app-id "$smoke_app_id")
        fi
        if [[ -n "$smoke_task" ]]; then
            smoke_args+=(--task "$smoke_task")
        fi
    fi
    echo "Smoke command args: ${smoke_args[*]}"
    "$script_dir/smoke-debug-emulator.sh" "${smoke_args[@]}"
fi

echo "Local validation passed."
