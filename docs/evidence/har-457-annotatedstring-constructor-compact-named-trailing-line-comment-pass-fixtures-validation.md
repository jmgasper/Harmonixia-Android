# HAR-457 Validation — AnnotatedString Constructor Compact Named Trailing Line-Comment Pass Fixture

Added pass-fixture coverage for compact named `AnnotatedString(text = ...)` constructor raw interpolation with escaped-dollar currency when a trailing `//` line comment is present.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(text = """Now ${title} costs \$5""" // localized ... ))`

This ensures the scanner does not falsely flag interpolated compact named constructor raw strings in trailing line-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-457-test-check-hardcoded-ui-text-literals-20260508T122924Z.log`
- `docs/evidence/har-457-check-hardcoded-ui-text-literals-20260508T122924Z.log`
