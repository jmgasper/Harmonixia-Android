# HAR-501 Validation — Icon Raw Reordered Named Close-Block Indentation Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation `"""Play ${title}"""` where a leading multiline block comment closes with deeper indentation.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription = /* localized`
  - `            */ """Play ${title}""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with close-block-indentation comment layout.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-501-test-check-hardcoded-ui-text-literals-20260509T232717Z.log`
- `docs/evidence/har-501-check-hardcoded-ui-text-literals-20260509T232717Z.log`
