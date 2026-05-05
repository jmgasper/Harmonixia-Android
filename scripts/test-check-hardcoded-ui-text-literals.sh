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
    Text(text = """$title""")
    Text(
        """$title"""
    )
    Text(
        // localized
        """$title"""
    )
    Text( // localized
        """$title"""
    )
    Text(
        // localized
        """$title""" // localized
    )
    Text(
        text =
            title
    )
    Text(
        text =
            // localized
            title
    )
    Text(
        text =
            """$title"""
    )
    Text(
        text =
            """$title""" // localized
    )
    Text(
        text = /* localized */
            """$title"""
    )
    BasicText(text = title)
    BasicText(text = """$title""")
    BasicText(
        """$title"""
    )
    BasicText(text = AnnotatedString(title))
    BasicText(text = AnnotatedString(text = title))
    BasicText(text = AnnotatedString(text = """$title"""))
    BasicText(text = AnnotatedString(
        """$title"""
    ))
    Text(text = buildAnnotatedString {
        append(title)
        appendLine(title)
        appendRange(title, 0, title.length)
        appendRange("""$title""", 0, title.length)
        append(text = title)
        appendLine(text = title)
        appendLine(text = """$title""")
        appendRange(text = title, startIndex = 0, endIndex = title.length)
        appendRange(text = """$title""", startIndex = 0, endIndex = title.length)
        append(start = 0, end = title.length, text = title)
        append(start = 0, end = title.length, text = """$title""")
        append(end = title.length, text = title, start = 0)
        append(end = title.length, text = """$title""", start = 0)
        appendRange(startIndex = 0, endIndex = title.length, text = title)
        appendRange(endIndex = title.length, text = title, startIndex = 0)
        appendRange(endIndex = title.length, text = """$title""", startIndex = 0)
        appendRange(
            endIndex = title.length,
            text = title,
            startIndex = 0
        )
        append(
            end = title.length,
            text = """$title""",
            start = 0
        )
        appendLine(
            text = title
        )
        appendLine(
            value = """$title"""
        )
        appendLine(
            title
        )
        appendLine(
            """$title"""
        )
        appendLine(value = title)
        appendLine(value = """$title""")
    })
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.action_play))
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            title
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            """$title"""
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            // localized
            title
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized */
            title
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            """$title"""
    )
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

content_description_multiline_fail_dir="${tmp_dir}/content-description-multiline-fail"
mkdir -p "$content_description_multiline_fail_dir"
cat > "${content_description_multiline_fail_dir}/ContentDescriptionMultilineLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionMultiline() {
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            "Play track"
    )
}
KOTLIN

content_description_multiline_fail_output="$(run_expect_exit 1 "$content_description_multiline_fail_dir")"
assert_contains "$content_description_multiline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_multiline_fail_output" "ContentDescriptionMultilineLiteral.kt"
assert_contains "$content_description_multiline_fail_output" "\"Play track\""

content_description_multiline_raw_fail_dir="${tmp_dir}/content-description-multiline-raw-fail"
mkdir -p "$content_description_multiline_raw_fail_dir"
cat > "${content_description_multiline_raw_fail_dir}/ContentDescriptionMultilineRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionMultilineRaw() {
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            """Play track"""
    )
}
KOTLIN

content_description_multiline_raw_fail_output="$(run_expect_exit 1 "$content_description_multiline_raw_fail_dir")"
assert_contains "$content_description_multiline_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_multiline_raw_fail_output" "ContentDescriptionMultilineRawLiteral.kt"
assert_contains "$content_description_multiline_raw_fail_output" "\"\"\"Play track\"\"\""

content_description_raw_fail_dir="${tmp_dir}/content-description-raw-fail"
mkdir -p "$content_description_raw_fail_dir"
cat > "${content_description_raw_fail_dir}/ContentDescriptionRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionRaw() {
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = """Play track""")
}
KOTLIN

content_description_raw_fail_output="$(run_expect_exit 1 "$content_description_raw_fail_dir")"
assert_contains "$content_description_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_raw_fail_output" "ContentDescriptionRawLiteral.kt"
assert_contains "$content_description_raw_fail_output" "contentDescription = \"\"\"Play track\"\"\""

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

