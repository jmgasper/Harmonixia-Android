# HAR-485 Validation — Icon Raw Reordered Named Compact Close-Block Indentation Pass Fixture

Added pass-fixture coverage for reordered named `Icon(...)` raw-string interpolation with compact close-block localization comment indentation.

## Changes

- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(`
  - `contentDescription = /* localized` + `*/ """Play ${title} for \$5""",`
  - `imageVector = Icons.Outlined.PlayArrow`
  - `)`

## Validation

- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Logs:

- `docs/evidence/har-485-test-check-hardcoded-ui-text-literals-20260509T161835Z.log`
- `docs/evidence/har-485-check-hardcoded-ui-text-literals-20260509T161835Z.log`
