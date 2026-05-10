# HAR-434 Validation — Escaped-Dollar Raw Reordered Named-Arg Trailing Line-Comment Single-Line Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that use trailing `// TODO localize` comments in compact call layouts.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_trailing_line_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgTrailingLineCommentFail.kt`
- Added assertions proving both reordered named-arg paths are flagged:
  - `endIndex = 3, text = """Price \$5""", // TODO localize`
  - `end = 3, text = """Price \$5""", // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-434-test-check-hardcoded-ui-text-literals-20260507T122133Z.log`
- `docs/evidence/har-434-check-hardcoded-ui-text-literals-20260507T122133Z.log`
