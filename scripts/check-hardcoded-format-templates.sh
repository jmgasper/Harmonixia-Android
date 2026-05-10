#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

targets=(
  "app/src/main/java"
)

string_format_pattern='String\.format\([^,\n]*,\s*"[^"\n]*%[^"\n]*"'
dot_format_pattern='"[^"\n]*%[^"\n]*"\.format\('

violations_file="$(mktemp)"
trap 'rm -f "$violations_file"' EXIT

rg --no-heading --line-number --color never --glob '*.kt' "$string_format_pattern" "${targets[@]}" >>"$violations_file" || true
rg --no-heading --line-number --color never --glob '*.kt' "$dot_format_pattern" "${targets[@]}" >>"$violations_file" || true

if [[ -s "$violations_file" ]]; then
  echo "FAIL: hardcoded format templates found in Kotlin sources:"
  cat "$violations_file"
  exit 1
fi

echo "PASS: no hardcoded Kotlin format templates found in ${targets[*]}."
