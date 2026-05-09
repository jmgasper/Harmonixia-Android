# HAR-478 Validation — AnnotatedString Constructor Compact Positional Escaped Multiline Trailing Block-Comment Pass Fixture

Added pass-fixture coverage for compact positional `AnnotatedString(...)` constructor escaped-string interpolation with escaped-dollar currency where a multiline trailing block comment follows the literal.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("Now ${title} costs \$5" /* localized`
  - `            */`
  - `))`

This ensures the scanner does not falsely flag interpolated compact positional escaped constructor strings in multiline trailing block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-478-test-check-hardcoded-ui-text-literals-20260509T113155Z.log`
- `docs/evidence/har-478-check-hardcoded-ui-text-literals-20260509T113155Z.log`
