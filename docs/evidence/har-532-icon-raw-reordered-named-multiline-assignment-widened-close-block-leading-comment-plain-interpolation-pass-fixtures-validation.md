# HAR-532 Validation — Icon Raw Reordered Named Multiline Assignment Widened Close-Block Leading Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation (`${title}`) in multiline assignment layout using widened close-block indentation for a leading multiline block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized`
  - `                */ """Play ${title}""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation when multiline block-comment close indentation is widened.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-532-test-check-hardcoded-ui-text-literals-20260510T152018Z.log`
- `docs/evidence/har-532-check-hardcoded-ui-text-literals-20260510T152018Z.log`
