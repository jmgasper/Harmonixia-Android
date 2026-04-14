#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

with_smoke="false"
avd_name="Medium_Phone"
smoke_serial=""
smoke_no_launch="false"
smoke_connect_timeout=""
smoke_boot_timeout=""
smoke_app_id=""
smoke_task=""
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
  --serial <id>        adb serial to target for smoke validation
  --no-launch          Forward --no-launch to smoke validation
  --connect-timeout <s> Forward smoke adb connect timeout in seconds
  --boot-timeout <s>   Forward smoke boot completion timeout in seconds
  --smoke-app-id <id>  Forward app id to smoke validation
  --smoke-task <task>  Forward Gradle install task to smoke validation
  --skip-compile       Skip :app:compileDebugKotlin gate
  --skip-test          Skip :app:testDebugUnitTest gate
  --skip-lint          Skip :app:lintDebug gate
  --help               Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --with-smoke)
            with_smoke="true"
            shift
            ;;
        --avd)
            avd_name="$2"
            shift 2
            ;;
        --serial)
            smoke_serial="$2"
            shift 2
            ;;
        --no-launch)
            smoke_no_launch="true"
            shift
            ;;
        --connect-timeout)
            smoke_connect_timeout="$2"
            shift 2
            ;;
        --boot-timeout)
            smoke_boot_timeout="$2"
            shift 2
            ;;
        --smoke-app-id)
            smoke_app_id="$2"
            shift 2
            ;;
        --smoke-task)
            smoke_task="$2"
            shift 2
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
    echo "No validation gates selected. Enable at least one gate or use --with-smoke." >&2
    exit 1
fi

if [[ "$with_smoke" == "true" ]]; then
    echo "Running emulator smoke gate..."
    smoke_args=()
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
    if [[ -n "$smoke_app_id" ]]; then
        smoke_args+=(--app-id "$smoke_app_id")
    fi
    if [[ -n "$smoke_task" ]]; then
        smoke_args+=(--task "$smoke_task")
    fi
    "$script_dir/smoke-debug-emulator.sh" "${smoke_args[@]}"
fi

echo "Local validation passed."
