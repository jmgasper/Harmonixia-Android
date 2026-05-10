# HAR-513 Validation — Icon Raw Reordered Named Multiline Assignment Leading Block Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with quoted interpolation in multiline assignment layout and a leading block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized */ """Play "$title"""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation with multiline leading block-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-513-test-check-hardcoded-ui-text-literals-20260510T055506Z.log`
- `docs/evidence/har-513-check-hardcoded-ui-text-literals-20260510T055506Z.log`
