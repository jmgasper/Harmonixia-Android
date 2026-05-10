# HAR-514 Validation — Icon Raw Reordered Named Multiline Assignment Trailing Line Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with quoted interpolation in multiline assignment layout and a trailing line-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    """Play "$title"""", // localized`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation with multiline trailing line-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-514-test-check-hardcoded-ui-text-literals-20260510T065856Z.log`
- `docs/evidence/har-514-check-hardcoded-ui-text-literals-20260510T065856Z.log`
