#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

with_smoke="false"
avd_name="Medium_Phone"

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

echo "Running Gradle validation gates (compile + unit tests + lint)..."
(
    cd "$repo_root"
    ./gradlew --no-daemon \
        :app:compileDebugKotlin \
        :app:testDebugUnitTest \
        :app:lintDebug
)

if [[ "$with_smoke" == "true" ]]; then
    echo "Running emulator smoke gate..."
    "$script_dir/smoke-debug-emulator.sh" --avd "$avd_name"
fi

echo "Local validation passed."
