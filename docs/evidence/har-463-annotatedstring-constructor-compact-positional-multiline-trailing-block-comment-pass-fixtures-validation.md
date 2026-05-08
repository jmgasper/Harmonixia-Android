# HAR-463 Validation — AnnotatedString Constructor Compact Positional Multiline Trailing Block-Comment Pass Fixture

Added pass-fixture coverage for compact positional `AnnotatedString(...)` constructor raw interpolation with escaped-dollar currency where a multiline trailing block comment follows the literal.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("""Now ${title} costs \$5""" /* localized`
  - `            */
  - `))`

This ensures the scanner does not falsely flag interpolated compact positional constructor raw strings in multiline trailing block-comment layouts.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-463-test-check-hardcoded-ui-text-literals-20260508T190604Z.log`
- `docs/evidence/har-463-check-hardcoded-ui-text-literals-20260508T190604Z.log`
