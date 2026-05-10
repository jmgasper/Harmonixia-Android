# HAR-490 Validation — Deduplicate Escaped Reordered Named Multiline Pass Fixture

## Scope
Removed one duplicated escaped reordered named multiline pass fixture entry in `scripts/test-check-hardcoded-ui-text-literals.sh`.

## Code Change
- Removed duplicate pass fixture block:
  - `Icon(contentDescription = /* localized ... */ "Play ${title} for \$5", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: keep the escaped reordered named fixture matrix intentional and non-redundant while preserving behavior.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-490-test-check-hardcoded-ui-text-literals-20260509T195036Z.log`
- `docs/evidence/har-490-check-hardcoded-ui-text-literals-20260509T195036Z.log`
