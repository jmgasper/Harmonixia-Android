#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

output_dir="${1:-${repo_root}/build/testing}"
source_apk="${repo_root}/app/build/outputs/apk/debug/app-debug.apk"
target_apk="${output_dir}/Harmonixia-debug.apk"

if ! command -v java >/dev/null 2>&1; then
    echo "Java is required. Install JDK 17 and ensure java is on PATH." >&2
    exit 1
fi

java_version_line="$(java -version 2>&1 | head -n 1)"
java_major="$(echo "${java_version_line}" | sed -E 's/.*version "([0-9]+).*/\1/')"
if [[ "${java_major}" != "17" ]]; then
    echo "JDK 17 is required to build Harmonixia debug APKs." >&2
    echo "Detected: ${java_version_line}" >&2
    echo "Set JAVA_HOME to a JDK 17 installation and retry." >&2
    exit 1
fi

echo "Building debug APK with Gradle..."
"${repo_root}/gradlew" --no-daemon :app:assembleDebug

if [[ ! -f "${source_apk}" ]]; then
    echo "Debug APK not found at ${source_apk}" >&2
    exit 1
fi

mkdir -p "${output_dir}"
cp "${source_apk}" "${target_apk}"

git_sha="unknown"
if command -v git >/dev/null 2>&1 && git -C "${repo_root}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git_sha="$(git -C "${repo_root}" rev-parse HEAD)"
fi

timestamp="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
cat > "${output_dir}/build-info.txt" <<EOF
git_sha=${git_sha}
generated_at=${timestamp}
source_apk=${source_apk}
EOF

echo "Debug APK written to ${target_apk}"
echo "Build metadata written to ${output_dir}/build-info.txt"
