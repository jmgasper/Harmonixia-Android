# HAR-445 Validation — Escaped-Dollar Raw Constructor Compact Named Close Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor calls where the hardcoded literal starts after a block-comment close line.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_close_block_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedCloseBlockCommentFail.kt`
- Added assertion proving compact named constructor close block-comment path is flagged:
  - `*/ """Price \$5"""`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-445-test-check-hardcoded-ui-text-literals-20260508T002052Z.log`
- `docs/evidence/har-445-check-hardcoded-ui-text-literals-20260508T002052Z.log`
