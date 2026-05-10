# HAR-530 Validation — Icon Raw Reordered Named Multiline Assignment Leading Single-Line Block Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation (`${title}`) in multiline assignment layout and a leading single-line block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized */ """Play ${title}""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with multiline leading single-line block-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-530-test-check-hardcoded-ui-text-literals-20260510T141002Z.log`
- `docs/evidence/har-530-check-hardcoded-ui-text-literals-20260510T141002Z.log`
