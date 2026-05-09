# HAR-483 Validation — Icon Escaped Reordered Named Close-Block Indentation Pass Fixtures

Finalized follow-up pass-fixture coverage for escaped reordered named `Icon(...)` close-block indentation variants.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with two additional compact close-block indentation forms:
  - `contentDescription = /* localized` + `*/ "Play ${title} for \$5",` before `imageVector`
  - same pattern with wider indentation before the comment close line.

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-483-test-check-hardcoded-ui-text-literals-20260509T150403Z.log`
- `docs/evidence/har-483-check-hardcoded-ui-text-literals-20260509T150403Z.log`
