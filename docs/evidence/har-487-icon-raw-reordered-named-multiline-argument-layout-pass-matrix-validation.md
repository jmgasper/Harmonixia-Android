# HAR-487 Validation — Icon Raw Reordered Named Multiline Argument-Layout Pass Matrix

## Scope
Audited the raw reordered named `Icon(...)` pass matrix for multiline argument layout where `contentDescription =` and its value are split across lines.

## Audit Finding
- Existing pass fixtures already covered compact reordered named raw variants (no comment, trailing comment, leading inline block, close-block).
- Missing before this pass: multiline argument-assignment variants for reordered named raw `contentDescription`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with multiline reordered named raw `Icon(...)` forms:
  - No comment:
    - `contentDescription =` on one line and `"""Play ${title} for \$5"""` on the next line.
  - Trailing inline block comment on the raw value.
  - Leading inline block comment before the raw value.
  - Close-block comment before the raw value.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-487-test-check-hardcoded-ui-text-literals-20260509T173348Z.log`
- `docs/evidence/har-487-check-hardcoded-ui-text-literals-20260509T173348Z.log`
