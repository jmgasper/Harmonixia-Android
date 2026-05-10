# HAR-526 Validation — Icon Raw Reordered Named Multiline Assignment Widened Close-Block Leading Comment Plain Interpolation Dollar5 Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with `${title} for \$5` plain interpolation in multiline assignment layout using widened close-block indentation for a leading multiline block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized`
  - `                */ """Play ${title} for \$5""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation when multiline block-comment close indentation is widened.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-526-test-check-hardcoded-ui-text-literals-20260510T105146Z.log`
- `docs/evidence/har-526-check-hardcoded-ui-text-literals-20260510T105146Z.log`
