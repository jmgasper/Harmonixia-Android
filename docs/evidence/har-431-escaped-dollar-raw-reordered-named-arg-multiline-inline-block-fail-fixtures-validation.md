# HAR-431 Validation — Escaped-Dollar Raw Reordered Named-Arg Multiline Inline-Block Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that use inline block comments before raw literals in multiline call layouts.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_multiline_inline_block_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgMultilineInlineBlockCommentFail.kt`
- Added assertions proving multiline reordered inline-block raw literal lines are flagged for both `appendRange` and `append`.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-431-test-check-hardcoded-ui-text-literals-20260507T085713Z.log`
- `docs/evidence/har-431-check-hardcoded-ui-text-literals-20260507T085713Z.log`
