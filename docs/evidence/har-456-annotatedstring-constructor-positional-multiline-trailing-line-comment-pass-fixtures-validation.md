# HAR-456 Validation — AnnotatedString Constructor Positional Multiline Trailing Line-Comment Pass Fixture

Added pass-fixture coverage for positional multiline `AnnotatedString(...)` constructor raw interpolation with escaped-dollar currency when a trailing `//` line comment is present.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(`
  - `    """Now ${title} costs \$5""" // localized`
  - `))`

This ensures the scanner does not falsely flag interpolated constructor raw strings in multiline positional trailing-line-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-456-test-check-hardcoded-ui-text-literals-20260508T112503Z.log`
- `docs/evidence/har-456-check-hardcoded-ui-text-literals-20260508T112503Z.log`
