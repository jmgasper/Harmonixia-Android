# HAR-499 Validation — Icon Raw Reordered Named Trailing Line Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation `"""Play ${title}"""` and a trailing line comment on `contentDescription`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription = """Play ${title}""", // localized`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with trailing line-comment annotation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-499-test-check-hardcoded-ui-text-literals-20260509T221718Z.log`
- `docs/evidence/har-499-check-hardcoded-ui-text-literals-20260509T221718Z.log`
