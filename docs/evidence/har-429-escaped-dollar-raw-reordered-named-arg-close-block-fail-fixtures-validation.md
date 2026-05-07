# HAR-429 Validation — Escaped-Dollar Raw Reordered Named-Arg Close-Block Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that use close-block comments immediately before the raw literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_close_block_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgCloseBlockCommentFail.kt`
- Added assertions proving the scanner flags both close-block-comment raw literal occurrences in the reordered named-arg forms.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-429-test-check-hardcoded-ui-text-literals-20260507T063629Z.log`
- `docs/evidence/har-429-check-hardcoded-ui-text-literals-20260507T063629Z.log`
