# HAR-438 Validation — Escaped-Dollar Raw Named-Arg Inline-Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls that place inline block comments immediately before raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_inline_block_comment_fail_dir`
  - `EscapedDollarRawNamedArgInlineBlockCommentFail.kt`
- Added assertions proving named-arg inline-block paths are flagged:
  - `value = /* TODO localize */ """Price \$5"""`
  - `text = /* TODO localize */ """Price \$5""",`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-438-test-check-hardcoded-ui-text-literals-20260507T164151Z.log`
- `docs/evidence/har-438-check-hardcoded-ui-text-literals-20260507T164151Z.log`
