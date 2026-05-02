#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

run_gradle_checks="false"

usage() {
    cat <<USAGE
Usage: $(basename "$0") [options]

Audit AGP 9 Phase 2 migration guardrails for this repository.

Options:
  --with-gradle-checks   Run Phase 2 validation Gradle gates after static audit
  --help, -h             Show this help

Static checks:
  - Root plugin versions (AGP, legacy-kapt, Hilt)
  - Gradle wrapper version
  - Built-in Kotlin plugin migration state
  - Hilt plugin/runtime/compiler version alignment
  - Removal of temporary AGP 9 opt-out flags
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --with-gradle-checks)
            run_gradle_checks="true"
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

require_file() {
    local file="$1"
    if [[ ! -f "$file" ]]; then
        echo "Missing required file: $file" >&2
        exit 1
    fi
}

select_java_home() {
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
        return 0
    fi

    local candidates=(
        "${HARMONIXIA_JAVA_HOME:-}"
        "${JAVA17_HOME:-}"
        "${JDK17_HOME:-}"
        "${JAVA_HOME_17_X64:-}"
        "$HOME/.local/jdks/temurin-17"
        "$HOME/.jdks/jdk-17.0.17+10"
        "$HOME/.jdks/jdk-17.0.1"
        "/tmp/jdk17/jdk-17"
    )

    local candidate
    for candidate in "${candidates[@]}"; do
        if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
            export JAVA_HOME="$candidate"
            return 0
        fi
    done

    return 1
}

