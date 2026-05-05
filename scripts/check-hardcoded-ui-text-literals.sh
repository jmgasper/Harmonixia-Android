#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

default_targets=(
  "app/src/main/java/com/harmonixia/android/ui"
)

targets=()
if [[ $# -gt 0 ]]; then
  targets=("$@")
else
  targets=("${default_targets[@]}")
fi

# Conservative literal checks for Compose UI callsites.
escaped_string_literal_pattern='"([^"\\$]|\\.)+"'
raw_string_literal_pattern='"""[^$\n][^"\n]*"""'
literal_pattern="(${escaped_string_literal_pattern}|${raw_string_literal_pattern})"

text_call_pattern="Text\\(\\s*${literal_pattern}"
basic_text_call_pattern="BasicText\\(\\s*${literal_pattern}"
annotated_string_constructor_pattern="AnnotatedString\\(\\s*${literal_pattern}"
annotated_string_named_text_pattern="AnnotatedString\\([^)]*text\\s*=\\s*(/[*].*[*]/\\s*)?${literal_pattern}"
named_text_pattern="text\\s*=\\s*(/[*].*[*]/\\s*)?${literal_pattern}"
content_description_pattern="contentDescription\\s*=\\s*(/[*].*[*]/\\s*)?${literal_pattern}"
annotated_append_call_start_pattern='append(Line|Range)?[[:space:]]*[(]'
annotated_append_literal_pattern="${literal_pattern}"
multiline_direct_call_start_pattern='(Text|BasicText|AnnotatedString)[[:space:]]*[(][[:space:]]*((//.*)|(/[*].*))?[[:space:]]*$'
multiline_direct_literal_line_pattern="^[[:space:]]*((/[*].*[*]/|[*]/)[[:space:]]*)*${literal_pattern}[[:space:]]*,?[[:space:]]*((//.*)|(/[*].*))?[[:space:]]*$"
multiline_assignment_start_pattern='(text|contentDescription)[[:space:]]*=[[:space:]]*((//.*)|(/[*].*))?[[:space:]]*$'

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

rg --no-heading --line-number --color never --glob '*.kt' "$text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$basic_text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_constructor_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$content_description_pattern" "${targets[@]}" >>"$violations_file" || true

while IFS= read -r kotlin_file; do
  awk -v append_call_start_pattern="$annotated_append_call_start_pattern" -v append_literal_pattern="$annotated_append_literal_pattern" -v direct_call_start_pattern="$multiline_direct_call_start_pattern" -v direct_literal_line_pattern="$multiline_direct_literal_line_pattern" -v assignment_start_pattern="$multiline_assignment_start_pattern" '
    function is_ignorable_pending_line(source) {
      return source ~ /^[[:space:]]*$/ || source ~ /^[[:space:]]*\/\// || source ~ /^[[:space:]]*\/\*/ || source ~ /^[[:space:]]*\*/ || source ~ /^[[:space:]]*\*\//
    }

    function update_brace_depth(source, i, c) {
      for (i = 1; i <= length(source); i++) {
        c = substr(source, i, 1)
        if (c == "{") {
          brace_depth++
        } else if (c == "}") {
          brace_depth--
        }
      }
      if (brace_depth < 0) {
        brace_depth = 0
      }
    }

    function update_append_paren_depth(source, i, c) {
      for (i = 1; i <= length(source); i++) {
        c = substr(source, i, 1)
        if (c == "(") {
          append_paren_depth++
        } else if (c == ")") {
          append_paren_depth--
        }
      }
      if (append_paren_depth < 0) {
        append_paren_depth = 0
      }
    }

    {
      if (pending_multiline_direct_literal_call == 1) {
        if ($0 ~ direct_literal_line_pattern) {
          printf "%s:%d:%s\n", FILENAME, NR, $0
          pending_multiline_direct_literal_call = 0
        } else if (is_ignorable_pending_line($0)) {
          # keep waiting through blank/comment lines
        } else {
          pending_multiline_direct_literal_call = 0
        }
      }

      if ($0 ~ direct_call_start_pattern) {
        pending_multiline_direct_literal_call = 1
      }

      if (pending_multiline_assignment_literal == 1) {
        if ($0 ~ direct_literal_line_pattern) {
          printf "%s:%d:%s\n", FILENAME, NR, $0
          pending_multiline_assignment_literal = 0
        } else if (is_ignorable_pending_line($0)) {
          # keep waiting through blank/comment lines
        } else {
          pending_multiline_assignment_literal = 0
        }
      }

      if ($0 ~ assignment_start_pattern) {
        pending_multiline_assignment_literal = 1
      }

      if (in_annotated_block == 0 && $0 ~ /buildAnnotatedString[[:space:]]*\{/) {
        in_annotated_block = 1
        brace_depth = 0
      }

      if (in_annotated_block == 1) {
        if (in_append_call == 1) {
          if ($0 ~ append_literal_pattern) {
            printf "%s:%d:%s\n", FILENAME, NR, $0
          }
          update_append_paren_depth($0)
          if (append_paren_depth <= 0) {
            in_append_call = 0
            append_paren_depth = 0
          }
        }

        if (in_append_call == 0 && $0 ~ append_call_start_pattern) {
          in_append_call = 1
          append_paren_depth = 0
          if ($0 ~ append_literal_pattern) {
            printf "%s:%d:%s\n", FILENAME, NR, $0
          }
          update_append_paren_depth($0)
          if (append_paren_depth <= 0) {
            in_append_call = 0
            append_paren_depth = 0
          }
        }

        update_brace_depth($0)
        if (brace_depth <= 0) {
          in_annotated_block = 0
          brace_depth = 0
          in_append_call = 0
          append_paren_depth = 0
        }
      }
    }
  ' "$kotlin_file" >>"$violations_file"
done < <(rg --files --glob '*.kt' "${targets[@]}" || true)

# Different regex passes can hit the same source line; normalize to unique
# violations while preserving first-seen order for readable output.
deduped_violations_file="$(mktemp)"
awk '!seen[$0]++' "$violations_file" >"$deduped_violations_file"
mv "$deduped_violations_file" "$violations_file"

if [[ -s "$violations_file" ]]; then
  echo "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
  cat "$violations_file"
  exit 1
fi

echo "PASS: no hardcoded UI text literals found in ${targets[*]}."