semantics_raw_fail_dir="${tmp_dir}/semantics-raw-fail"
mkdir -p "$semantics_raw_fail_dir"
cat > "${semantics_raw_fail_dir}/SemanticsContentDescriptionRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailSemanticsContentDescriptionRaw() {
    Box(modifier = Modifier.semantics { contentDescription = """Volume control""" })
}
KOTLIN

semantics_raw_fail_output="$(run_expect_exit 1 "$semantics_raw_fail_dir")"
assert_contains "$semantics_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$semantics_raw_fail_output" "SemanticsContentDescriptionRawLiteral.kt"
assert_contains "$semantics_raw_fail_output" "contentDescription = \"\"\"Volume control\"\"\""

text_named_arg_raw_fail_dir="${tmp_dir}/text-named-arg-raw-fail"
mkdir -p "$text_named_arg_raw_fail_dir"
cat > "${text_named_arg_raw_fail_dir}/TextNamedArgRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextNamedArgRaw() {
    Text(text = """Now playing""")
}
KOTLIN

text_named_arg_raw_fail_output="$(run_expect_exit 1 "$text_named_arg_raw_fail_dir")"
assert_contains "$text_named_arg_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_named_arg_raw_fail_output" "TextNamedArgRawLiteral.kt"
assert_contains "$text_named_arg_raw_fail_output" "Text(text = \"\"\"Now playing\"\"\")"

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

text_multiline_positional_fail_dir="${tmp_dir}/text-multiline-positional-fail"
mkdir -p "$text_multiline_positional_fail_dir"
cat > "${text_multiline_positional_fail_dir}/TextMultilinePositionalLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositional() {
    Text(
        "Now playing"
    )
}
KOTLIN

text_multiline_positional_fail_output="$(run_expect_exit 1 "$text_multiline_positional_fail_dir")"
assert_contains "$text_multiline_positional_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_fail_output" "TextMultilinePositionalLiteral.kt"
assert_contains "$text_multiline_positional_fail_output" "\"Now playing\""

text_multiline_positional_comment_fail_dir="${tmp_dir}/text-multiline-positional-comment-fail"
mkdir -p "$text_multiline_positional_comment_fail_dir"
cat > "${text_multiline_positional_comment_fail_dir}/TextMultilinePositionalCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalComment() {
    Text(
        // TODO localize
        "Now playing"
    )
}
KOTLIN

text_multiline_positional_comment_fail_output="$(run_expect_exit 1 "$text_multiline_positional_comment_fail_dir")"
assert_contains "$text_multiline_positional_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_comment_fail_output" "TextMultilinePositionalCommentLiteral.kt"
assert_contains "$text_multiline_positional_comment_fail_output" "\"Now playing\""

text_multiline_positional_call_start_inline_comment_fail_dir="${tmp_dir}/text-multiline-positional-call-start-inline-comment-fail"
mkdir -p "$text_multiline_positional_call_start_inline_comment_fail_dir"
cat > "${text_multiline_positional_call_start_inline_comment_fail_dir}/TextMultilinePositionalCallStartInlineCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalCallStartInlineComment() {
    Text( // TODO localize
        "Now playing"
    )
}
KOTLIN

text_multiline_positional_call_start_inline_comment_fail_output="$(run_expect_exit 1 "$text_multiline_positional_call_start_inline_comment_fail_dir")"
assert_contains "$text_multiline_positional_call_start_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_call_start_inline_comment_fail_output" "TextMultilinePositionalCallStartInlineCommentLiteral.kt"
assert_contains "$text_multiline_positional_call_start_inline_comment_fail_output" "\"Now playing\""

text_multiline_positional_inline_comment_fail_dir="${tmp_dir}/text-multiline-positional-inline-comment-fail"
mkdir -p "$text_multiline_positional_inline_comment_fail_dir"
cat > "${text_multiline_positional_inline_comment_fail_dir}/TextMultilinePositionalInlineCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalInlineComment() {
    Text(
        "Now playing" // TODO localize
    )
}
KOTLIN

