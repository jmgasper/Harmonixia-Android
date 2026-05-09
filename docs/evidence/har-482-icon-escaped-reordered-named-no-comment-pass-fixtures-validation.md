# HAR-482 Validation — Icon Escaped Reordered Named No-Comment Pass Fixture

Added pass-fixture coverage for reordered named `Icon(...)` escaped interpolation where `contentDescription` appears before `imageVector` with no trailing localization comment.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(`
  - `contentDescription = "Play ${title} for \$5",`
  - `imageVector = Icons.Outlined.PlayArrow`
  - `)`

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-482-test-check-hardcoded-ui-text-literals-20260509T150120Z.log`
- `docs/evidence/har-482-check-hardcoded-ui-text-literals-20260509T150120Z.log`
