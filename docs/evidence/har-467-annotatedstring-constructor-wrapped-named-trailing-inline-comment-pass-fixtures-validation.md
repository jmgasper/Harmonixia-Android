# HAR-467 Validation — AnnotatedString Constructor Wrapped Named Trailing Inline Comment Pass Fixture

Added pass-fixture coverage for wrapped named-argument `AnnotatedString(text = ...)` constructor raw interpolation with escaped-dollar currency where `text =` is on its own line and the value line has a trailing inline block comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    text =`
  - `        """Now ${title} costs \$5""" /* localized */`
  - `))`

This ensures the scanner does not falsely flag interpolated wrapped named constructor raw strings in trailing inline block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-467-test-check-hardcoded-ui-text-literals-20260508T232511Z.log`
- `docs/evidence/har-467-check-hardcoded-ui-text-literals-20260508T232511Z.log`
