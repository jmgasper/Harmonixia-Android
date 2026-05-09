# HAR-476 Validation — AnnotatedString Constructor Compact Named Escaped Multiline Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for compact named `AnnotatedString(text = ...)` constructor escaped-string interpolation with escaped-dollar currency where a trailing inline block comment follows the literal and the call closes on the next line.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(text = "Now ${title} costs \$5" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated compact named escaped constructor strings in multiline trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-476-test-check-hardcoded-ui-text-literals-20260509T091520Z.log`
- `docs/evidence/har-476-check-hardcoded-ui-text-literals-20260509T091520Z.log`
