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
escaped_string_literal_pattern='"[^"$][^"\n]*"'
raw_string_literal_pattern='"""[^$\n][^"\n]*"""'
literal_pattern="(${escaped_string_literal_pattern}|${raw_string_literal_pattern})"

text_call_pattern="Text\\(\\s*${literal_pattern}"
basic_text_call_pattern="BasicText\\(\\s*${literal_pattern}"
annotated_string_constructor_pattern="AnnotatedString\\(\\s*${literal_pattern}"
annotated_string_named_text_pattern="AnnotatedString\\([^)]*text\\s*=\\s*${literal_pattern}"
named_text_pattern="text\\s*=\\s*${literal_pattern}"
content_description_pattern="contentDescription\\s*=\\s*${literal_pattern}"
annotated_append_call_start_pattern='append(Line|Range)?[[:space:]]*[(]'
annotated_append_literal_pattern='("[^"$][^"]*"|"""[^$][^"]*""")'

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

rg --no-heading --line-number --color never --glob '*.kt' "$text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$basic_text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_constructor_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$content_description_pattern" "${targets[@]}" >>"$violations_file" || true

while IFS= read -r kotlin_file; do
  awk -v append_call_start_pattern="$annotated_append_call_start_pattern" -v append_literal_pattern="$annotated_append_literal_pattern" '
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

if [[ -s "$violations_file" ]]; then
  echo "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
  cat "$violations_file"
  exit 1
fi

echo "PASS: no hardcoded UI text literals found in ${targets[*]}."
