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

assert_count() {
    local text="$1"
    local needle="$2"
    local expected_count="$3"
    local actual_count
    actual_count="$(printf '%s\n' "$text" | grep -F -c "$needle" || true)"
    if [[ "$actual_count" -ne "$expected_count" ]]; then
        echo "$text" >&2
        fail "expected '$needle' count ${expected_count}, got ${actual_count}"
    fi
}

pass_dir="${tmp_dir}/pass"
mkdir -p "$pass_dir"
cat > "${pass_dir}/Pass.kt" <<'KOTLIN'
@Composable
fun Pass(title: String) {
    Text(text = title)
    Text(text = /* localized */ "$title")
    Text(text = "Now ${title}")
    Text(text = "Now ${title.uppercase()}")
    Text(text = "Now ${title}" /* localized */)
    Text(text = """$title""")
    Text(text = """Now ${title}""")
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
    Text( /* localized
        """$title"""
    )
    Text( /* localized
        */ """$title"""
    )
    Text( /* localized
        */ "Now \"$title\""
    )
    Text(
        /* localized */ """$title"""
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
    Text(
        text = /* localized
            """$title"""
    )
    Text(
        text = /* localized
            */ """$title"""
    )
    Text(
        text = /* localized
            */ "Now ${title}"
    )
    Text(
        text =
            /* localized */ """$title"""
    )
    BasicText(text = title)
    BasicText(text = "Track: ${title}")
    BasicText(text = """$title""")
    BasicText(text = """Track: ${title}""")
    BasicText(
        """$title"""
    )
    BasicText( /* localized
        */ """$title"""
    )
    BasicText( /* localized
        */ "Now \"$title\""
    )
    BasicText(text = AnnotatedString(title))
    BasicText(text = AnnotatedString(text = title))
    BasicText(text = AnnotatedString(text = /* localized */ "$title"))
    BasicText(text = AnnotatedString(text = "Now ${title}"))
    BasicText(text = AnnotatedString(text = "Now ${title}" /* localized */))
    BasicText(text = AnnotatedString(text = "Now ${title} costs \$5"))
    BasicText(text = AnnotatedString(text = "Now ${title} costs \$5" /* localized */))
    BasicText(text = AnnotatedString(text = "Now ${title} costs \$5" // localized
    ))
    BasicText(text = AnnotatedString(text = /* localized
            */ "Now ${title} costs \$5"))
    BasicText(text = AnnotatedString(text = /* localized
                    */ "Now ${title} costs \$5"))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5"""))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" /* localized */))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString(text = /* localized
        */ """Now ${title} costs \$5"""))
    BasicText(text = AnnotatedString(text = /* localized
                    */ """Now ${title} costs \$5"""))
    BasicText(text = AnnotatedString(text = """$title"""))
    BasicText(text = AnnotatedString(text = /* localized
        */ """$title"""))
    BasicText(text = AnnotatedString(
        """$title"""
    ))
    BasicText(text = AnnotatedString( /* localized
        */ """$title"""
    ))
    BasicText(text = AnnotatedString("Now ${title} costs \$5" /* localized */))
    BasicText(text = AnnotatedString("Now ${title} costs \$5" // localized
    ))
    BasicText(text = AnnotatedString("""Now ${title} costs \$5"""))
    BasicText(text = AnnotatedString("""Now ${title} costs \$5""" /* localized */))
    BasicText(text = AnnotatedString("""Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" /* localized */))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" /* localized */
    ))
    BasicText(text = AnnotatedString("""Now ${title} costs \$5""" /* localized */
    ))
    BasicText(text = AnnotatedString(
        """Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString("""Now ${title} costs \$5""" /* localized
            */
    ))
    BasicText(text = AnnotatedString(
        """Now ${title} costs \$5""" /* localized
            */
    ))
    BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" /* localized
            */
    ))
    BasicText(text = AnnotatedString(
        text = """Now ${title} costs \$5""" /* localized
            */
    ))
    BasicText(text = AnnotatedString(
        text = """Now ${title} costs \$5""" /* localized */
    ))
    BasicText(text = AnnotatedString(
        text = """Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString(
        text =
            """Now ${title} costs \$5""" // localized
    ))
    BasicText(text = AnnotatedString(
        text =
            """Now ${title} costs \$5""" /* localized */
    ))
    BasicText(text = AnnotatedString(
        text =
            """Now ${title} costs \$5""" /* localized
                */
    ))
    BasicText(text = AnnotatedString( /* localized
        */ "Now ${title} costs \$5"
    ))
    BasicText(text = AnnotatedString( /* localized
            */ "Now ${title} costs \$5"
    ))
    BasicText(text = AnnotatedString( /* localized
        */ """Now ${title} costs \$5"""
    ))
    BasicText(text = AnnotatedString( /* localized
            */ """Now ${title} costs \$5"""
    ))
    Text(text = buildAnnotatedString {
        append(title)
        appendLine(title)
        appendRange(title, 0, title.length)
        appendRange("""$title""", 0, title.length)
        appendRange("Now \"$title\"" /* localized */, 0, title.length)
        append("Now ${title} costs \$5")
        append("Now ${title} costs \$5" /* localized */)
        append("""Now ${title} costs \$5""")
        append("""Now ${title} costs \$5""" /* localized */)
        append(text = title)
        appendLine(text = title)
        appendLine(text = "Now ${title}")
        appendLine(text = """$title""")
        appendLine(text = """Now ${title}""")
        appendLine(text = "Now ${title}" /* localized */)
        appendLine(text = "Now \"$title\"" /* localized */)
        appendRange(text = title, startIndex = 0, endIndex = title.length)
        appendRange(text = """$title""", startIndex = 0, endIndex = title.length)
        appendRange(text = """Now $title""" /* localized */, startIndex = 0, endIndex = title.length)
        appendRange(text = "Now ${title} costs \$5", startIndex = 0, endIndex = title.length)
        appendRange(text = "Now ${title} costs \$5" /* localized */, startIndex = 0, endIndex = title.length)
        appendRange(text = """Now ${title} costs \$5""", startIndex = 0, endIndex = title.length)
        appendRange(text = """Now ${title} costs \$5""" /* localized */, startIndex = 0, endIndex = title.length)
        append(start = 0, end = title.length, text = title)
        append(start = 0, end = title.length, text = """$title""")
        append(end = title.length, text = title, start = 0)
        append(end = title.length, text = """$title""", start = 0)
        append(end = title.length, text = "Now \"$title\"" /* localized */, start = 0)
        append(end = title.length, text = "Now ${title} costs \$5", start = 0)
        append(end = title.length, text = "Now ${title} costs \$5" /* localized */, start = 0)
        append(end = title.length, text = """Now ${title} costs \$5""", start = 0)
        append(end = title.length, text = """Now ${title} costs \$5""" /* localized */, start = 0)
        append(
            end = title.length,
            text = """Now ${title} costs \$5""", // localized
            start = 0
        )
        appendRange(startIndex = 0, endIndex = title.length, text = title)
        appendRange(endIndex = title.length, text = title, startIndex = 0)
        appendRange(endIndex = title.length, text = """$title""", startIndex = 0)
        appendRange(endIndex = title.length, text = "Now \"$title\"" /* localized */, startIndex = 0)
        appendRange(endIndex = title.length, text = "Now ${title}" /* localized */, startIndex = 0)
        appendRange(endIndex = title.length, text = """Now $title""" /* localized */, startIndex = 0)
        appendRange(endIndex = title.length, text = "Now ${title} costs \$5", startIndex = 0)
        appendRange(endIndex = title.length, text = "Now ${title} costs \$5" /* localized */, startIndex = 0)
        appendRange(endIndex = title.length, text = """Now ${title} costs \$5""", startIndex = 0)
        appendRange(endIndex = title.length, text = """Now ${title} costs \$5""" /* localized */, startIndex = 0)
        appendRange(
            endIndex = title.length,
            text = """Now ${title} costs \$5""", // localized
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ title,
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ """$title""",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ "Now \"$title\"",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ "Now ${title}",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ "Now ${title} costs \$5",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                    */ "Now ${title} costs \$5",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                */ """Now ${title} costs \$5""",
            startIndex = 0
        )
        appendRange(
            endIndex = title.length,
            text = /* localized
                    */ """Now ${title} costs \$5""",
            startIndex = 0
        )
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
        append(
            end = title.length,
            text = /* localized
                */ title,
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                */ """$title""",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                */ "Now \"$title\"",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                */ "Now ${title}",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                */ "Now ${title} costs \$5",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                    */ "Now ${title} costs \$5",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                */ """Now ${title} costs \$5""",
            start = 0
        )
        append(
            end = title.length,
            text = /* localized
                    */ """Now ${title} costs \$5""",
            start = 0
        )
        appendLine(
            text = title
        )
        appendLine(
            text = /* localized
                */ title
        )
        appendLine(
            text = /* localized
                */ """$title"""
        )
        appendLine(
            text = /* localized
                */ "Now \"$title\""
        )
        appendLine(
            text = /* localized
                */ "Now ${title}"
        )
        appendLine(
            text = """Now ${title} costs \$5"""
        )
        appendLine(
            text = """Now ${title} costs \$5""" /* localized */
        )
        appendLine(
            text = """Now ${title} costs \$5""" // localized
        )
        appendLine(
            text = "Now ${title} costs \$5" // localized
        )
        appendLine(
            text = "Now ${title} costs \$5" /* localized */
        )
        appendLine(
            text = /* localized
                */ """Now ${title} costs \$5"""
        )
        appendLine(
            text = /* localized
                    */ """Now ${title} costs \$5"""
        )
        appendLine(
            text = /* localized
                */ "Now ${title} costs \$5"
        )
        appendLine(
            text = /* localized
                    */ "Now ${title} costs \$5"
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
        appendLine(
            """Now ${title} costs \$5"""
        )
        appendLine(value = title)
        appendLine(value = """$title""")
        appendLine(value = "Now ${title}")
        appendLine(value = "Now ${title}" /* localized */)
        appendLine(value = "Now ${title} costs \$5")
        appendLine(value = "Now ${title} costs \$5" /* localized */)
        appendLine(value = """Now ${title} costs \$5""")
        appendLine(value = """Now ${title} costs \$5""" /* localized */)
        appendLine(
            value = "Now ${title} costs \$5" // localized
        )
        appendLine(
            value = """Now ${title} costs \$5""" // localized
        )
        appendLine(
            value = /* localized
                */ "Now ${title}"
        )
        appendLine(
            value = /* localized
                */ "Now ${title} costs \$5"
        )
        appendLine(
            value = /* localized
                    */ "Now ${title} costs \$5"
        )
        appendLine(
            value = /* localized
                */ """Now ${title} costs \$5"""
        )
        appendLine(
            value = /* localized
                    */ """Now ${title} costs \$5"""
        )
        appendLine(
            value = """Now ${title}""" /* localized */
        )
    })
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = stringResource(R.string.action_play))
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play ${title}")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play ${title}" /* localized */)
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = """Play ${title} for \$5""")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = """Play ${title} for \$5""" /* localized */)
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
        contentDescription = /* localized
            title
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
            */ """$title"""
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
            */ "Now \"$title\""
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
            */ "Play ${title}"
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
                    */ "Play ${title} for \$5"
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
            */ """Play ${title} for \$5"""
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* localized
                    */ """Play ${title} for \$5"""
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            /* localized */ title
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            """$title"""
    )
    Box(modifier = Modifier.semantics { contentDescription = title })
    Box(modifier = Modifier.semantics { contentDescription = /* localized */ title })
    Box(modifier = Modifier.semantics { contentDescription = "Volume ${title}" })
    Box(modifier = Modifier.semantics { contentDescription = "Volume ${title}" /* localized */ })
    Box(modifier = Modifier.semantics { contentDescription = "Volume ${title} costs \$5" })
    Box(modifier = Modifier.semantics { contentDescription = /* localized */ "Volume ${title} costs \$5" })
    Box(modifier = Modifier.semantics { contentDescription = "Volume ${title} costs \$5" // localized
    })
    Box(modifier = Modifier.semantics { contentDescription = "Volume ${title} costs \$5" /* localized */ })
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* localized
                */ "Volume ${title} costs \$5"
        }
    )
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* localized
                    */ "Volume ${title} costs \$5"
        }
    )
    Box(modifier = Modifier.semantics { contentDescription = """Volume ${title} costs \$5""" })
    Box(modifier = Modifier.semantics { contentDescription = """Volume ${title} costs \$5""" // localized
    })
    Box(modifier = Modifier.semantics { contentDescription = """Volume ${title} costs \$5""" /* localized */ })
    Box(modifier = Modifier.semantics { contentDescription = /* localized */ """Volume ${title} costs \$5""" })
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* localized
                */ """Volume ${title} costs \$5"""
        }
    )
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* localized
                    */ """Volume ${title} costs \$5"""
        }
    )
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "$title")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = /* localized */ "$title")
}
KOTLIN

