# HAR-477 Validation — AnnotatedString Constructor Compact Named Escaped Multiline Trailing Block Comment Pass Fixture

Added pass-fixture coverage for compact named `AnnotatedString(text = ...)` escaped-string interpolation with escaped-dollar currency when a trailing block comment spans lines and the call closes on the next line.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(text = "Now ${title} costs \$5" /* localized`
  - `*/`
  - `))`

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-477-test-check-hardcoded-ui-text-literals-20260509T102207Z.log`
- `docs/evidence/har-477-check-hardcoded-ui-text-literals-20260509T102207Z.log`
