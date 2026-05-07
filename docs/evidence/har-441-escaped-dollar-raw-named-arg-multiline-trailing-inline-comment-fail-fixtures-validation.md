# HAR-441 Validation — Escaped-Dollar Raw Named-Arg Multiline Trailing-Inline-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls in multiline call layouts with trailing inline block comments after raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_multiline_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawNamedArgMultilineTrailingInlineCommentFail.kt`
- Added assertions proving multiline trailing-inline-comment paths are flagged:
  - `value = """Price \$5""" /* TODO localize */`
  - `text = """Price \$5""" /* TODO localize */,`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-441-test-check-hardcoded-ui-text-literals-20260507T195704Z.log`
- `docs/evidence/har-441-check-hardcoded-ui-text-literals-20260507T195704Z.log`