text_multiline_positional_inline_comment_fail_output="$(run_expect_exit 1 "$text_multiline_positional_inline_comment_fail_dir")"
assert_contains "$text_multiline_positional_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_inline_comment_fail_output" "TextMultilinePositionalInlineCommentLiteral.kt"
assert_contains "$text_multiline_positional_inline_comment_fail_output" "\"Now playing\" // TODO localize"

basic_text_multiline_positional_fail_dir="${tmp_dir}/basic-text-multiline-positional-fail"
mkdir -p "$basic_text_multiline_positional_fail_dir"
cat > "${basic_text_multiline_positional_fail_dir}/BasicTextMultilinePositionalLiteral.kt" <<'KOTLIN'
@Composable
fun FailBasicTextMultilinePositional() {
    BasicText(
        "Now playing"
    )
}
KOTLIN

basic_text_multiline_positional_fail_output="$(run_expect_exit 1 "$basic_text_multiline_positional_fail_dir")"
assert_contains "$basic_text_multiline_positional_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$basic_text_multiline_positional_fail_output" "BasicTextMultilinePositionalLiteral.kt"
assert_contains "$basic_text_multiline_positional_fail_output" "\"Now playing\""

text_multiline_named_assignment_fail_dir="${tmp_dir}/text-multiline-named-assignment-fail"
mkdir -p "$text_multiline_named_assignment_fail_dir"
cat > "${text_multiline_named_assignment_fail_dir}/TextMultilineNamedAssignmentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilineNamedAssignment() {
    Text(
        text =
            "Now playing"
    )
    Text(
        text =
            """Now playing"""
    )
}
KOTLIN

text_multiline_named_assignment_fail_output="$(run_expect_exit 1 "$text_multiline_named_assignment_fail_dir")"
assert_contains "$text_multiline_named_assignment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_named_assignment_fail_output" "TextMultilineNamedAssignmentLiteral.kt"
assert_contains "$text_multiline_named_assignment_fail_output" "\"Now playing\""
assert_contains "$text_multiline_named_assignment_fail_output" "\"\"\"Now playing\"\"\""

text_multiline_named_assignment_comment_fail_dir="${tmp_dir}/text-multiline-named-assignment-comment-fail"
mkdir -p "$text_multiline_named_assignment_comment_fail_dir"
cat > "${text_multiline_named_assignment_comment_fail_dir}/TextMultilineNamedAssignmentCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilineNamedAssignmentComment() {
    Text(
        text =
            // TODO localize
            "Now playing"
    )
}
KOTLIN

text_multiline_named_assignment_comment_fail_output="$(run_expect_exit 1 "$text_multiline_named_assignment_comment_fail_dir")"
assert_contains "$text_multiline_named_assignment_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_named_assignment_comment_fail_output" "TextMultilineNamedAssignmentCommentLiteral.kt"
assert_contains "$text_multiline_named_assignment_comment_fail_output" "\"Now playing\""

text_multiline_named_assignment_start_inline_comment_fail_dir="${tmp_dir}/text-multiline-named-assignment-start-inline-comment-fail"
mkdir -p "$text_multiline_named_assignment_start_inline_comment_fail_dir"
cat > "${text_multiline_named_assignment_start_inline_comment_fail_dir}/TextMultilineNamedAssignmentStartInlineCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilineNamedAssignmentStartInlineComment() {
    Text(
        text = /* TODO localize */
            "Now playing"
    )
}
KOTLIN

text_multiline_named_assignment_start_inline_comment_fail_output="$(run_expect_exit 1 "$text_multiline_named_assignment_start_inline_comment_fail_dir")"
assert_contains "$text_multiline_named_assignment_start_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_named_assignment_start_inline_comment_fail_output" "TextMultilineNamedAssignmentStartInlineCommentLiteral.kt"
assert_contains "$text_multiline_named_assignment_start_inline_comment_fail_output" "\"Now playing\""

