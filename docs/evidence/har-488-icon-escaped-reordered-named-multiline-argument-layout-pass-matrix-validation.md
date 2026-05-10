# HAR-488 Validation - Icon Escaped Reordered Named Multiline Argument-Layout Pass Matrix

## Scope
Audited the escaped-string reordered named `Icon(...)` pass matrix for multiline argument layout where `contentDescription =` and its value are split across lines.

## Audit Finding
- Existing pass fixtures already covered multiline reordered named escaped variants for:
  - no comment on value line
  - trailing inline block comment
  - leading inline block comment
  - close-block comment before the value
- Missing before this pass: close-block multiline argument-layout variant with deeper close-token indentation alignment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures by adding:
  - `Icon(contentDescription =\n    /* localized\n                */ "Play ${title} for \$5", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: complete multiline escaped reordered named matrix coverage for close-block indentation layouts and prevent regressions in pass classification.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-488-test-check-hardcoded-ui-text-literals-20260509T184147Z.log`
- `docs/evidence/har-488-check-hardcoded-ui-text-literals-20260509T184147Z.log`
