# HAR-514 Validation — Icon Escaped Reordered Named Multiline Assignment Leading Line-Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for escaped quoted interpolation in reordered named `Icon(...)` with multiline assignment layout and a leading line-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    // localized`
  - `    "Play \"$title\"",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named escaped quoted interpolation with multiline leading line-comment formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-514-test-check-hardcoded-ui-text-literals-20260510T081032Z.log`
- `docs/evidence/har-514-check-hardcoded-ui-text-literals-20260510T081032Z.log`