text_multiline_named_assignment_inline_comment_fail_dir="${tmp_dir}/text-multiline-named-assignment-inline-comment-fail"
mkdir -p "$text_multiline_named_assignment_inline_comment_fail_dir"
cat > "${text_multiline_named_assignment_inline_comment_fail_dir}/TextMultilineNamedAssignmentInlineCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilineNamedAssignmentInlineComment() {
    Text(
        text =
            "Now playing" // TODO localize
    )
}
KOTLIN

text_multiline_named_assignment_inline_comment_fail_output="$(run_expect_exit 1 "$text_multiline_named_assignment_inline_comment_fail_dir")"
assert_contains "$text_multiline_named_assignment_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_named_assignment_inline_comment_fail_output" "TextMultilineNamedAssignmentInlineCommentLiteral.kt"
assert_contains "$text_multiline_named_assignment_inline_comment_fail_output" "\"Now playing\" // TODO localize"

basic_text_raw_fail_dir="${tmp_dir}/basic-text-raw-fail"
mkdir -p "$basic_text_raw_fail_dir"
cat > "${basic_text_raw_fail_dir}/BasicTextRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailBasicTextRaw() {
    BasicText("""Now playing""")
}
KOTLIN

basic_text_raw_fail_output="$(run_expect_exit 1 "$basic_text_raw_fail_dir")"
assert_contains "$basic_text_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$basic_text_raw_fail_output" "BasicTextRawLiteral.kt"
assert_contains "$basic_text_raw_fail_output" "BasicText(\"\"\"Now playing\"\"\")"

annotated_string_constructor_fail_dir="${tmp_dir}/annotated-string-constructor-fail"
mkdir -p "$annotated_string_constructor_fail_dir"
cat > "${annotated_string_constructor_fail_dir}/AnnotatedStringConstructorLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringConstructor() {
    BasicText(text = AnnotatedString("Now playing"))
}
KOTLIN

annotated_string_constructor_fail_output="$(run_expect_exit 1 "$annotated_string_constructor_fail_dir")"
assert_contains "$annotated_string_constructor_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_constructor_fail_output" "AnnotatedStringConstructorLiteral.kt"
assert_contains "$annotated_string_constructor_fail_output" "AnnotatedString(\"Now playing\")"

annotated_string_constructor_multiline_fail_dir="${tmp_dir}/annotated-string-constructor-multiline-fail"
mkdir -p "$annotated_string_constructor_multiline_fail_dir"
cat > "${annotated_string_constructor_multiline_fail_dir}/AnnotatedStringConstructorMultilineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringConstructorMultiline() {
    BasicText(text = AnnotatedString(
        "Now playing"
    ))
}
KOTLIN

annotated_string_constructor_multiline_fail_output="$(run_expect_exit 1 "$annotated_string_constructor_multiline_fail_dir")"
assert_contains "$annotated_string_constructor_multiline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_constructor_multiline_fail_output" "AnnotatedStringConstructorMultilineLiteral.kt"
assert_contains "$annotated_string_constructor_multiline_fail_output" "\"Now playing\""

annotated_string_constructor_raw_fail_dir="${tmp_dir}/annotated-string-constructor-raw-fail"
mkdir -p "$annotated_string_constructor_raw_fail_dir"
cat > "${annotated_string_constructor_raw_fail_dir}/AnnotatedStringConstructorRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringConstructorRaw() {
    BasicText(text = AnnotatedString("""Now playing"""))
}
KOTLIN

annotated_string_constructor_raw_fail_output="$(run_expect_exit 1 "$annotated_string_constructor_raw_fail_dir")"
assert_contains "$annotated_string_constructor_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_constructor_raw_fail_output" "AnnotatedStringConstructorRawLiteral.kt"
assert_contains "$annotated_string_constructor_raw_fail_output" "AnnotatedString(\"\"\"Now playing\"\"\")"

annotated_string_named_arg_fail_dir="${tmp_dir}/annotated-string-named-arg-fail"
mkdir -p "$annotated_string_named_arg_fail_dir"
cat > "${annotated_string_named_arg_fail_dir}/AnnotatedStringNamedArgLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArg() {
    BasicText(text = AnnotatedString(text = "Now playing"))
}
KOTLIN

