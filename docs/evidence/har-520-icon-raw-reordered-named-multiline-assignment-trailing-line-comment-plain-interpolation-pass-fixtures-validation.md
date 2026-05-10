# HAR-520 Validation — Icon Raw Reordered Named Multiline Assignment Trailing Line-Comment Plain Interpolation Pass Fixture

## Scope
Added the missing raw triple-quoted plain-interpolation pass fixture variant for reordered named `Icon(...)` using multiline assignment with a trailing line-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    """Play ${title} for \$5""", // localized`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: close the raw multiline-assignment trailing line-comment gap for plain interpolation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-520-test-check-hardcoded-ui-text-literals-20260510T103207Z.log`
- `docs/evidence/har-520-check-hardcoded-ui-text-literals-20260510T103207Z.log`
