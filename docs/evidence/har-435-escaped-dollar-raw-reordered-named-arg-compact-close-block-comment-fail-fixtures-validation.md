# HAR-435 Validation — Escaped-Dollar Raw Reordered Named-Arg Compact Close-Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that use compact call layouts with close-block comments immediately before raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_compact_close_block_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgCompactCloseBlockCommentFail.kt`
- Added assertions proving compact reordered named-arg paths are flagged:
  - `*/ """Price \$5""", startIndex = 0)`
  - `*/ """Price \$5""", start = 0)`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-435-test-check-hardcoded-ui-text-literals-20260507T132556Z.log`
- `docs/evidence/har-435-check-hardcoded-ui-text-literals-20260507T132556Z.log`
