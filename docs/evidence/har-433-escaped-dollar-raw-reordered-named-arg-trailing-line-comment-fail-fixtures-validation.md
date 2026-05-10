# HAR-433 Validation — Escaped-Dollar Raw Reordered Named-Arg Trailing Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that place trailing `// TODO localize` comments after raw literals in multiline call layouts.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_multiline_trailing_line_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgMultilineTrailingLineCommentFail.kt`
- Added assertions proving both multiline reordered named-arg paths are flagged:
  - `text = """Price \$5""", // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-433-test-check-hardcoded-ui-text-literals-20260507T111411Z.log`
- `docs/evidence/har-433-check-hardcoded-ui-text-literals-20260507T111411Z.log`
