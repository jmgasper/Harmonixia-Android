# HAR-439 Validation — Escaped-Dollar Raw Named-Arg Compact Inline-Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls in compact call layouts with inline block comments before raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_compact_inline_block_comment_fail_dir`
  - `EscapedDollarRawNamedArgCompactInlineBlockCommentFail.kt`
- Added assertions proving compact named-arg inline-block paths are flagged:
  - `value = /* TODO localize */ """Price \$5""")`
  - `text = /* TODO localize */ """Price \$5""", startIndex = 0, endIndex = 3)`
  - `text = /* TODO localize */ """Price \$5""", start = 0)`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-439-test-check-hardcoded-ui-text-literals-20260507T174505Z.log`
- `docs/evidence/har-439-check-hardcoded-ui-text-literals-20260507T174505Z.log`
