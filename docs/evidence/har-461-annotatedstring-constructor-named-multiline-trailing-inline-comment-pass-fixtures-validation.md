# HAR-461 Validation — AnnotatedString Constructor Named Multiline Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for multiline named-argument `AnnotatedString(text = ...)` constructor raw interpolation with escaped-dollar currency when a trailing inline block comment follows the literal.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    text = """Now ${title} costs \$5""" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated named constructor raw strings in multiline trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-461-test-check-hardcoded-ui-text-literals-20260508T165606Z.log`
- `docs/evidence/har-461-check-hardcoded-ui-text-literals-20260508T165606Z.log`
