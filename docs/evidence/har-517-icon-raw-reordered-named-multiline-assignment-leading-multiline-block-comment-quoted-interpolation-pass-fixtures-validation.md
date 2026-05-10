# HAR-517 Validation — Icon Raw Reordered Named Multiline Assignment Leading Multiline Block-Comment Quoted Interpolation Pass Fixture

## Scope
Added the symmetric raw quoted-interpolation pass-fixture variant for reordered named `Icon(...)` using multiline assignment with a leading multiline block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized`
  - `        */ """Play "$title"""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: close the raw counterpart matrix gap after HAR-516.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-517-test-check-hardcoded-ui-text-literals-20260510T092140Z.log`
- `docs/evidence/har-517-check-hardcoded-ui-text-literals-20260510T092140Z.log`
