# HAR-510 Validation — Icon Raw Reordered Named Multiline Assignment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with quoted interpolation `"""Play "$title""""` and multiline assignment layout.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    """Play "$title"""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation in multiline assignment form.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-510-test-check-hardcoded-ui-text-literals-20260510T050554Z.log`
- `docs/evidence/har-510-check-hardcoded-ui-text-literals-20260510T050554Z.log`
