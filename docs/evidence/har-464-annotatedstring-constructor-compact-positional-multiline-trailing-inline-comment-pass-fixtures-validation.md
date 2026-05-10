# HAR-464 Validation — AnnotatedString Constructor Compact Positional Multiline Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for compact positional `AnnotatedString(...)` constructor raw interpolation with escaped-dollar currency when a trailing inline block comment follows the literal and the call closes on the next line.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("""Now ${title} costs \$5""" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated compact positional constructor raw strings in multiline trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-464-test-check-hardcoded-ui-text-literals-20260508T201037Z.log`
- `docs/evidence/har-464-check-hardcoded-ui-text-literals-20260508T201037Z.log`
