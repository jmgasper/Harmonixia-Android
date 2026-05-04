#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
scanner_script="${script_dir}/check-hardcoded-ui-text-literals.sh"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

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
    output="$("${scanner_script}" "$@" 2>&1)"
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

pass_dir="${tmp_dir}/pass"
mkdir -p "$pass_dir"
cat > "${pass_dir}/Pass.kt" <<'KOTLIN'
@Composable
fun Pass(title: String) {
    Text(text = title)
    BasicText(text = title)
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.action_play))
    Box(modifier = Modifier.semantics { contentDescription = title })
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "$title")
}
KOTLIN

pass_output="$(run_expect_exit 0 "$pass_dir")"
assert_contains "$pass_output" "PASS: no hardcoded UI text literals found in ${pass_dir}."

content_description_fail_dir="${tmp_dir}/content-description-fail"
mkdir -p "$content_description_fail_dir"
cat > "${content_description_fail_dir}/ContentDescriptionLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescription() {
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play track")
}
KOTLIN

content_description_fail_output="$(run_expect_exit 1 "$content_description_fail_dir")"
assert_contains "$content_description_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_fail_output" "ContentDescriptionLiteral.kt"
assert_contains "$content_description_fail_output" "contentDescription = \"Play track\""

semantics_fail_dir="${tmp_dir}/semantics-fail"
mkdir -p "$semantics_fail_dir"
cat > "${semantics_fail_dir}/SemanticsContentDescriptionLiteral.kt" <<'KOTLIN'
@Composable
fun FailSemanticsContentDescription() {
    Box(modifier = Modifier.semantics { contentDescription = "Volume control" })
}
KOTLIN

semantics_fail_output="$(run_expect_exit 1 "$semantics_fail_dir")"
assert_contains "$semantics_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$semantics_fail_output" "SemanticsContentDescriptionLiteral.kt"
assert_contains "$semantics_fail_output" "contentDescription = \"Volume control\""

basic_text_fail_dir="${tmp_dir}/basic-text-fail"
mkdir -p "$basic_text_fail_dir"
cat > "${basic_text_fail_dir}/BasicTextLiteral.kt" <<'KOTLIN'
@Composable
fun FailBasicText() {
    BasicText("Now playing")
}
KOTLIN

basic_text_fail_output="$(run_expect_exit 1 "$basic_text_fail_dir")"
assert_contains "$basic_text_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$basic_text_fail_output" "BasicTextLiteral.kt"
assert_contains "$basic_text_fail_output" "BasicText(\"Now playing\")"

echo "check-hardcoded-ui-text-literals tests passed."
