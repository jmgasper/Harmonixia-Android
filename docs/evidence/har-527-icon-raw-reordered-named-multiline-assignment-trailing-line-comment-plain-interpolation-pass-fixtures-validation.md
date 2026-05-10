# HAR-527 Validation — Icon Raw Reordered Named Multiline Assignment Trailing Line Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation (`${title}`) in multiline assignment layout and a trailing line-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    """Play ${title}""", // localized`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with multiline trailing line-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-527-test-check-hardcoded-ui-text-literals-20260510T130124Z.log`
- `docs/evidence/har-527-check-hardcoded-ui-text-literals-20260510T130124Z.log`