annotated_string_named_arg_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_fail_dir")"
assert_contains "$annotated_string_named_arg_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_fail_output" "AnnotatedStringNamedArgLiteral.kt"
assert_contains "$annotated_string_named_arg_fail_output" "AnnotatedString(text = \"Now playing\")"

annotated_string_named_arg_raw_fail_dir="${tmp_dir}/annotated-string-named-arg-raw-fail"
mkdir -p "$annotated_string_named_arg_raw_fail_dir"
cat > "${annotated_string_named_arg_raw_fail_dir}/AnnotatedStringNamedArgRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgRaw() {
    BasicText(text = AnnotatedString(text = """Now playing"""))
}
KOTLIN

annotated_string_named_arg_raw_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_raw_fail_dir")"
assert_contains "$annotated_string_named_arg_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_raw_fail_output" "AnnotatedStringNamedArgRawLiteral.kt"
assert_contains "$annotated_string_named_arg_raw_fail_output" "AnnotatedString(text = \"\"\"Now playing\"\"\")"

annotated_string_append_fail_dir="${tmp_dir}/annotated-string-append-fail"
mkdir -p "$annotated_string_append_fail_dir"
cat > "${annotated_string_append_fail_dir}/AnnotatedStringAppendLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppend() {
    Text(
        text = buildAnnotatedString {
            append("Now playing")
        }
    )
}
KOTLIN

annotated_string_append_fail_output="$(run_expect_exit 1 "$annotated_string_append_fail_dir")"
assert_contains "$annotated_string_append_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_fail_output" "AnnotatedStringAppendLiteral.kt"
assert_contains "$annotated_string_append_fail_output" "append(\"Now playing\")"

annotated_string_named_arg_append_fail_dir="${tmp_dir}/annotated-string-named-arg-append-fail"
mkdir -p "$annotated_string_named_arg_append_fail_dir"
cat > "${annotated_string_named_arg_append_fail_dir}/AnnotatedStringNamedArgAppendLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppend() {
    Text(
        text = buildAnnotatedString {
            append(text = "Now playing")
            append(text = """Now playing""")
        }
    )
}
KOTLIN

annotated_string_named_arg_append_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_fail_dir")"
assert_contains "$annotated_string_named_arg_append_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_fail_output" "AnnotatedStringNamedArgAppendLiteral.kt"
assert_contains "$annotated_string_named_arg_append_fail_output" "append(text = \"Now playing\")"
assert_contains "$annotated_string_named_arg_append_fail_output" "append(text = \"\"\"Now playing\"\"\")"