pass_output="$(run_expect_exit 0 "$pass_dir")"
assert_contains "$pass_output" "PASS: no hardcoded UI text literals found in ${pass_dir}."

interpolated_escaped_quote_pass_dir="${tmp_dir}/interpolated-escaped-quote-pass"
mkdir -p "$interpolated_escaped_quote_pass_dir"
cat > "${interpolated_escaped_quote_pass_dir}/InterpolatedEscapedQuotePass.kt" <<'KOTLIN'
@Composable
fun PassInterpolatedEscapedQuote(title: String) {
    Text("He said \"$title\"")
    Text(text = "Now \"$title\"")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play \"$title\"")
}
KOTLIN

interpolated_escaped_quote_pass_output="$(run_expect_exit 0 "$interpolated_escaped_quote_pass_dir")"
assert_contains "$interpolated_escaped_quote_pass_output" "PASS: no hardcoded UI text literals found in ${interpolated_escaped_quote_pass_dir}."

escaped_quote_literal_fail_dir="${tmp_dir}/escaped-quote-literal-fail"
mkdir -p "$escaped_quote_literal_fail_dir"
cat > "${escaped_quote_literal_fail_dir}/EscapedQuoteLiteralFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedQuoteLiteral() {
    Text("He said \"Now playing\"")
}
KOTLIN

escaped_quote_literal_fail_output="$(run_expect_exit 1 "$escaped_quote_literal_fail_dir")"
assert_contains "$escaped_quote_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_quote_literal_fail_output" "EscapedQuoteLiteralFail.kt"
assert_contains "$escaped_quote_literal_fail_output" "Text(\"He said \\\"Now playing\\\"\")"

escaped_dollar_interpolation_pass_dir="${tmp_dir}/escaped-dollar-interpolation-pass"
mkdir -p "$escaped_dollar_interpolation_pass_dir"
cat > "${escaped_dollar_interpolation_pass_dir}/EscapedDollarInterpolationPass.kt" <<'KOTLIN'
@Composable
fun PassEscapedDollarInterpolation(title: String) {
    Text(text = "Now $title costs \$5")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play $title for \$5")
    Text(
        text = buildAnnotatedString {
            appendLine(text = "Now $title costs \$5")
        }
    )
}
KOTLIN

escaped_dollar_interpolation_pass_output="$(run_expect_exit 0 "$escaped_dollar_interpolation_pass_dir")"
assert_contains "$escaped_dollar_interpolation_pass_output" "PASS: no hardcoded UI text literals found in ${escaped_dollar_interpolation_pass_dir}."

escaped_dollar_literal_fail_dir="${tmp_dir}/escaped-dollar-literal-fail"
mkdir -p "$escaped_dollar_literal_fail_dir"
cat > "${escaped_dollar_literal_fail_dir}/EscapedDollarLiteralFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarLiteral() {
    Text(text = "Price \$5")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = "Play for \$5")
}
KOTLIN

escaped_dollar_literal_fail_output="$(run_expect_exit 1 "$escaped_dollar_literal_fail_dir")"
assert_contains "$escaped_dollar_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_literal_fail_output" "EscapedDollarLiteralFail.kt"
assert_contains "$escaped_dollar_literal_fail_output" "Text(text = \"Price \\\$5\")"
assert_contains "$escaped_dollar_literal_fail_output" "contentDescription = \"Play for \\\$5\""

escaped_dollar_semantics_fail_dir="${tmp_dir}/escaped-dollar-semantics-fail"
mkdir -p "$escaped_dollar_semantics_fail_dir"
cat > "${escaped_dollar_semantics_fail_dir}/EscapedDollarSemanticsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarSemantics() {
    Box(modifier = Modifier.semantics { contentDescription = "Volume \$5" })
    Box(modifier = Modifier.semantics { contentDescription = "Volume \$5" /* TODO localize */ })
    Box(modifier = Modifier.semantics { contentDescription = """Volume \$5""" })
    Box(modifier = Modifier.semantics { contentDescription = """Volume \$5""" /* TODO localize */ })
    Box(modifier = Modifier.semantics { contentDescription = /* TODO localize */ """Volume \$5""" })
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* TODO localize
                */ """Volume \$5"""
        }
    )
}
KOTLIN

escaped_dollar_semantics_fail_output="$(run_expect_exit 1 "$escaped_dollar_semantics_fail_dir")"
assert_contains "$escaped_dollar_semantics_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_semantics_fail_output" "EscapedDollarSemanticsFail.kt"
assert_contains "$escaped_dollar_semantics_fail_output" "contentDescription = \"Volume \\\$5\""
assert_contains "$escaped_dollar_semantics_fail_output" "contentDescription = \"Volume \\\$5\" /* TODO localize */"
assert_contains "$escaped_dollar_semantics_fail_output" "contentDescription = \"\"\"Volume \\\$5\"\"\""
assert_contains "$escaped_dollar_semantics_fail_output" "contentDescription = \"\"\"Volume \\\$5\"\"\" /* TODO localize */"
assert_contains "$escaped_dollar_semantics_fail_output" "contentDescription = /* TODO localize */ \"\"\"Volume \\\$5\"\"\""
assert_contains "$escaped_dollar_semantics_fail_output" "*/ \"\"\"Volume \\\$5\"\"\""

escaped_dollar_annotated_append_fail_dir="${tmp_dir}/escaped-dollar-annotated-append-fail"
mkdir -p "$escaped_dollar_annotated_append_fail_dir"
cat > "${escaped_dollar_annotated_append_fail_dir}/EscapedDollarAnnotatedAppendFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarAnnotatedAppend() {
    Text(
        text = buildAnnotatedString {
            appendLine(text = "Price \$5")
        }
    )
}
KOTLIN

escaped_dollar_annotated_append_fail_output="$(run_expect_exit 1 "$escaped_dollar_annotated_append_fail_dir")"
assert_contains "$escaped_dollar_annotated_append_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_annotated_append_fail_output" "EscapedDollarAnnotatedAppendFail.kt"
assert_contains "$escaped_dollar_annotated_append_fail_output" "appendLine(text = \"Price \\\$5\")"

