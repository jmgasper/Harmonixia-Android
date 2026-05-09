# HAR-479 Validation — AnnotatedString Constructor Compact Positional Escaped Trailing Inline Pass Fixture

Added pass-fixture coverage for compact positional `AnnotatedString(...)` constructor escaped-string interpolation with escaped-dollar currency where the literal has a trailing inline block comment and the call closes on the next line.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("Now ${title} costs \$5" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated compact positional escaped constructor strings in trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-479-test-check-hardcoded-ui-text-literals-20260509T113747Z.log`
- `docs/evidence/har-479-check-hardcoded-ui-text-literals-20260509T113747Z.log`