annotated_string_named_arg_append_reordered_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_fail_dir}/AnnotatedStringNamedArgAppendReorderedLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReordered() {
    Text(
        text = buildAnnotatedString {
            append(start = 0, end = 3, text = "Now playing")
            append(start = 0, end = 3, text = """Now playing""")
            append(end = 3, text = "Now playing", start = 0)
            append(end = 3, text = """Now playing""", start = 0)
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "AnnotatedStringNamedArgAppendReorderedLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "append(start = 0, end = 3, text = \"Now playing\")"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "append(start = 0, end = 3, text = \"\"\"Now playing\"\"\")"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "append(end = 3, text = \"Now playing\", start = 0)"
assert_contains "$annotated_string_named_arg_append_reordered_fail_output" "append(end = 3, text = \"\"\"Now playing\"\"\", start = 0)"

annotated_string_append_line_fail_dir="${tmp_dir}/annotated-string-append-line-fail"
mkdir -p "$annotated_string_append_line_fail_dir"
cat > "${annotated_string_append_line_fail_dir}/AnnotatedStringAppendLineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLine() {
    Text(
        text = buildAnnotatedString {
            appendLine("Now playing")
        }
    )
}
KOTLIN

annotated_string_append_line_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_fail_dir")"
assert_contains "$annotated_string_append_line_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_fail_output" "AnnotatedStringAppendLineLiteral.kt"
assert_contains "$annotated_string_append_line_fail_output" "appendLine(\"Now playing\")"

annotated_string_append_range_fail_dir="${tmp_dir}/annotated-string-append-range-fail"
mkdir -p "$annotated_string_append_range_fail_dir"
cat > "${annotated_string_append_range_fail_dir}/AnnotatedStringAppendRangeLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRange() {
    Text(
        text = buildAnnotatedString {
            appendRange("Now playing", 0, 3)
            appendRange(text = "Now playing", startIndex = 0, endIndex = 3)
        }
    )
}
KOTLIN

annotated_string_append_range_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_fail_dir")"
assert_contains "$annotated_string_append_range_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_fail_output" "AnnotatedStringAppendRangeLiteral.kt"
assert_contains "$annotated_string_append_range_fail_output" "appendRange(\"Now playing\", 0, 3)"
assert_contains "$annotated_string_append_range_fail_output" "appendRange(text = \"Now playing\", startIndex = 0, endIndex = 3)"

annotated_string_append_range_raw_fail_dir="${tmp_dir}/annotated-string-append-range-raw-fail"
mkdir -p "$annotated_string_append_range_raw_fail_dir"
cat > "${annotated_string_append_range_raw_fail_dir}/AnnotatedStringAppendRangeRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeRaw() {
    Text(
        text = buildAnnotatedString {
            appendRange("""Now playing""", 0, 3)
            appendRange(text = """Now playing""", startIndex = 0, endIndex = 3)
        }
    )
}
KOTLIN

annotated_string_append_range_raw_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_raw_fail_dir")"
assert_contains "$annotated_string_append_range_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_raw_fail_output" "AnnotatedStringAppendRangeRawLiteral.kt"
assert_contains "$annotated_string_append_range_raw_fail_output" "appendRange(\"\"\"Now playing\"\"\", 0, 3)"
assert_contains "$annotated_string_append_range_raw_fail_output" "appendRange(text = \"\"\"Now playing\"\"\", startIndex = 0, endIndex = 3)"

annotated_string_append_range_reordered_named_args_fail_dir="${tmp_dir}/annotated-string-append-range-reordered-named-args-fail"
mkdir -p "$annotated_string_append_range_reordered_named_args_fail_dir"
cat > "${annotated_string_append_range_reordered_named_args_fail_dir}/AnnotatedStringAppendRangeReorderedNamedArgsLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeReorderedNamedArgs() {
    Text(
        text = buildAnnotatedString {
            appendRange(startIndex = 0, endIndex = 3, text = "Now playing")
            appendRange(startIndex = 0, endIndex = 3, text = """Now playing""")
            appendRange(endIndex = 3, text = "Now playing", startIndex = 0)
            appendRange(endIndex = 3, text = """Now playing""", startIndex = 0)
        }
    )
}
KOTLIN

annotated_string_append_range_reordered_named_args_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_reordered_named_args_fail_dir")"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "AnnotatedStringAppendRangeReorderedNamedArgsLiteral.kt"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "appendRange(startIndex = 0, endIndex = 3, text = \"Now playing\")"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "appendRange(startIndex = 0, endIndex = 3, text = \"\"\"Now playing\"\"\")"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "appendRange(endIndex = 3, text = \"Now playing\", startIndex = 0)"
assert_contains "$annotated_string_append_range_reordered_named_args_fail_output" "appendRange(endIndex = 3, text = \"\"\"Now playing\"\"\", startIndex = 0)"

annotated_string_append_multiline_reordered_named_args_fail_dir="${tmp_dir}/annotated-string-append-multiline-reordered-named-args-fail"
mkdir -p "$annotated_string_append_multiline_reordered_named_args_fail_dir"
cat > "${annotated_string_append_multiline_reordered_named_args_fail_dir}/AnnotatedStringAppendMultilineReorderedNamedArgsLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendMultilineReorderedNamedArgs() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = "Now playing",
                startIndex = 0
            )
            append(
                end = 3,
                text = """Now playing""",
                start = 0
            )
        }
    )
}
KOTLIN