escaped_dollar_named_arg_paths_fail_dir="${tmp_dir}/escaped-dollar-named-arg-paths-fail"
mkdir -p "$escaped_dollar_named_arg_paths_fail_dir"
cat > "${escaped_dollar_named_arg_paths_fail_dir}/EscapedDollarNamedArgPathsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarNamedArgPaths() {
    BasicText(text = AnnotatedString(text = "Price \$5"))
    Text(
        text = buildAnnotatedString {
            appendLine(text = "Price \$5")
            appendLine(value = "Price \$5")
            appendLine(value = /* TODO localize */ "Price \$5")
            appendLine(
                value = /* TODO localize
                    */ "Price \$5"
            )
            appendRange(text = "Price \$5", startIndex = 0, endIndex = 3)
            appendRange(endIndex = 3, text = "Price \$5", startIndex = 0)
            appendRange(
                text = /* TODO localize */ "Price \$5",
                startIndex = 0,
                endIndex = 3
            )
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ "Price \$5",
                startIndex = 0
            )
            append(start = 0, end = 3, text = "Price \$5")
            append(end = 3, text = "Price \$5", start = 0)
            append(
                start = 0,
                end = 3,
                text = /* TODO localize */ "Price \$5"
            )
            append(
                end = 3,
                text = /* TODO localize
                    */ "Price \$5",
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_named_arg_paths_fail_output="$(run_expect_exit 1 "$escaped_dollar_named_arg_paths_fail_dir")"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "EscapedDollarNamedArgPathsFail.kt"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "AnnotatedString(text = \"Price \\\$5\")"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "appendLine(text = \"Price \\\$5\")"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "appendLine(value = \"Price \\\$5\")"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "value = /* TODO localize */ \"Price \\\$5\""
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "*/ \"Price \\\$5\""
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "appendRange(text = \"Price \\\$5\", startIndex = 0, endIndex = 3)"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "appendRange(endIndex = 3, text = \"Price \\\$5\", startIndex = 0)"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "text = /* TODO localize */ \"Price \\\$5\""
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "append(start = 0, end = 3, text = \"Price \\\$5\")"
assert_contains "$escaped_dollar_named_arg_paths_fail_output" "append(end = 3, text = \"Price \\\$5\", start = 0)"

escaped_dollar_raw_named_arg_paths_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-paths-fail"
mkdir -p "$escaped_dollar_raw_named_arg_paths_fail_dir"
cat > "${escaped_dollar_raw_named_arg_paths_fail_dir}/EscapedDollarRawNamedArgPathsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgPaths() {
    BasicText(text = AnnotatedString("""Price \$5"""))
    BasicText(text = AnnotatedString(text = """Price \$5"""))
    BasicText(text = AnnotatedString(text = /* TODO localize */ """Price \$5"""))
    BasicText(
        text = AnnotatedString(
            text = /* TODO localize
                */ """Price \$5"""
        )
    )
    Text(
        text = buildAnnotatedString {
            appendLine(text = """Price \$5""")
            appendLine(value = """Price \$5""")
            appendLine(value = /* TODO localize */ """Price \$5""")
            appendRange(text = """Price \$5""", startIndex = 0, endIndex = 3)
            appendRange(endIndex = 3, text = """Price \$5""", startIndex = 0)
            appendRange(text = /* TODO localize */ """Price \$5""", startIndex = 0, endIndex = 3)
            append(start = 0, end = 3, text = """Price \$5""")
            append(end = 3, text = """Price \$5""", start = 0)
            append(start = 0, end = 3, text = /* TODO localize */ """Price \$5""")
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_paths_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_paths_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "EscapedDollarRawNamedArgPathsFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "AnnotatedString(\"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "AnnotatedString(text = \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\""
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "*/ \"\"\"Price \\\$5\"\"\""
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "appendLine(text = \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "appendLine(value = \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "value = /* TODO localize */ \"\"\"Price \\\$5\"\"\""
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "appendRange(text = \"\"\"Price \\\$5\"\"\", startIndex = 0, endIndex = 3)"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "appendRange(endIndex = 3, text = \"\"\"Price \\\$5\"\"\", startIndex = 0)"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "append(start = 0, end = 3, text = \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_paths_fail_output" "append(end = 3, text = \"\"\"Price \\\$5\"\"\", start = 0)"

escaped_dollar_raw_constructor_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_comment_fail_dir}/EscapedDollarRawConstructorCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorComment() {
    BasicText(text = AnnotatedString("""Price \$5""" /* TODO localize */))
    BasicText(text = AnnotatedString(
        /* TODO localize
            */ """Price \$5"""
    ))
}
KOTLIN

escaped_dollar_raw_constructor_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_comment_fail_output" "EscapedDollarRawConstructorCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_comment_fail_output" "AnnotatedString(\"\"\"Price \\\$5\"\"\" /* TODO localize */)"
assert_contains "$escaped_dollar_raw_constructor_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\""

escaped_dollar_raw_constructor_positional_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-positional-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_positional_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_positional_inline_block_comment_fail_dir}/EscapedDollarRawConstructorPositionalInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorPositionalInlineBlockComment() {
    BasicText(text = AnnotatedString(/* TODO localize */ """Price \$5"""))
}
KOTLIN

escaped_dollar_raw_constructor_positional_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_positional_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_positional_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_positional_inline_block_comment_fail_output" "EscapedDollarRawConstructorPositionalInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_positional_inline_block_comment_fail_output" "AnnotatedString(/* TODO localize */ \"\"\"Price \\\$5\"\"\")"

escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-positional-multiline-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_dir}/EscapedDollarRawConstructorPositionalMultilineInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorPositionalMultilineInlineBlockComment() {
    BasicText(text = AnnotatedString(
        /* TODO localize */ """Price \$5"""
    ))
}
KOTLIN

escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_output" "EscapedDollarRawConstructorPositionalMultilineInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_output" "/* TODO localize */ \"\"\"Price \\\$5\"\"\""

escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-positional-multiline-trailing-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_dir}/EscapedDollarRawConstructorPositionalMultilineTrailingBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorPositionalMultilineTrailingBlockComment() {
    BasicText(text = AnnotatedString(
        """Price \$5""" /* TODO localize
            */
    ))
}
KOTLIN

escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_output" "EscapedDollarRawConstructorPositionalMultilineTrailingBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_output" "\"\"\"Price \\\$5\"\"\" /* TODO localize"

escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-positional-multiline-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_dir}/EscapedDollarRawConstructorPositionalMultilineTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorPositionalMultilineTrailingInlineComment() {
    BasicText(text = AnnotatedString(
        """Price \$5""" /* TODO localize */
    ))
}
KOTLIN

escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_output" "EscapedDollarRawConstructorPositionalMultilineTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_inline_comment_fail_output" "\"\"\"Price \\\$5\"\"\" /* TODO localize */"

escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-positional-multiline-trailing-line-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_dir}/EscapedDollarRawConstructorPositionalMultilineTrailingLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorPositionalMultilineTrailingLineComment() {
    BasicText(text = AnnotatedString(
        """Price \$5""" // TODO localize
    ))
}
KOTLIN

escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_output" "EscapedDollarRawConstructorPositionalMultilineTrailingLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_output" "\"\"\"Price \\\$5\"\"\" // TODO localize"

escaped_dollar_raw_constructor_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-line-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_line_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_line_comment_fail_dir}/EscapedDollarRawConstructorLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorLineComment() {
    BasicText(text = AnnotatedString("""Price \$5""" // TODO localize
    ))
    BasicText(text = AnnotatedString(
        text = """Price \$5""" // TODO localize
    ))
}
KOTLIN

escaped_dollar_raw_constructor_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_line_comment_fail_output" "EscapedDollarRawConstructorLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_line_comment_fail_output" "AnnotatedString(\"\"\"Price \\\$5\"\"\" // TODO localize"
assert_contains "$escaped_dollar_raw_constructor_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" // TODO localize"

escaped_dollar_raw_constructor_compact_named_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-line-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_line_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_line_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedLineComment() {
    BasicText(text = AnnotatedString(text = """Price \$5""" // TODO localize
    ))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_line_comment_fail_output" "EscapedDollarRawConstructorCompactNamedLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_line_comment_fail_output" "AnnotatedString(text = \"\"\"Price \\\$5\"\"\" // TODO localize"

escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedInlineBlockComment() {
    BasicText(text = AnnotatedString(text = /* TODO localize */ """Price \$5"""))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_output" "EscapedDollarRawConstructorCompactNamedInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_output" "AnnotatedString(text = /* TODO localize */ \"\"\"Price \\\$5\"\"\")"

escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedCloseBlockComment() {
    BasicText(text = AnnotatedString(
        text = /* TODO localize
            */ """Price \$5"""
    ))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_output" "EscapedDollarRawConstructorCompactNamedCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\""

escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedTrailingInlineComment() {
    BasicText(text = AnnotatedString(text = """Price \$5""" /* TODO localize */))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_output" "EscapedDollarRawConstructorCompactNamedTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_output" "AnnotatedString(text = \"\"\"Price \\\$5\"\"\" /* TODO localize */)"

escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-multiline-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedMultilineTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedMultilineTrailingInlineComment() {
    BasicText(text = AnnotatedString(
        text = """Price \$5""" /* TODO localize */
    ))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_output" "EscapedDollarRawConstructorCompactNamedMultilineTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */"

escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-multiline-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedMultilineInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedMultilineInlineBlockComment() {
    BasicText(text = AnnotatedString(
        text = /* TODO localize */ """Price \$5"""
    ))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_output" "EscapedDollarRawConstructorCompactNamedMultilineInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\""

escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-constructor-compact-named-multiline-trailing-block-comment-fail"
mkdir -p "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_dir"
cat > "${escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_dir}/EscapedDollarRawConstructorCompactNamedMultilineTrailingBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawConstructorCompactNamedMultilineTrailingBlockComment() {
    BasicText(text = AnnotatedString(
        text = """Price \$5""" /* TODO localize
            */
    ))
}
KOTLIN

escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_output" "EscapedDollarRawConstructorCompactNamedMultilineTrailingBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize"

escaped_dollar_raw_named_arg_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_close_block_comment_fail_dir}/EscapedDollarRawNamedArgCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                value = /* TODO localize
                    */ """Price \$5"""
            )
            appendRange(
                text = /* TODO localize
                    */ """Price \$5""",
                startIndex = 0,
                endIndex = 3
            )
            append(
                end = 3,
                text = /* TODO localize
                    */ """Price \$5""",
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_close_block_comment_fail_output" "EscapedDollarRawNamedArgCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\""

escaped_dollar_raw_named_arg_compact_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-compact-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_compact_close_block_comment_fail_dir}/EscapedDollarRawNamedArgCompactCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgCompactCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(value = /* TODO localize
                    */ """Price \$5""")
            appendRange(text = /* TODO localize
                    */ """Price \$5""", startIndex = 0, endIndex = 3)
            append(end = 3, text = /* TODO localize
                    */ """Price \$5""", start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output" "EscapedDollarRawNamedArgCompactCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\", startIndex = 0, endIndex = 3)"
assert_contains "$escaped_dollar_raw_named_arg_compact_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\", start = 0)"

escaped_dollar_raw_named_arg_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_inline_block_comment_fail_dir}/EscapedDollarRawNamedArgInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgInlineBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                value = /* TODO localize */ """Price \$5"""
            )
            appendRange(
                text = /* TODO localize */ """Price \$5""",
                startIndex = 0,
                endIndex = 3
            )
            append(
                end = 3,
                text = /* TODO localize */ """Price \$5""",
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_inline_block_comment_fail_output" "EscapedDollarRawNamedArgInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_inline_block_comment_fail_output" "value = /* TODO localize */ \"\"\"Price \\\$5\"\"\""
assert_contains "$escaped_dollar_raw_named_arg_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\","
assert_count "$escaped_dollar_raw_named_arg_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\"," 2

escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-compact-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_dir}/EscapedDollarRawNamedArgCompactInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgCompactInlineBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(value = /* TODO localize */ """Price \$5""")
            appendRange(text = /* TODO localize */ """Price \$5""", startIndex = 0, endIndex = 3)
            append(end = 3, text = /* TODO localize */ """Price \$5""", start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output" "EscapedDollarRawNamedArgCompactInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output" "value = /* TODO localize */ \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\", startIndex = 0, endIndex = 3)"
assert_contains "$escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\", start = 0)"

escaped_dollar_raw_named_arg_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_trailing_inline_comment_fail_dir}/EscapedDollarRawNamedArgTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgTrailingInlineComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(value = """Price \$5""" /* TODO localize */)
            appendRange(text = """Price \$5""" /* TODO localize */, startIndex = 0, endIndex = 3)
            append(end = 3, text = """Price \$5""" /* TODO localize */, start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_output" "EscapedDollarRawNamedArgTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_output" "value = \"\"\"Price \\\$5\"\"\" /* TODO localize */"
assert_contains "$escaped_dollar_raw_named_arg_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */"

escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-multiline-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_dir}/EscapedDollarRawNamedArgMultilineTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgMultilineTrailingInlineComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                value = """Price \$5""" /* TODO localize */
            )
            appendRange(
                text = """Price \$5""" /* TODO localize */,
                startIndex = 0,
                endIndex = 3
            )
            append(
                end = 3,
                text = """Price \$5""" /* TODO localize */,
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output" "EscapedDollarRawNamedArgMultilineTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output" "value = \"\"\"Price \\\$5\"\"\" /* TODO localize */"
assert_contains "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */,"
assert_count "$escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */," 2

escaped_dollar_raw_named_arg_trailing_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-trailing-line-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_trailing_line_comment_fail_dir}/EscapedDollarRawNamedArgTrailingLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgTrailingLineComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                value = """Price \$5""" // TODO localize
            )
            appendRange(
                text = """Price \$5""", // TODO localize
                startIndex = 0,
                endIndex = 3
            )
            append(
                end = 3,
                text = """Price \$5""", // TODO localize
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_trailing_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_output" "EscapedDollarRawNamedArgTrailingLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_output" "value = \"\"\"Price \\\$5\"\"\" // TODO localize"
assert_contains "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize"
assert_count "$escaped_dollar_raw_named_arg_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize" 2

escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-named-arg-compact-trailing-line-comment-fail"
mkdir -p "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_dir"
cat > "${escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_dir}/EscapedDollarRawNamedArgCompactTrailingLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawNamedArgCompactTrailingLineComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(value = """Price \$5""" // TODO localize
            )
            appendRange(text = """Price \$5""", // TODO localize
                startIndex = 0, endIndex = 3)
            append(end = 3, text = """Price \$5""", // TODO localize
                start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output" "EscapedDollarRawNamedArgCompactTrailingLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output" "value = \"\"\"Price \\\$5\"\"\" // TODO localize"
assert_contains "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize"
assert_count "$escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize" 2

escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_dir}/EscapedDollarRawReorderedNamedArgTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgTrailingInlineComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(endIndex = 3, text = """Price \$5""" /* TODO localize */, startIndex = 0)
            append(end = 3, text = """Price \$5""" /* TODO localize */, start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_output" "EscapedDollarRawReorderedNamedArgTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_output" "endIndex = 3, text = \"\"\"Price \\\$5\"\"\" /* TODO localize */, startIndex = 0"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_output" "end = 3, text = \"\"\"Price \\\$5\"\"\" /* TODO localize */, start = 0"

escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-trailing-line-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_dir}/EscapedDollarRawReorderedNamedArgTrailingLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgTrailingLineComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(endIndex = 3, text = """Price \$5""", // TODO localize
                startIndex = 0)
            append(end = 3, text = """Price \$5""", // TODO localize
                start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_output" "EscapedDollarRawReorderedNamedArgTrailingLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_output" "endIndex = 3, text = \"\"\"Price \\\$5\"\"\", // TODO localize"
assert_contains "$escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_output" "end = 3, text = \"\"\"Price \\\$5\"\"\", // TODO localize"

escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-multiline-trailing-inline-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_dir}/EscapedDollarRawReorderedNamedArgMultilineTrailingInlineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgMultilineTrailingInlineComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = """Price \$5""" /* TODO localize */,
                startIndex = 0
            )
            append(
                end = 3,
                text = """Price \$5""" /* TODO localize */,
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_output" "EscapedDollarRawReorderedNamedArgMultilineTrailingInlineCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */,"
assert_count "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\" /* TODO localize */," 2

escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-multiline-trailing-line-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_dir}/EscapedDollarRawReorderedNamedArgMultilineTrailingLineCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgMultilineTrailingLineComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = """Price \$5""", // TODO localize
                startIndex = 0
            )
            append(
                end = 3,
                text = """Price \$5""", // TODO localize
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_output" "EscapedDollarRawReorderedNamedArgMultilineTrailingLineCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize"
assert_count "$escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_output" "text = \"\"\"Price \\\$5\"\"\", // TODO localize" 2

escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_dir}/EscapedDollarRawReorderedNamedArgInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgInlineBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(endIndex = 3, text = /* TODO localize */ """Price \$5""", startIndex = 0)
            append(end = 3, text = /* TODO localize */ """Price \$5""", start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_output" "EscapedDollarRawReorderedNamedArgInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_output" "endIndex = 3, text = /* TODO localize */ \"\"\"Price \\\$5\"\"\", startIndex = 0"
assert_contains "$escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_output" "end = 3, text = /* TODO localize */ \"\"\"Price \\\$5\"\"\", start = 0"

escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-multiline-inline-block-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_dir}/EscapedDollarRawReorderedNamedArgMultilineInlineBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgMultilineInlineBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = /* TODO localize */ """Price \$5""",
                startIndex = 0
            )
            append(
                end = 3,
                text = /* TODO localize */ """Price \$5""",
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_output" "EscapedDollarRawReorderedNamedArgMultilineInlineBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\","
assert_count "$escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Price \\\$5\"\"\"," 2

escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_dir}/EscapedDollarRawReorderedNamedArgCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ """Price \$5""",
                startIndex = 0
            )
            append(
                end = 3,
                text = /* TODO localize
                    */ """Price \$5""",
                start = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_output" "EscapedDollarRawReorderedNamedArgCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\","
assert_count "$escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\"," 2

escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-reordered-named-arg-compact-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_dir}/EscapedDollarRawReorderedNamedArgCompactCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawReorderedNamedArgCompactCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(endIndex = 3, text = /* TODO localize
                    */ """Price \$5""", startIndex = 0)
            append(end = 3, text = /* TODO localize
                    */ """Price \$5""", start = 0)
        }
    )
}
KOTLIN

escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_output" "EscapedDollarRawReorderedNamedArgCompactCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\", startIndex = 0)"
assert_contains "$escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\", start = 0)"

escaped_dollar_raw_positional_append_paths_fail_dir="${tmp_dir}/escaped-dollar-raw-positional-append-paths-fail"
mkdir -p "$escaped_dollar_raw_positional_append_paths_fail_dir"
cat > "${escaped_dollar_raw_positional_append_paths_fail_dir}/EscapedDollarRawPositionalAppendPathsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawPositionalAppendPaths() {
    Text(
        text = buildAnnotatedString {
            appendLine("""Price \$5""")
            appendLine("""Price \$5""" /* TODO localize */)
            appendLine(/* TODO localize */ """Price \$5""")
            append("""Price \$5""")
            append("""Price \$5""" /* TODO localize */)
            append(/* TODO localize */ """Price \$5""")
            appendRange("""Price \$5""", 0, 3)
            appendRange("""Price \$5""" /* TODO localize */, 0, 3)
            appendRange(/* TODO localize */ """Price \$5""", 0, 3)
        }
    )
}
KOTLIN

escaped_dollar_raw_positional_append_paths_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_positional_append_paths_fail_dir")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "EscapedDollarRawPositionalAppendPathsFail.kt"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendLine(\"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendLine(\"\"\"Price \\\$5\"\"\" /* TODO localize */)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendLine(/* TODO localize */ \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(\"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(\"\"\"Price \\\$5\"\"\" /* TODO localize */)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(/* TODO localize */ \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(\"\"\"Price \\\$5\"\"\", 0, 3)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(\"\"\"Price \\\$5\"\"\" /* TODO localize */, 0, 3)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(/* TODO localize */ \"\"\"Price \\\$5\"\"\", 0, 3)"

escaped_dollar_raw_positional_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-raw-positional-close-block-comment-fail"
mkdir -p "$escaped_dollar_raw_positional_close_block_comment_fail_dir"
cat > "${escaped_dollar_raw_positional_close_block_comment_fail_dir}/EscapedDollarRawPositionalCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawPositionalCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                /* TODO localize
                    */ """Price \$5"""
            )
            append(
                /* TODO localize
                    */ """Price \$5"""
            )
            appendRange(
                /* TODO localize
                    */ """Price \$5""",
                0,
                3
            )
        }
    )
}
KOTLIN

escaped_dollar_raw_positional_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_positional_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_raw_positional_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_positional_close_block_comment_fail_output" "EscapedDollarRawPositionalCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_raw_positional_close_block_comment_fail_output" "*/ \"\"\"Price \\\$5\"\"\""

escaped_dollar_positional_append_paths_fail_dir="${tmp_dir}/escaped-dollar-positional-append-paths-fail"
mkdir -p "$escaped_dollar_positional_append_paths_fail_dir"
cat > "${escaped_dollar_positional_append_paths_fail_dir}/EscapedDollarPositionalAppendPathsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarPositionalAppendPaths() {
    Text(
        text = buildAnnotatedString {
            append("Price \$5")
            append("Price \$5" /* TODO localize */)
            append(/* TODO localize */ "Price \$5")
            append(
                /* TODO localize
                    */ "Price \$5"
            )
            appendRange("Price \$5", 0, 3)
            appendRange("Price \$5" /* TODO localize */, 0, 3)
            appendRange(/* TODO localize */ "Price \$5", 0, 3)
            appendRange(
                /* TODO localize
                    */ "Price \$5",
                0,
                3
            )
        }
    )
}
KOTLIN

escaped_dollar_positional_append_paths_fail_output="$(run_expect_exit 1 "$escaped_dollar_positional_append_paths_fail_dir")"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "EscapedDollarPositionalAppendPathsFail.kt"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "append(\"Price \\\$5\")"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "append(\"Price \\\$5\" /* TODO localize */)"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "append(/* TODO localize */ \"Price \\\$5\")"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "*/ \"Price \\\$5\""
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "appendRange(\"Price \\\$5\", 0, 3)"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "appendRange(\"Price \\\$5\" /* TODO localize */, 0, 3)"
assert_contains "$escaped_dollar_positional_append_paths_fail_output" "appendRange(/* TODO localize */ \"Price \\\$5\", 0, 3)"

escaped_dollar_raw_positional_append_paths_fail_dir="${tmp_dir}/escaped-dollar-raw-positional-append-paths-fail"
mkdir -p "$escaped_dollar_raw_positional_append_paths_fail_dir"
cat > "${escaped_dollar_raw_positional_append_paths_fail_dir}/EscapedDollarRawPositionalAppendPathsFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarRawPositionalAppendPaths() {
    Text(
        text = buildAnnotatedString {
            append("""Price \$5""")
            append("""Price \$5""" /* TODO localize */)
            append(/* TODO localize */ """Price \$5""")
            appendRange("""Price \$5""", 0, 3)
            appendRange("""Price \$5""" /* TODO localize */, 0, 3)
            appendRange(/* TODO localize */ """Price \$5""", 0, 3)
        }
    )
}
KOTLIN

escaped_dollar_raw_positional_append_paths_fail_output="$(run_expect_exit 1 "$escaped_dollar_raw_positional_append_paths_fail_dir")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "EscapedDollarRawPositionalAppendPathsFail.kt"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(\"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(\"\"\"Price \\\$5\"\"\" /* TODO localize */)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "append(/* TODO localize */ \"\"\"Price \\\$5\"\"\")"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(\"\"\"Price \\\$5\"\"\", 0, 3)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(\"\"\"Price \\\$5\"\"\" /* TODO localize */, 0, 3)"
assert_contains "$escaped_dollar_raw_positional_append_paths_fail_output" "appendRange(/* TODO localize */ \"\"\"Price \\\$5\"\"\", 0, 3)"

escaped_dollar_append_range_close_block_comment_fail_dir="${tmp_dir}/escaped-dollar-append-range-close-block-comment-fail"
mkdir -p "$escaped_dollar_append_range_close_block_comment_fail_dir"
cat > "${escaped_dollar_append_range_close_block_comment_fail_dir}/EscapedDollarAppendRangeCloseBlockCommentFail.kt" <<'KOTLIN'
@Composable
fun FailEscapedDollarAppendRangeCloseBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                /* TODO localize
                    */ "Price \$5",
                0,
                3
            )
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ "Price \$5",
                startIndex = 0
            )
        }
    )
}
KOTLIN

escaped_dollar_append_range_close_block_comment_fail_output="$(run_expect_exit 1 "$escaped_dollar_append_range_close_block_comment_fail_dir")"
assert_contains "$escaped_dollar_append_range_close_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$escaped_dollar_append_range_close_block_comment_fail_output" "EscapedDollarAppendRangeCloseBlockCommentFail.kt"
assert_contains "$escaped_dollar_append_range_close_block_comment_fail_output" "*/ \"Price \\\$5\","

raw_interpolation_nonleading_dollar_pass_dir="${tmp_dir}/raw-interpolation-nonleading-dollar-pass"
mkdir -p "$raw_interpolation_nonleading_dollar_pass_dir"
cat > "${raw_interpolation_nonleading_dollar_pass_dir}/RawInterpolationNonleadingDollarPass.kt" <<'KOTLIN'
@Composable
fun PassRawInterpolationNonleadingDollar(title: String) {
    Text(text = """Now $title""")
    BasicText(text = """Track: $title""")
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = """Play $title""")
    Text(
        text = buildAnnotatedString {
            appendLine("""Now $title""")
        }
    )
}
KOTLIN

raw_interpolation_nonleading_dollar_pass_output="$(run_expect_exit 0 "$raw_interpolation_nonleading_dollar_pass_dir")"
assert_contains "$raw_interpolation_nonleading_dollar_pass_output" "PASS: no hardcoded UI text literals found in ${raw_interpolation_nonleading_dollar_pass_dir}."

annotated_string_raw_interpolation_nonleading_dollar_pass_dir="${tmp_dir}/annotated-string-raw-interpolation-nonleading-dollar-pass"
mkdir -p "$annotated_string_raw_interpolation_nonleading_dollar_pass_dir"
cat > "${annotated_string_raw_interpolation_nonleading_dollar_pass_dir}/AnnotatedStringRawInterpolationNonleadingDollarPass.kt" <<'KOTLIN'
@Composable
fun PassAnnotatedStringRawInterpolationNonleadingDollar(title: String) {
    BasicText(text = AnnotatedString("""Track: $title"""))
    BasicText(text = AnnotatedString(text = """Now $title"""))
    Text(
        text = buildAnnotatedString {
            append("""Track: $title""")
            appendLine(text = """Now $title""")
        }
    )
}
KOTLIN

annotated_string_raw_interpolation_nonleading_dollar_pass_output="$(run_expect_exit 0 "$annotated_string_raw_interpolation_nonleading_dollar_pass_dir")"
assert_contains "$annotated_string_raw_interpolation_nonleading_dollar_pass_output" "PASS: no hardcoded UI text literals found in ${annotated_string_raw_interpolation_nonleading_dollar_pass_dir}."

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

content_description_block_comment_inline_fail_dir="${tmp_dir}/content-description-block-comment-inline-fail"
mkdir -p "$content_description_block_comment_inline_fail_dir"
cat > "${content_description_block_comment_inline_fail_dir}/ContentDescriptionBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionBlockCommentInline() {
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = /* TODO localize */ "Play track")
}
KOTLIN

content_description_block_comment_inline_fail_output="$(run_expect_exit 1 "$content_description_block_comment_inline_fail_dir")"
assert_contains "$content_description_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_block_comment_inline_fail_output" "ContentDescriptionBlockCommentInlineLiteral.kt"
assert_contains "$content_description_block_comment_inline_fail_output" "\"Play track\""

content_description_raw_block_comment_inline_fail_dir="${tmp_dir}/content-description-raw-block-comment-inline-fail"
mkdir -p "$content_description_raw_block_comment_inline_fail_dir"
cat > "${content_description_raw_block_comment_inline_fail_dir}/ContentDescriptionRawBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionRawBlockCommentInline() {
    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = /* TODO localize */ """Play track""")
}
KOTLIN

content_description_raw_block_comment_inline_fail_output="$(run_expect_exit 1 "$content_description_raw_block_comment_inline_fail_dir")"
assert_contains "$content_description_raw_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_raw_block_comment_inline_fail_output" "ContentDescriptionRawBlockCommentInlineLiteral.kt"
assert_contains "$content_description_raw_block_comment_inline_fail_output" "\"\"\"Play track\"\"\""

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

content_description_multiline_start_block_comment_fail_dir="${tmp_dir}/content-description-multiline-start-block-comment-fail"
mkdir -p "$content_description_multiline_start_block_comment_fail_dir"
cat > "${content_description_multiline_start_block_comment_fail_dir}/ContentDescriptionMultilineStartBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionMultilineStartBlockComment() {
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* TODO localize
            "Play track"
    )
}
KOTLIN

content_description_multiline_start_block_comment_fail_output="$(run_expect_exit 1 "$content_description_multiline_start_block_comment_fail_dir")"
assert_contains "$content_description_multiline_start_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_multiline_start_block_comment_fail_output" "ContentDescriptionMultilineStartBlockCommentLiteral.kt"
assert_contains "$content_description_multiline_start_block_comment_fail_output" "\"Play track\""

content_description_multiline_close_block_comment_inline_literal_fail_dir="${tmp_dir}/content-description-multiline-close-block-comment-inline-literal-fail"
mkdir -p "$content_description_multiline_close_block_comment_inline_literal_fail_dir"
cat > "${content_description_multiline_close_block_comment_inline_literal_fail_dir}/ContentDescriptionMultilineCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionMultilineCloseBlockCommentInlineLiteral() {
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* TODO localize
            */ "Play track"
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = /* TODO localize
            */ """Play track"""
    )
}
KOTLIN

content_description_multiline_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$content_description_multiline_close_block_comment_inline_literal_fail_dir")"
assert_contains "$content_description_multiline_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_multiline_close_block_comment_inline_literal_fail_output" "ContentDescriptionMultilineCloseBlockCommentInlineLiteral.kt"
assert_contains "$content_description_multiline_close_block_comment_inline_literal_fail_output" "*/ \"Play track\""
assert_contains "$content_description_multiline_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Play track\"\"\""

content_description_multiline_inline_block_comment_fail_dir="${tmp_dir}/content-description-multiline-inline-block-comment-fail"
mkdir -p "$content_description_multiline_inline_block_comment_fail_dir"
cat > "${content_description_multiline_inline_block_comment_fail_dir}/ContentDescriptionMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailContentDescriptionMultilineInlineBlockComment() {
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            "Play track" /* TODO localize */
    )
    Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription =
            """Play track""" /* TODO localize */
    )
}
KOTLIN

content_description_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$content_description_multiline_inline_block_comment_fail_dir")"
assert_contains "$content_description_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$content_description_multiline_inline_block_comment_fail_output" "ContentDescriptionMultilineInlineBlockCommentLiteral.kt"
assert_contains "$content_description_multiline_inline_block_comment_fail_output" "\"Play track\" /* TODO localize */"
assert_contains "$content_description_multiline_inline_block_comment_fail_output" "\"\"\"Play track\"\"\" /* TODO localize */"

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

semantics_inline_block_comment_fail_dir="${tmp_dir}/semantics-inline-block-comment-fail"
mkdir -p "$semantics_inline_block_comment_fail_dir"
cat > "${semantics_inline_block_comment_fail_dir}/SemanticsContentDescriptionInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailSemanticsContentDescriptionInlineBlockComment() {
    Box(modifier = Modifier.semantics { contentDescription = /* TODO localize */ "Volume control" })
    Box(modifier = Modifier.semantics { contentDescription = /* TODO localize */ """Volume control""" })
}
KOTLIN

semantics_inline_block_comment_fail_output="$(run_expect_exit 1 "$semantics_inline_block_comment_fail_dir")"
assert_contains "$semantics_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$semantics_inline_block_comment_fail_output" "SemanticsContentDescriptionInlineBlockCommentLiteral.kt"
assert_contains "$semantics_inline_block_comment_fail_output" "\"Volume control\""
assert_contains "$semantics_inline_block_comment_fail_output" "\"\"\"Volume control\"\"\""

semantics_multiline_inline_block_comment_fail_dir="${tmp_dir}/semantics-multiline-inline-block-comment-fail"
mkdir -p "$semantics_multiline_inline_block_comment_fail_dir"
cat > "${semantics_multiline_inline_block_comment_fail_dir}/SemanticsContentDescriptionMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailSemanticsContentDescriptionMultilineInlineBlockComment() {
    Box(
        modifier = Modifier.semantics {
            contentDescription =
                "Volume control" /* TODO localize */
        }
    )
    Box(
        modifier = Modifier.semantics {
            contentDescription =
                """Volume control""" /* TODO localize */
        }
    )
}
KOTLIN

semantics_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$semantics_multiline_inline_block_comment_fail_dir")"
assert_contains "$semantics_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$semantics_multiline_inline_block_comment_fail_output" "SemanticsContentDescriptionMultilineInlineBlockCommentLiteral.kt"
assert_contains "$semantics_multiline_inline_block_comment_fail_output" "\"Volume control\" /* TODO localize */"
assert_contains "$semantics_multiline_inline_block_comment_fail_output" "\"\"\"Volume control\"\"\" /* TODO localize */"

semantics_multiline_close_block_comment_inline_literal_fail_dir="${tmp_dir}/semantics-multiline-close-block-comment-inline-literal-fail"
mkdir -p "$semantics_multiline_close_block_comment_inline_literal_fail_dir"
cat > "${semantics_multiline_close_block_comment_inline_literal_fail_dir}/SemanticsContentDescriptionMultilineCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailSemanticsContentDescriptionMultilineCloseBlockCommentInlineLiteral() {
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* TODO localize
                */ "Volume control"
        }
    )
    Box(
        modifier = Modifier.semantics {
            contentDescription = /* TODO localize
                */ """Volume control"""
        }
    )
}
KOTLIN

semantics_multiline_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$semantics_multiline_close_block_comment_inline_literal_fail_dir")"
assert_contains "$semantics_multiline_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$semantics_multiline_close_block_comment_inline_literal_fail_output" "SemanticsContentDescriptionMultilineCloseBlockCommentInlineLiteral.kt"
assert_contains "$semantics_multiline_close_block_comment_inline_literal_fail_output" "*/ \"Volume control\""
assert_contains "$semantics_multiline_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Volume control\"\"\""

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

text_named_arg_block_comment_inline_fail_dir="${tmp_dir}/text-named-arg-block-comment-inline-fail"
mkdir -p "$text_named_arg_block_comment_inline_fail_dir"
cat > "${text_named_arg_block_comment_inline_fail_dir}/TextNamedArgBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextNamedArgBlockCommentInline() {
    Text(text = /* TODO localize */ "Now playing")
}
KOTLIN

text_named_arg_block_comment_inline_fail_output="$(run_expect_exit 1 "$text_named_arg_block_comment_inline_fail_dir")"
assert_contains "$text_named_arg_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_named_arg_block_comment_inline_fail_output" "TextNamedArgBlockCommentInlineLiteral.kt"
assert_contains "$text_named_arg_block_comment_inline_fail_output" "\"Now playing\""

text_named_arg_raw_block_comment_inline_fail_dir="${tmp_dir}/text-named-arg-raw-block-comment-inline-fail"
mkdir -p "$text_named_arg_raw_block_comment_inline_fail_dir"
cat > "${text_named_arg_raw_block_comment_inline_fail_dir}/TextNamedArgRawBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextNamedArgRawBlockCommentInline() {
    Text(text = /* TODO localize */ """Now playing""")
}
KOTLIN

text_named_arg_raw_block_comment_inline_fail_output="$(run_expect_exit 1 "$text_named_arg_raw_block_comment_inline_fail_dir")"
assert_contains "$text_named_arg_raw_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_named_arg_raw_block_comment_inline_fail_output" "TextNamedArgRawBlockCommentInlineLiteral.kt"
assert_contains "$text_named_arg_raw_block_comment_inline_fail_output" "\"\"\"Now playing\"\"\""

text_named_assignment_close_block_comment_inline_literal_fail_dir="${tmp_dir}/text-named-assignment-close-block-comment-inline-literal-fail"
mkdir -p "$text_named_assignment_close_block_comment_inline_literal_fail_dir"
cat > "${text_named_assignment_close_block_comment_inline_literal_fail_dir}/TextNamedAssignmentCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextNamedAssignmentCloseBlockCommentInlineLiteral() {
    Text(
        text = /* TODO localize
            */ "Now playing"
    )
    BasicText(
        text = /* TODO localize
            */ """Now playing"""
    )
}
KOTLIN

text_named_assignment_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$text_named_assignment_close_block_comment_inline_literal_fail_dir")"
assert_contains "$text_named_assignment_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_named_assignment_close_block_comment_inline_literal_fail_output" "TextNamedAssignmentCloseBlockCommentInlineLiteral.kt"
assert_contains "$text_named_assignment_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$text_named_assignment_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

text_multiline_positional_call_start_block_comment_fail_dir="${tmp_dir}/text-multiline-positional-call-start-block-comment-fail"
mkdir -p "$text_multiline_positional_call_start_block_comment_fail_dir"
cat > "${text_multiline_positional_call_start_block_comment_fail_dir}/TextMultilinePositionalCallStartBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalCallStartBlockComment() {
    Text( /* TODO localize
        "Now playing"
    )
}
KOTLIN

text_multiline_positional_call_start_block_comment_fail_output="$(run_expect_exit 1 "$text_multiline_positional_call_start_block_comment_fail_dir")"
assert_contains "$text_multiline_positional_call_start_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_call_start_block_comment_fail_output" "TextMultilinePositionalCallStartBlockCommentLiteral.kt"
assert_contains "$text_multiline_positional_call_start_block_comment_fail_output" "\"Now playing\""

text_multiline_positional_call_close_block_comment_inline_literal_fail_dir="${tmp_dir}/text-multiline-positional-call-close-block-comment-inline-literal-fail"
mkdir -p "$text_multiline_positional_call_close_block_comment_inline_literal_fail_dir"
cat > "${text_multiline_positional_call_close_block_comment_inline_literal_fail_dir}/TextMultilinePositionalCallCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalCallCloseBlockCommentInlineLiteral() {
    Text( /* TODO localize
        */ "Now playing"
    )
    Text( /* TODO localize
        */ """Now playing"""
    )
}
KOTLIN

text_multiline_positional_call_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$text_multiline_positional_call_close_block_comment_inline_literal_fail_dir")"
assert_contains "$text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "TextMultilinePositionalCallCloseBlockCommentInlineLiteral.kt"
assert_contains "$text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

text_multiline_positional_literal_prefixed_block_comment_fail_dir="${tmp_dir}/text-multiline-positional-literal-prefixed-block-comment-fail"
mkdir -p "$text_multiline_positional_literal_prefixed_block_comment_fail_dir"
cat > "${text_multiline_positional_literal_prefixed_block_comment_fail_dir}/TextMultilinePositionalLiteralPrefixedBlockComment.kt" <<'KOTLIN'
@Composable
fun FailTextMultilinePositionalLiteralPrefixedBlockComment() {
    Text(
        /* TODO localize */ "Now playing"
    )
}
KOTLIN

text_multiline_positional_literal_prefixed_block_comment_fail_output="$(run_expect_exit 1 "$text_multiline_positional_literal_prefixed_block_comment_fail_dir")"
assert_contains "$text_multiline_positional_literal_prefixed_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_positional_literal_prefixed_block_comment_fail_output" "TextMultilinePositionalLiteralPrefixedBlockComment.kt"
assert_contains "$text_multiline_positional_literal_prefixed_block_comment_fail_output" "\"Now playing\""

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

basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_dir="${tmp_dir}/basic-text-multiline-positional-call-close-block-comment-inline-literal-fail"
mkdir -p "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_dir"
cat > "${basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_dir}/BasicTextMultilinePositionalCallCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailBasicTextMultilinePositionalCallCloseBlockCommentInlineLiteral() {
    BasicText( /* TODO localize
        */ "Now playing"
    )
    BasicText( /* TODO localize
        */ """Now playing"""
    )
}
KOTLIN

basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_dir")"
assert_contains "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "BasicTextMultilinePositionalCallCloseBlockCommentInlineLiteral.kt"
assert_contains "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$basic_text_multiline_positional_call_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

text_multiline_named_assignment_literal_prefixed_block_comment_fail_dir="${tmp_dir}/text-multiline-named-assignment-literal-prefixed-block-comment-fail"
mkdir -p "$text_multiline_named_assignment_literal_prefixed_block_comment_fail_dir"
cat > "${text_multiline_named_assignment_literal_prefixed_block_comment_fail_dir}/TextMultilineNamedAssignmentLiteralPrefixedBlockComment.kt" <<'KOTLIN'
@Composable
fun FailTextMultilineNamedAssignmentLiteralPrefixedBlockComment() {
    Text(
        text =
            /* TODO localize */ "Now playing"
    )
}
KOTLIN

text_multiline_named_assignment_literal_prefixed_block_comment_fail_output="$(run_expect_exit 1 "$text_multiline_named_assignment_literal_prefixed_block_comment_fail_dir")"
assert_contains "$text_multiline_named_assignment_literal_prefixed_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$text_multiline_named_assignment_literal_prefixed_block_comment_fail_output" "TextMultilineNamedAssignmentLiteralPrefixedBlockComment.kt"
assert_contains "$text_multiline_named_assignment_literal_prefixed_block_comment_fail_output" "\"Now playing\""

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

annotated_string_constructor_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-constructor-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_constructor_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_constructor_close_block_comment_inline_literal_fail_dir}/AnnotatedStringConstructorCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringConstructorCloseBlockCommentInlineLiteral() {
    BasicText(
        text = AnnotatedString( /* TODO localize
            */ "Now playing"
        )
    )
    BasicText(
        text = AnnotatedString( /* TODO localize
            */ """Now playing"""
        )
    )
}
KOTLIN

annotated_string_constructor_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_constructor_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_constructor_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_constructor_close_block_comment_inline_literal_fail_output" "AnnotatedStringConstructorCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_constructor_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$annotated_string_constructor_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

annotated_string_named_arg_dedup_fail_dir="${tmp_dir}/annotated-string-named-arg-dedup-fail"
mkdir -p "$annotated_string_named_arg_dedup_fail_dir"
cat > "${annotated_string_named_arg_dedup_fail_dir}/AnnotatedStringNamedArgDedupLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgDedup() {
    BasicText(text = AnnotatedString(text = "Now playing"))
}
KOTLIN

annotated_string_named_arg_dedup_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_dedup_fail_dir")"
assert_contains "$annotated_string_named_arg_dedup_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_dedup_fail_output" "AnnotatedStringNamedArgDedupLiteral.kt"
assert_count "$annotated_string_named_arg_dedup_fail_output" "AnnotatedStringNamedArgDedupLiteral.kt" 1

annotated_string_named_arg_block_comment_inline_fail_dir="${tmp_dir}/annotated-string-named-arg-block-comment-inline-fail"
mkdir -p "$annotated_string_named_arg_block_comment_inline_fail_dir"
cat > "${annotated_string_named_arg_block_comment_inline_fail_dir}/AnnotatedStringNamedArgBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgBlockCommentInline() {
    BasicText(text = AnnotatedString(text = /* TODO localize */ "Now playing"))
}
KOTLIN

annotated_string_named_arg_block_comment_inline_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_block_comment_inline_fail_dir")"
assert_contains "$annotated_string_named_arg_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_block_comment_inline_fail_output" "AnnotatedStringNamedArgBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_block_comment_inline_fail_output" "\"Now playing\""

annotated_string_named_arg_raw_block_comment_inline_fail_dir="${tmp_dir}/annotated-string-named-arg-raw-block-comment-inline-fail"
mkdir -p "$annotated_string_named_arg_raw_block_comment_inline_fail_dir"
cat > "${annotated_string_named_arg_raw_block_comment_inline_fail_dir}/AnnotatedStringNamedArgRawBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgRawBlockCommentInline() {
    BasicText(text = AnnotatedString(text = /* TODO localize */ """Now playing"""))
}
KOTLIN

annotated_string_named_arg_raw_block_comment_inline_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_raw_block_comment_inline_fail_dir")"
assert_contains "$annotated_string_named_arg_raw_block_comment_inline_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_raw_block_comment_inline_fail_output" "AnnotatedStringNamedArgRawBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_raw_block_comment_inline_fail_output" "\"\"\"Now playing\"\"\""

annotated_string_named_arg_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-named-arg-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_named_arg_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_named_arg_close_block_comment_inline_literal_fail_dir}/AnnotatedStringNamedArgCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgCloseBlockCommentInlineLiteral() {
    BasicText(
        text = AnnotatedString(
            text = /* TODO localize
                */ "Now playing"
        )
    )
    BasicText(
        text = AnnotatedString(
            text = /* TODO localize
                */ """Now playing"""
        )
    )
}
KOTLIN

annotated_string_named_arg_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_named_arg_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_close_block_comment_inline_literal_fail_output" "AnnotatedStringNamedArgCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$annotated_string_named_arg_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

annotated_string_append_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append("Now playing" /* TODO localize */)
            append("""Now playing""" /* TODO localize */)
        }
    )
}
KOTLIN

annotated_string_append_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_trailing_inline_block_comment_fail_output" "\"Now playing\" /* TODO localize */"
assert_contains "$annotated_string_append_trailing_inline_block_comment_fail_output" "\"\"\"Now playing\"\"\" /* TODO localize */"

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

annotated_string_named_arg_append_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_trailing_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append(text = "Now playing" /* TODO localize */)
            append(text = """Now playing""" /* TODO localize */)
        }
    )
}
KOTLIN

annotated_string_named_arg_append_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */"
assert_contains "$annotated_string_named_arg_append_trailing_inline_block_comment_fail_output" "text = \"\"\"Now playing\"\"\" /* TODO localize */"

annotated_string_named_arg_append_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-named-arg-append-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_named_arg_append_close_block_comment_inline_literal_fail_dir}/AnnotatedStringNamedArgAppendCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            append(
                text = /* TODO localize
                    */ "Now playing"
            )
            append(
                start = 0,
                end = 3,
                text = /* TODO localize
                    */ """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_output" "AnnotatedStringNamedArgAppendCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$annotated_string_named_arg_append_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendReorderedTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReorderedTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append(end = 3, text = "Now playing" /* TODO localize */, start = 0)
            append(end = 3, text = """Now playing""" /* TODO localize */, start = 0)
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendReorderedTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_output" "end = 3, text = \"Now playing\" /* TODO localize */, start = 0"
assert_contains "$annotated_string_named_arg_append_reordered_trailing_inline_block_comment_fail_output" "end = 3, text = \"\"\"Now playing\"\"\" /* TODO localize */, start = 0"

annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-multiline-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendReorderedMultilineTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReorderedMultilineTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append(
                end = 3,
                text = "Now playing" /* TODO localize */,
                start = 0
            )
            append(
                end = 3,
                text = """Now playing""" /* TODO localize */,
                start = 0
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendReorderedMultilineTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */,"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_output" "text = \"\"\"Now playing\"\"\" /* TODO localize */,"

annotated_string_named_arg_append_reordered_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendReorderedInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReorderedInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append(end = 3, text = /* TODO localize */ "Now playing", start = 0)
            append(end = 3, text = /* TODO localize */ """Now playing""", start = 0)
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendReorderedInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_output" "end = 3, text = /* TODO localize */ \"Now playing\", start = 0"
assert_contains "$annotated_string_named_arg_append_reordered_inline_block_comment_fail_output" "end = 3, text = /* TODO localize */ \"\"\"Now playing\"\"\", start = 0"

annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-multiline-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendReorderedMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReorderedMultilineInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            append(
                end = 3,
                text = /* TODO localize */ "Now playing",
                start = 0
            )
            append(
                end = 3,
                text = /* TODO localize */ """Now playing""",
                start = 0
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendReorderedMultilineInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"Now playing\","
assert_contains "$annotated_string_named_arg_append_reordered_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Now playing\"\"\","

annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-named-arg-append-reordered-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_dir}/AnnotatedStringNamedArgAppendReorderedCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendReorderedCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            append(
                end = 3,
                text = /* TODO localize
                    */ "Now playing",
                start = 0
            )
            append(
                end = 3,
                text = /* TODO localize
                    */ """Now playing""",
                start = 0
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_output" "AnnotatedStringNamedArgAppendReorderedCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\","
assert_contains "$annotated_string_named_arg_append_reordered_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\","

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

annotated_string_append_range_named_args_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-named-args-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_named_args_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_named_args_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeNamedArgsInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeNamedArgsInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(text = /* TODO localize */ "Now playing", startIndex = 0, endIndex = 3)
            appendRange(text = /* TODO localize */ """Now playing""", startIndex = 0, endIndex = 3)
        }
    )
}
KOTLIN

annotated_string_append_range_named_args_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_named_args_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_named_args_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_named_args_inline_block_comment_fail_output" "AnnotatedStringAppendRangeNamedArgsInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_named_args_inline_block_comment_fail_output" "text = /* TODO localize */ \"Now playing\", startIndex = 0, endIndex = 3"
assert_contains "$annotated_string_append_range_named_args_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Now playing\"\"\", startIndex = 0, endIndex = 3"

annotated_string_append_range_named_args_multiline_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-named-args-multiline-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_named_args_multiline_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeNamedArgsMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeNamedArgsMultilineInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                text = /* TODO localize */ "Now playing",
                startIndex = 0,
                endIndex = 3
            )
            appendRange(
                text = /* TODO localize */ """Now playing""",
                startIndex = 0,
                endIndex = 3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_named_args_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_output" "AnnotatedStringAppendRangeNamedArgsMultilineInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"Now playing\","
assert_contains "$annotated_string_append_range_named_args_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Now playing\"\"\","

annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-append-range-named-args-multiline-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_dir}/AnnotatedStringAppendRangeNamedArgsMultilineCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeNamedArgsMultilineCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                text = /* TODO localize
                    */ "Now playing",
                startIndex = 0,
                endIndex = 3
            )
            appendRange(
                text = /* TODO localize
                    */ """Now playing""",
                startIndex = 0,
                endIndex = 3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_output" "AnnotatedStringAppendRangeNamedArgsMultilineCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\","
assert_contains "$annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\","

annotated_string_append_range_positional_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-positional-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_positional_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_positional_inline_block_comment_fail_dir}/AnnotatedStringAppendRangePositionalInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangePositionalInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(/* TODO localize */ "Now playing", 0, 3)
            appendRange(/* TODO localize */ """Now playing""", 0, 3)
        }
    )
}
KOTLIN

annotated_string_append_range_positional_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_positional_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_positional_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_positional_inline_block_comment_fail_output" "AnnotatedStringAppendRangePositionalInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_positional_inline_block_comment_fail_output" "appendRange(/* TODO localize */ \"Now playing\", 0, 3)"
assert_contains "$annotated_string_append_range_positional_inline_block_comment_fail_output" "/* TODO localize */ \"\"\"Now playing\"\"\", 0, 3"

annotated_string_append_range_positional_multiline_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-positional-multiline-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_positional_multiline_inline_block_comment_fail_dir}/AnnotatedStringAppendRangePositionalMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangePositionalMultilineInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                /* TODO localize */ "Now playing",
                0,
                3
            )
            appendRange(
                /* TODO localize */ """Now playing""",
                0,
                3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_positional_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_output" "AnnotatedStringAppendRangePositionalMultilineInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_output" "/* TODO localize */ \"Now playing\","
assert_contains "$annotated_string_append_range_positional_multiline_inline_block_comment_fail_output" "/* TODO localize */ \"\"\"Now playing\"\"\","

annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-append-range-positional-multiline-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_dir}/AnnotatedStringAppendRangePositionalMultilineCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangePositionalMultilineCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                /* TODO localize
                    */ "Now playing",
                0,
                3
            )
            appendRange(
                /* TODO localize
                    */ """Now playing""",
                0,
                3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_output" "AnnotatedStringAppendRangePositionalMultilineCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\","
assert_contains "$annotated_string_append_range_positional_multiline_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\","

annotated_string_append_range_positional_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-positional-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_positional_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendRangePositionalTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangePositionalTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange("Now playing" /* TODO localize */, 0, 3)
            appendRange("""Now playing""" /* TODO localize */, 0, 3)
        }
    )
}
KOTLIN

