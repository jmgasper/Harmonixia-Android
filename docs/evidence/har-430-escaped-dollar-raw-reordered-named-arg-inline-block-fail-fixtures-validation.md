# HAR-430 Validation — Escaped-Dollar Raw Reordered Named-Arg Inline-Block Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls with inline block comments directly before the raw literal on the same line.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_inline_block_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgInlineBlockCommentFail.kt`
- Added assertions for emitted snippets:
  - `endIndex = 3, text = /* TODO localize */ """Price \$5""", startIndex = 0`
  - `end = 3, text = /* TODO localize */ """Price \$5""", start = 0`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-430-test-check-hardcoded-ui-text-literals-20260507T074538Z.log`
- `docs/evidence/har-430-check-hardcoded-ui-text-literals-20260507T074538Z.log`
