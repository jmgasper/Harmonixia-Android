# HAR-515 Validation — Icon Raw Reordered Named Multiline Assignment Leading Line Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with quoted interpolation in multiline assignment layout and a leading line-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    // localized`
  - `    """Play "$title"""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation with multiline leading line-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-515-test-check-hardcoded-ui-text-literals-20260510T081259Z.log`
- `docs/evidence/har-515-check-hardcoded-ui-text-literals-20260510T081259Z.log`
