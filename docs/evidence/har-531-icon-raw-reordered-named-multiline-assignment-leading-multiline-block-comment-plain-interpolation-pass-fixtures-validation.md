# HAR-531 Validation — Icon Raw Reordered Named Multiline Assignment Leading Multiline Block Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation (`${title}`) in multiline assignment layout and a leading multiline block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized`
  - `        */ """Play ${title}""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with multiline leading block-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-531-test-check-hardcoded-ui-text-literals-20260510T151529Z.log`
- `docs/evidence/har-531-check-hardcoded-ui-text-literals-20260510T151529Z.log`

## Heartbeat Re-Validation (2026-05-11)
- Re-ran both scanner commands on the current HAR-531 checkout to confirm this fixture still behaves as a pass case after subsequent fixture-matrix updates.
- Result: both commands passed.
- Additional logs:
  - `docs/evidence/har-531-test-check-hardcoded-ui-text-literals-20260510T151706Z.log`
  - `docs/evidence/har-531-check-hardcoded-ui-text-literals-20260510T151706Z.log`
