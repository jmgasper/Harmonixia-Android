# HAR-506 Validation — Icon Escaped Reordered Named Compact Leading Block Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for escaped reordered named `Icon(...)` with compact single-line argument layout and quoted interpolation `"Play \"$title\""` with leading block-comment annotation.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = /* localized */ "Play \"$title\"", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for reordered named escaped quoted interpolation with compact leading block comment.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-506-test-check-hardcoded-ui-text-literals-20260510T024939Z.log`
- `docs/evidence/har-506-check-hardcoded-ui-text-literals-20260510T024939Z.log`
