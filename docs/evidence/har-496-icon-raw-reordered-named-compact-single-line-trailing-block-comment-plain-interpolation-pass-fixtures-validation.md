# HAR-496 Validation — Icon Raw Reordered Named Compact Single-Line Trailing Block Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with compact single-line argument layout and plain interpolation `"""Play ${title}"""` with a trailing block comment before the comma.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play ${title}""" /* localized */, imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw interpolation with trailing block-comment annotation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-496-test-check-hardcoded-ui-text-literals-20260509T210712Z.log`
- `docs/evidence/har-496-check-hardcoded-ui-text-literals-20260509T210712Z.log`
