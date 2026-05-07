# HAR-437 Validation — Escaped-Dollar Raw Named-Arg Compact Close-Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls in compact call layouts with close-block comments immediately before raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_compact_close_block_comment_fail_dir`
  - `EscapedDollarRawNamedArgCompactCloseBlockCommentFail.kt`
- Added assertions proving compact named-arg paths are flagged:
  - `*/ """Price \$5""")`
  - `*/ """Price \$5""", startIndex = 0, endIndex = 3)`
  - `*/ """Price \$5""", start = 0)`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-437-test-check-hardcoded-ui-text-literals-20260507T153526Z.log`
- `docs/evidence/har-437-check-hardcoded-ui-text-literals-20260507T153526Z.log`