annotated_string_append_range_positional_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendRangePositionalTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_output" "\"Now playing\" /* TODO localize */, 0, 3"
assert_contains "$annotated_string_append_range_positional_trailing_inline_block_comment_fail_output" "\"\"\"Now playing\"\"\" /* TODO localize */, 0, 3"

annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-positional-multiline-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendRangePositionalMultilineTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangePositionalMultilineTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                "Now playing" /* TODO localize */,
                0,
                3
            )
            appendRange(
                """Now playing""" /* TODO localize */,
                0,
                3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendRangePositionalMultilineTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_output" "\"Now playing\" /* TODO localize */,"
assert_contains "$annotated_string_append_range_positional_multiline_trailing_inline_block_comment_fail_output" "\"\"\"Now playing\"\"\" /* TODO localize */,"

annotated_string_append_range_named_args_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-named-args-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_named_args_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeNamedArgsTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeNamedArgsTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(text = "Now playing" /* TODO localize */, startIndex = 0, endIndex = 3)
            appendRange(text = """Now playing""" /* TODO localize */, startIndex = 0, endIndex = 3)
        }
    )
}
KOTLIN

annotated_string_append_range_named_args_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendRangeNamedArgsTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */, startIndex = 0, endIndex = 3"
assert_contains "$annotated_string_append_range_named_args_trailing_inline_block_comment_fail_output" "text = \"\"\"Now playing\"\"\" /* TODO localize */, startIndex = 0, endIndex = 3"

annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-named-args-multiline-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeNamedArgsMultilineTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeNamedArgsMultilineTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                text = "Now playing" /* TODO localize */,
                startIndex = 0,
                endIndex = 3
            )
            appendRange(
                text = """Now playing""" /* TODO localize */,
                startIndex = 0,
                endIndex = 3
            )
        }
    )
}
KOTLIN

annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendRangeNamedArgsMultilineTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */,"
assert_contains "$annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_output" "text = \"\"\"Now playing\"\"\" /* TODO localize */,"

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

annotated_string_append_range_reordered_named_args_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-reordered-named-args-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_reordered_named_args_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeReorderedNamedArgsInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeReorderedNamedArgsInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(endIndex = 3, text = /* TODO localize */ "Now playing", startIndex = 0)
            appendRange(endIndex = 3, text = /* TODO localize */ """Now playing""", startIndex = 0)
        }
    )
}
KOTLIN

annotated_string_append_range_reordered_named_args_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_output" "AnnotatedStringAppendRangeReorderedNamedArgsInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_output" "endIndex = 3, text = /* TODO localize */ \"Now playing\", startIndex = 0"
assert_contains "$annotated_string_append_range_reordered_named_args_inline_block_comment_fail_output" "endIndex = 3, text = /* TODO localize */ \"\"\"Now playing\"\"\", startIndex = 0"

annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-reordered-named-args-multiline-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeReorderedNamedArgsMultilineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeReorderedNamedArgsMultilineInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = /* TODO localize */ "Now playing",
                startIndex = 0
            )
            appendRange(
                endIndex = 3,
                text = /* TODO localize */ """Now playing""",
                startIndex = 0
            )
        }
    )
}
KOTLIN

annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_output" "AnnotatedStringAppendRangeReorderedNamedArgsMultilineInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"Now playing\","
assert_contains "$annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_output" "text = /* TODO localize */ \"\"\"Now playing\"\"\","

annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-append-range-reordered-named-args-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_dir}/AnnotatedStringAppendRangeReorderedNamedArgsCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeReorderedNamedArgsCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ "Now playing",
                startIndex = 0
            )
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ """Now playing""",
                startIndex = 0
            )
        }
    )
}
KOTLIN

annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_output" "AnnotatedStringAppendRangeReorderedNamedArgsCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\","
assert_contains "$annotated_string_append_range_reordered_named_args_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\","

annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-range-reordered-named-args-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendRangeReorderedNamedArgsTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendRangeReorderedNamedArgsTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = "Now playing" /* TODO localize */,
                startIndex = 0
            )
            appendRange(
                endIndex = 3,
                text = """Now playing""" /* TODO localize */,
                startIndex = 0
            )
        }
    )
}
KOTLIN

annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendRangeReorderedNamedArgsTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */,"
assert_contains "$annotated_string_append_range_reordered_named_args_trailing_inline_block_comment_fail_output" "text = \"\"\"Now playing\"\"\" /* TODO localize */,"

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

annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-append-multiline-reordered-named-args-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_dir}/AnnotatedStringAppendMultilineReorderedNamedArgsCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendMultilineReorderedNamedArgsCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendRange(
                endIndex = 3,
                text = /* TODO localize
                    */ "Now playing",
                startIndex = 0
            )
            append(
                end = 3,
                text = /* TODO localize
                    */ """Now playing""",
                start = 0
            )
        }
    )
}
KOTLIN

annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_output" "AnnotatedStringAppendMultilineReorderedNamedArgsCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\","
assert_contains "$annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\","

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

annotated_string_append_line_multiline_named_args_comment_fail_dir="${tmp_dir}/annotated-string-append-line-multiline-named-args-comment-fail"
mkdir -p "$annotated_string_append_line_multiline_named_args_comment_fail_dir"
cat > "${annotated_string_append_line_multiline_named_args_comment_fail_dir}/AnnotatedStringAppendLineMultilineNamedArgsCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineMultilineNamedArgsComment() {
    Text(
        text = buildAnnotatedString {
            appendLine( /* TODO localize
                text = "Now playing"
            )
            appendLine(
                // TODO localize
                value = """Now playing""" // TODO localize
            )
        }
    )
}
KOTLIN

annotated_string_append_line_multiline_named_args_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_multiline_named_args_comment_fail_dir")"
assert_contains "$annotated_string_append_line_multiline_named_args_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_multiline_named_args_comment_fail_output" "AnnotatedStringAppendLineMultilineNamedArgsCommentLiteral.kt"
assert_contains "$annotated_string_append_line_multiline_named_args_comment_fail_output" "text = \"Now playing\""
assert_contains "$annotated_string_append_line_multiline_named_args_comment_fail_output" "value = \"\"\"Now playing\"\"\" // TODO localize"

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

annotated_string_append_line_multiline_positional_comment_fail_dir="${tmp_dir}/annotated-string-append-line-multiline-positional-comment-fail"
mkdir -p "$annotated_string_append_line_multiline_positional_comment_fail_dir"
cat > "${annotated_string_append_line_multiline_positional_comment_fail_dir}/AnnotatedStringAppendLineMultilinePositionalCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineMultilinePositionalComment() {
    Text(
        text = buildAnnotatedString {
            appendLine( // TODO localize
                "Now playing" // TODO localize
            )
            appendLine(
                // TODO localize
                """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_append_line_multiline_positional_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_multiline_positional_comment_fail_dir")"
assert_contains "$annotated_string_append_line_multiline_positional_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_multiline_positional_comment_fail_output" "AnnotatedStringAppendLineMultilinePositionalCommentLiteral.kt"
assert_contains "$annotated_string_append_line_multiline_positional_comment_fail_output" "\"Now playing\" // TODO localize"
assert_contains "$annotated_string_append_line_multiline_positional_comment_fail_output" "\"\"\"Now playing\"\"\""

annotated_string_append_line_positional_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-line-positional-inline-block-comment-fail"
mkdir -p "$annotated_string_append_line_positional_inline_block_comment_fail_dir"
cat > "${annotated_string_append_line_positional_inline_block_comment_fail_dir}/AnnotatedStringAppendLinePositionalInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLinePositionalInlineBlockComment() {
    Text(
        text = buildAnnotatedString {
            appendLine(/* TODO localize */ "Now playing")
            appendLine(
                /* TODO localize */ """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_append_line_positional_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_positional_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_line_positional_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_positional_inline_block_comment_fail_output" "AnnotatedStringAppendLinePositionalInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_line_positional_inline_block_comment_fail_output" "appendLine(/* TODO localize */ \"Now playing\")"
assert_contains "$annotated_string_append_line_positional_inline_block_comment_fail_output" "/* TODO localize */ \"\"\"Now playing\"\"\""

annotated_string_append_line_positional_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-append-line-positional-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_append_line_positional_trailing_inline_block_comment_fail_dir}/AnnotatedStringAppendLinePositionalTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLinePositionalTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendLine("Now playing" /* TODO localize */)
            appendLine(
                """Now playing""" /* TODO localize */
            )
        }
    )
}
KOTLIN

annotated_string_append_line_positional_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_output" "AnnotatedStringAppendLinePositionalTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_output" "\"Now playing\" /* TODO localize */"
assert_contains "$annotated_string_append_line_positional_trailing_inline_block_comment_fail_output" "\"\"\"Now playing\"\"\" /* TODO localize */"

annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-append-line-multiline-positional-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_dir}/AnnotatedStringAppendLineMultilinePositionalCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringAppendLineMultilinePositionalCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                /* TODO localize
                    */ "Now playing"
            )
            appendLine(
                /* TODO localize
                    */ """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_output" "AnnotatedStringAppendLineMultilinePositionalCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$annotated_string_append_line_multiline_positional_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

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

annotated_string_named_arg_append_line_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-line-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_line_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_line_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendLineInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendLineInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendLine(text = /* TODO localize */ "Now playing")
            appendLine(
                value = /* TODO localize */ """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_line_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_line_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_line_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_line_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendLineInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_line_inline_block_comment_fail_output" "text = /* TODO localize */ \"Now playing\""
assert_contains "$annotated_string_named_arg_append_line_inline_block_comment_fail_output" "value = /* TODO localize */ \"\"\"Now playing\"\"\""

annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_dir="${tmp_dir}/annotated-string-named-arg-append-line-trailing-inline-block-comment-fail"
mkdir -p "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_dir"
cat > "${annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_dir}/AnnotatedStringNamedArgAppendLineTrailingInlineBlockCommentLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendLineTrailingInlineBlockCommentLiteral() {
    Text(
        text = buildAnnotatedString {
            appendLine(text = "Now playing" /* TODO localize */)
            appendLine(
                value = """Now playing""" /* TODO localize */
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_dir")"
assert_contains "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_output" "AnnotatedStringNamedArgAppendLineTrailingInlineBlockCommentLiteral.kt"
assert_contains "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_output" "text = \"Now playing\" /* TODO localize */"
assert_contains "$annotated_string_named_arg_append_line_trailing_inline_block_comment_fail_output" "value = \"\"\"Now playing\"\"\" /* TODO localize */"

annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_dir="${tmp_dir}/annotated-string-named-arg-append-line-close-block-comment-inline-literal-fail"
mkdir -p "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_dir"
cat > "${annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_dir}/AnnotatedStringNamedArgAppendLineCloseBlockCommentInlineLiteral.kt" <<'KOTLIN'
@Composable
fun FailAnnotatedStringNamedArgAppendLineCloseBlockCommentInlineLiteral() {
    Text(
        text = buildAnnotatedString {
            appendLine(
                text = /* TODO localize
                    */ "Now playing"
            )
            appendLine(
                value = /* TODO localize
                    */ """Now playing"""
            )
        }
    )
}
KOTLIN

annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_output="$(run_expect_exit 1 "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_dir")"
assert_contains "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_output" "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
assert_contains "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_output" "AnnotatedStringNamedArgAppendLineCloseBlockCommentInlineLiteral.kt"
assert_contains "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_output" "*/ \"Now playing\""
assert_contains "$annotated_string_named_arg_append_line_close_block_comment_inline_literal_fail_output" "*/ \"\"\"Now playing\"\"\""

echo "check-hardcoded-ui-text-literals tests passed."
