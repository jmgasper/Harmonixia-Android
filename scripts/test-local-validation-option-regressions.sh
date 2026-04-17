#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

show_usage() {
    cat <<USAGE
Usage: $(basename "$0") [--syntax-only | --behavior-only] [--dry-run]

Run local validation option regressions.

Modes:
  --syntax-only    Run only bash syntax checks.
  --behavior-only  Run only behavioral option regression scripts.
  --dry-run        Print selected mode(s) without executing checks.
  --help           Show this help.

Default:
  Runs both syntax checks and behavioral regressions.
USAGE
}

syntax_only="false"
behavior_only="false"
dry_run="false"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --syntax-only)
            syntax_only="true"
            shift
            ;;
        --behavior-only)
            behavior_only="true"
            shift
            ;;
        --dry-run)
            dry_run="true"
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            show_usage >&2
            exit 1
            ;;
    esac
done

if [[ "$syntax_only" == "true" && "$behavior_only" == "true" ]]; then
    echo "Cannot combine --syntax-only with --behavior-only." >&2
    show_usage >&2
    exit 1
fi

run_syntax="true"
run_behavior="true"
if [[ "$syntax_only" == "true" ]]; then
    run_behavior="false"
fi
if [[ "$behavior_only" == "true" ]]; then
    run_syntax="false"
fi

if [[ "$dry_run" == "true" ]]; then
    echo "Dry run mode enabled."
    if [[ "$run_syntax" == "true" ]]; then
        echo "Would run shell syntax checks."
    fi
    if [[ "$run_behavior" == "true" ]]; then
        echo "Would run behavioral option regressions."
    fi
    if [[ "$run_syntax" == "true" && "$run_behavior" == "true" ]]; then
        echo "Dry run summary: syntax and behavioral regressions."
    elif [[ "$run_syntax" == "true" ]]; then
        echo "Dry run summary: syntax regressions only."
    else
        echo "Dry run summary: behavioral regressions only."
    fi
    exit 0
fi

if [[ "$run_syntax" == "true" ]]; then
    echo "Running shell syntax checks..."
    bash -n "${script_dir}/validate-local.sh"
    bash -n "${script_dir}/smoke-debug-emulator.sh"
    bash -n "${script_dir}/test-validate-local-options.sh"
    bash -n "${script_dir}/test-smoke-debug-emulator-options.sh"
    bash -n "${script_dir}/test-local-validation-option-regressions-runner.sh"
    echo "Shell syntax checks passed."
fi

if [[ "$run_behavior" == "true" ]]; then
    echo "Running validate-local option regressions..."
    "${script_dir}/test-validate-local-options.sh"

    echo "Running smoke-debug-emulator option regressions..."
    "${script_dir}/test-smoke-debug-emulator-options.sh"
fi

if [[ "$run_syntax" == "true" && "$run_behavior" == "true" ]]; then
    echo "All local validation option regressions passed."
elif [[ "$run_syntax" == "true" ]]; then
    echo "Local validation syntax checks passed."
else
    echo "Local validation behavioral regressions passed."
fi
