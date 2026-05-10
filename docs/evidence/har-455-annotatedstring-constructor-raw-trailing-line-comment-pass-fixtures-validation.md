# HAR-455 Validation — AnnotatedString Constructor Raw Trailing Line-Comment Pass Fixtures

Added pass-fixture coverage for constructor-form `AnnotatedString(...)` raw-string interpolation with escaped-dollar currency when trailing `//` line comments are present.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("""Now \\${title} costs \\$5""" // localized ... ))`
  - `BasicText(text = AnnotatedString(text = """Now \\${title} costs \\$5""" // localized ... ))`

These ensure the scanner does not flag interpolated constructor raw strings as hardcoded literals in trailing line-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-455-test-check-hardcoded-ui-text-literals-20260508T102029Z.log`
- `docs/evidence/har-455-check-hardcoded-ui-text-literals-20260508T102029Z.log`
