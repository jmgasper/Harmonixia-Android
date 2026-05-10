# HAR-469 Validation — AnnotatedString Constructor Wrapped Positional Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for wrapped positional `AnnotatedString(...)` constructor raw interpolation with escaped-dollar currency where the literal line has a trailing inline block comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    """Now ${title} costs \$5""" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated wrapped positional constructor raw strings in trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-469-test-check-hardcoded-ui-text-literals-20260509T013456Z.log`
- `docs/evidence/har-469-check-hardcoded-ui-text-literals-20260509T013456Z.log`
