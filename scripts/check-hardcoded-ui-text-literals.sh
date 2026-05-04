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
named_text_pattern='text\s*=\s*"[^"$][^"\n]*"'
content_description_pattern='contentDescription\s*=\s*"[^"$][^"\n]*"'

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

rg --no-heading --line-number --color never --glob '*.kt' "$text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$basic_text_call_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$named_text_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$content_description_pattern" "${targets[@]}" >>"$violations_file" || true

if [[ -s "$violations_file" ]]; then
  echo "FAIL: hardcoded UI text literals found in Kotlin UI sources:"
  cat "$violations_file"
  exit 1
fi

echo "PASS: no hardcoded UI text literals found in ${targets[*]}."
