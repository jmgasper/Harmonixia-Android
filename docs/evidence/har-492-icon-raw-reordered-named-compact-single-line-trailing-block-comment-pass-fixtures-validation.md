# HAR-492 Validation — Icon Raw Reordered Named Compact Single-Line Trailing Block Comment Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with compact single-line argument layout where `contentDescription` has a trailing block comment before the comma.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play ${title} for \$5""" /* localized */, imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for compact reordered named raw trailing block-comment layout.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-492-test-check-hardcoded-ui-text-literals-20260509T195518Z.log`
- `docs/evidence/har-492-check-hardcoded-ui-text-literals-20260509T195518Z.log`