extract_version() {
    local pattern="$1"
    local file="$2"
    local value
    value="$(rg -o "$pattern" "$file" | head -n 1 | sed -E 's/.*version "([^"]+)".*/\1/')"
    printf '%s\n' "$value"
}

extract_dependency_version() {
    local pattern="$1"
    local file="$2"
    local value
    value="$(rg -o "$pattern" "$file" | head -n 1 | sed -E 's/.*:([^:]+)".*/\1/')"
    printf '%s\n' "$value"
}

extract_sdk_level() {
    local key="$1"
    local file="$2"
    local value
    value="$(rg -o "${key}\s*=\s*[0-9]+" "$file" | head -n 1 | sed -E 's/.*=\s*([0-9]+)/\1/')"
    printf '%s\n' "$value"
}

pass_count=0
warn_count=0
fail_count=0

pass() {
    pass_count=$((pass_count + 1))
    printf 'PASS: %s\n' "$1"
}

warn() {
    warn_count=$((warn_count + 1))
    printf 'WARN: %s\n' "$1"
}

fail() {
    fail_count=$((fail_count + 1))
    printf 'FAIL: %s\n' "$1"
}

check_equal() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [[ "$actual" == "$expected" ]]; then
        pass "$label is $actual"
    else
        fail "$label expected $expected but found ${actual:-<empty>}"
    fi
}

check_present() {
    local label="$1"
    local pattern="$2"
    local file="$3"
    if rg -q "$pattern" "$file"; then
        pass "$label present"
    else
        fail "$label missing"
    fi
}

check_absent() {
    local label="$1"
    local pattern="$2"
    local file="$3"
    if rg -q "$pattern" "$file"; then
        fail "$label should be absent"
    else
        pass "$label absent"
    fi
}

build_file="${repo_root}/build.gradle.kts"
app_build_file="${repo_root}/app/build.gradle.kts"
wrapper_file="${repo_root}/gradle/wrapper/gradle-wrapper.properties"
gradle_props_file="${repo_root}/gradle.properties"

require_file "$build_file"
require_file "$app_build_file"
require_file "$wrapper_file"
require_file "$gradle_props_file"

root_agp_version="$(extract_version 'id\("com.android.application"\) version "[^"]+"' "$build_file")"
root_legacy_kapt_version="$(extract_version 'id\("com.android.legacy-kapt"\) version "[^"]+"' "$build_file")"
root_hilt_plugin_version="$(extract_version 'id\("com.google.dagger.hilt.android"\) version "[^"]+"' "$build_file")"
wrapper_gradle_version="$(rg -o 'gradle-[0-9]+\.[0-9]+\.[0-9]+' "$wrapper_file" | head -n 1 | sed -E 's/gradle-//')"
hilt_runtime_version="$(extract_dependency_version 'implementation\("com.google.dagger:hilt-android:[^"]+"' "$app_build_file")"
hilt_compiler_version="$(extract_dependency_version 'kapt\("com.google.dagger:hilt-compiler:[^"]+"' "$app_build_file")"
compile_sdk="$(extract_sdk_level 'compileSdk' "$app_build_file")"
target_sdk="$(extract_sdk_level 'targetSdk' "$app_build_file")"

printf 'AGP 9 Phase 2 Audit\n'
printf 'Repository: %s\n' "$repo_root"
printf 'Detected versions: AGP=%s, legacy-kapt=%s, Hilt plugin=%s, Hilt runtime=%s, Hilt compiler=%s, Gradle=%s\n' \
    "$root_agp_version" "$root_legacy_kapt_version" "$root_hilt_plugin_version" "$hilt_runtime_version" "$hilt_compiler_version" "$wrapper_gradle_version"

check_equal "Root AGP plugin version" "9.1.1" "$root_agp_version"
check_equal "Root legacy-kapt plugin version" "$root_agp_version" "$root_legacy_kapt_version"
check_equal "Gradle wrapper version" "9.3.1" "$wrapper_gradle_version"
check_equal "Hilt plugin/runtime version alignment" "$root_hilt_plugin_version" "$hilt_runtime_version"
check_equal "Hilt plugin/compiler version alignment" "$root_hilt_plugin_version" "$hilt_compiler_version"
check_equal "compileSdk" "36" "$compile_sdk"
check_equal "targetSdk" "36" "$target_sdk"

check_absent "Temporary AGP opt-out flag android.builtInKotlin=false" '^android\.builtInKotlin=false$' "$gradle_props_file"
check_absent "Temporary AGP opt-out flag android.newDsl=false" '^android\.newDsl=false$' "$gradle_props_file"

check_absent "Legacy org.jetbrains.kotlin.android plugin in app module" 'id\("org\.jetbrains\.kotlin\.android"\)' "$app_build_file"
check_absent "Legacy org.jetbrains.kotlin.kapt plugin in app module" 'id\("org\.jetbrains\.kotlin\.kapt"\)' "$app_build_file"
check_present "AGP legacy-kapt plugin in app module" 'id\("com\.android\.legacy-kapt"\)' "$app_build_file"

if [[ "$root_agp_version" != "9.1.1" ]]; then
    warn "AGP version changed from documented baseline; update this script expectations when intentionally migrating."
fi
if [[ "$wrapper_gradle_version" != "9.3.1" ]]; then
    warn "Gradle wrapper changed from documented baseline; confirm AGP compatibility matrix before proceeding."
fi

if [[ "$run_gradle_checks" == "true" ]]; then
    if ! select_java_home; then
        fail "Unable to locate a JDK runtime for Gradle checks. Set JAVA_HOME (JDK 17)."
    else
        pass "Using JAVA_HOME=$JAVA_HOME for Gradle validation gates"
    fi

    echo "Running Phase 2 Gradle validation gates..."
    if [[ "$fail_count" -eq 0 ]]; then
        (
            cd "$repo_root"
            ./gradlew :app:kaptDebugUnitTestKotlin --warning-mode=all --rerun-tasks
            ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --warning-mode=all
        )
        pass "Gradle validation gates completed"
    fi
fi

echo ""
printf 'Summary: PASS=%d WARN=%d FAIL=%d\n' "$pass_count" "$warn_count" "$fail_count"

if [[ "$fail_count" -gt 0 ]]; then
    exit 1
fi
