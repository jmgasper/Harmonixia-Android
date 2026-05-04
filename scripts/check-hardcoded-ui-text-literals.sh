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
text_call_pattern='Text\(\s*"[^"$][^"\n]*"'
basic_text_call_pattern='BasicText\(\s*"[^"$][^"\n]*"'
annotated_string_constructor_pattern='AnnotatedString\(\s*"[^"$][^"\n]*"'
annotated_string_named_text_pattern='AnnotatedString\([^)]*text\s*=\s*"[^"$][^"\n]*"'
named_text_pattern='text\s*=\s*"[^"$][^"\n]*"'
content_description_pattern='contentDescription\s*=\s*"[^"$][^"\n]*"'

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

rg --no-heading --line-number --color never --glob '*.kt' "$text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$basic_text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_constructor_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$annotated_string_named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$content_description_pattern" "${targets[@]}" >>"$violations_file" || true

while IFS= read -r kotlin_file; do
  awk '
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

    {
      if (in_annotated_block == 1 && $0 ~ /append(Line)?[[:space:]]*\([[:space:]]*"[^"$][^"\n]*"/) {
        printf "%s:%d:%s\n", FILENAME, NR, $0
      }

      if (in_annotated_block == 0 && $0 ~ /buildAnnotatedString[[:space:]]*\{/) {
        in_annotated_block = 1
        brace_depth = 0
        if ($0 ~ /append(Line)?[[:space:]]*\([[:space:]]*"[^"$][^"\n]*"/) {
          printf "%s:%d:%s\n", FILENAME, NR, $0
        }
      }

      if (in_annotated_block == 1) {
        update_brace_depth($0)
        if (brace_depth <= 0) {
          in_annotated_block = 0
          brace_depth = 0
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
