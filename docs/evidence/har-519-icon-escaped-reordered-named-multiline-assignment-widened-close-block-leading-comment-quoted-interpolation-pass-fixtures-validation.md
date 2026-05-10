# HAR-519 Validation — Icon Escaped Reordered Named Multiline Assignment Widened Close-Block Leading Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for escaped reordered named `Icon(...)` with quoted interpolation in multiline assignment layout using widened close-block indentation for a leading multiline block-comment localization marker.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription =`
  - `    /* localized`
  - `                */ "Play \"$title\"",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named escaped quoted interpolation when multiline block-comment close indentation is widened.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-519-test-check-hardcoded-ui-text-literals-20260510T092449Z.log`
- `docs/evidence/har-519-check-hardcoded-ui-text-literals-20260510T092449Z.log`
