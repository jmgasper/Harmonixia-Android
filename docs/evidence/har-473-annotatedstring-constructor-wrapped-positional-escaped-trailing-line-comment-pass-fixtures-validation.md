# HAR-473 Validation — AnnotatedString Constructor Wrapped Positional Escaped Trailing Line-Comment Pass Fixture

Added pass-fixture coverage for wrapped positional `AnnotatedString(...)` constructor escaped-string interpolation with escaped-dollar currency where the value line has a trailing `//` comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    "Now ${title} costs \$5" // localized`
  - `))`

This ensures the scanner does not falsely flag interpolated wrapped positional escaped constructor strings in trailing line-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-473-test-check-hardcoded-ui-text-literals-20260509T055540Z.log`
- `docs/evidence/har-473-check-hardcoded-ui-text-literals-20260509T055540Z.log`
