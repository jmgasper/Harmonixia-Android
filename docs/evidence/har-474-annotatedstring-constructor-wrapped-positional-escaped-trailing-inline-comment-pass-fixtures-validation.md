# HAR-474 Validation — AnnotatedString Constructor Wrapped Positional Escaped Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for wrapped positional `AnnotatedString(...)` constructor escaped-string interpolation with escaped-dollar currency where the value line has a trailing inline block comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    "Now ${title} costs \$5" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated wrapped positional escaped constructor strings in trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-474-test-check-hardcoded-ui-text-literals-20260509T070126Z.log`
- `docs/evidence/har-474-check-hardcoded-ui-text-literals-20260509T070126Z.log`
