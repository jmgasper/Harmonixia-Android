# HAR-440 Validation — Escaped-Dollar Raw Named-Arg Compact Trailing Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls in compact call layouts with trailing `// TODO localize` comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_compact_trailing_line_comment_fail_dir`
  - `EscapedDollarRawNamedArgCompactTrailingLineCommentFail.kt`
- Added assertions proving compact trailing line-comment paths are flagged:
  - `value = """Price \$5""" // TODO localize`
  - `text = """Price \$5""", // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-440-test-check-hardcoded-ui-text-literals-20260507T185057Z.log`
- `docs/evidence/har-440-check-hardcoded-ui-text-literals-20260507T185057Z.log`
