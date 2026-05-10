# HAR-475 Validation — AnnotatedString Constructor Wrapped Positional Escaped Trailing Block-Comment Pass Fixture

Added pass-fixture coverage for wrapped positional `AnnotatedString(...)` constructor escaped-string interpolation with escaped-dollar currency where the value line has a trailing multiline block comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    "Now ${title} costs \$5" /* localized`
  - `        */`
  - `))`

This ensures the scanner does not falsely flag interpolated wrapped positional escaped constructor strings in trailing block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-475-test-check-hardcoded-ui-text-literals-20260509T081003Z.log`
- `docs/evidence/har-475-check-hardcoded-ui-text-literals-20260509T081003Z.log`