annotated_string_append_multiline_reordered_named_args_fail_output="$(run_expect_exit 1 "$annotated_string_append_multiline_reordered_named_args_fail_dir")"
assert_contains "$annotated_string_append_multiline_reordered_named_args_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_multiline_reordered_named_args_fail_output" "AnnotatedStringAppendMultilineReorderedNamedArgsLiteral.kt"
assert_contains "$annotated_string_append_multiline_reordered_named_args_fail_output" "text = \"Now playing\""
assert_contains "$annotated_string_append_multiline_reordered_named_args_fail_output" "text = \"\"\"Now playing\"\"\""

annotated_string_append_line_multiline_named_args_fail_dir="${tmp_dir}/annotated-string-append-line-multiline-named-args-fail"
mkdir -p "$annotated_string_append_line_multiline_named_args_fail_dir"
cat > "${annotated_string_append_line_multiline_named_args_fail_dir}/AnnotatedStringAppendLineMultilineNamedArgsLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineMultilineNamedArgs() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                text = "Now playing"
            )
            appendLine(
                value = """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_append_line_multiline_named_args_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_multiline_named_args_fail_dir")"
assert_contains "$annotated_string_append_line_multiline_named_args_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_multiline_named_args_fail_output" "AnnotatedStringAppendLineMultilineNamedArgsLiteral.kt"
assert_contains "$annotated_string_append_line_multiline_named_args_fail_output" "text = \"Now playing\""
assert_contains "$annotated_string_append_line_multiline_named_args_fail_output" "value = \"\"\"Now playing\"\"\""

annotated_string_append_line_raw_fail_dir="${tmp_dir}/annotated-string-append-line-raw-fail"
mkdir -p "$annotated_string_append_line_raw_fail_dir"
cat > "${annotated_string_append_line_raw_fail_dir}/AnnotatedStringAppendLineRawLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineRaw() {
    Text(
        text = buildAnnotatedString {
            appendLine("""Now playing""")
        }
    )
}
KOTLIN

annotated_string_append_line_raw_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_raw_fail_dir")"
assert_contains "$annotated_string_append_line_raw_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_raw_fail_output" "AnnotatedStringAppendLineRawLiteral.kt"
assert_contains "$annotated_string_append_line_raw_fail_output" "appendLine(\"\"\"Now playing\"\"\")"

annotated_string_append_line_multiline_positional_fail_dir="${tmp_dir}/annotated-string-append-line-multiline-positional-fail"
mkdir -p "$annotated_string_append_line_multiline_positional_fail_dir"
cat > "${annotated_string_append_line_multiline_positional_fail_dir}/AnnotatedStringAppendLineMultilinePositionalLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineMultilinePositional() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                "Now playing"
            )
            appendLine(
                """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_append_line_multiline_positional_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_multiline_positional_fail_dir")"
assert_contains "$annotated_string_append_line_multiline_positional_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_multiline_positional_fail_output" "AnnotatedStringAppendLineMultilinePositionalLiteral.kt"
assert_contains "$annotated_string_append_line_multiline_positional_fail_output" "\"Now playing\""
assert_contains "$annotated_string_append_line_multiline_positional_fail_output" "\"\"\"Now playing\"\"\""

annotated_string_named_arg_append_line_fail_dir="${tmp_dir}/annotated-string-named-arg-append-line-fail"
mkdir -p "$annotated_string_named_arg_append_line_fail_dir"
cat > "${annotated_string_named_arg_append_line_fail_dir}/AnnotatedStringNamedArgAppendLineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendLine() {
    Text(
        text = buildAnnotatedString {
            appendLine(text = "Now playing")
            appendLine(text = """Now playing""")
            appendLine(value = "Now playing")
            appendLine(value = """Now playing""")
        }
    )
}
KOTLIN

annotated_string_named_arg_append_line_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_line_fail_dir")"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "AnnotatedStringNamedArgAppendLineLiteral.kt"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "appendLine(text = \"Now playing\")"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "appendLine(text = \"\"\"Now playing\"\"\")"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "appendLine(value = \"Now playing\")"
assert_contains "$annotated_string_named_arg_append_line_fail_output" "appendLine(value = \"\"\"Now playing\"\"\")"

echo "check-hardcoded-ui-text-literals tests passed."
