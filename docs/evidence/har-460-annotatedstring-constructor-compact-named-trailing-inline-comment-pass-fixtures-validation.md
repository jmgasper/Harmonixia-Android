# HAR-460 Validation — AnnotatedString Constructor Compact Named Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for compact named `AnnotatedString(text = ...)` constructor raw interpolation with escaped-dollar currency when a trailing inline block comment follows the literal.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" /* localized */))`

This ensures the scanner does not falsely flag interpolated compact named constructor raw strings in trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-460-test-check-hardcoded-ui-text-literals-20260508T154330Z.log`
- `docs/evidence/har-460-check-hardcoded-ui-text-literals-20260508T154330Z.log`
