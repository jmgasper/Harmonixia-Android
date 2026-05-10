# HAR-436 Validation — Escaped-Dollar Raw Named-Arg Trailing Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg `appendLine`, `appendRange`, and `append` calls that use trailing `// TODO localize` comments after raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_trailing_line_comment_fail_dir`
  - `EscapedDollarRawNamedArgTrailingLineCommentFail.kt`
- Added assertions proving named-arg paths are flagged:
  - `value = """Price \$5""" // TODO localize`
  - `text = """Price \$5""", // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-436-test-check-hardcoded-ui-text-literals-20260507T143220Z.log`
- `docs/evidence/har-436-check-hardcoded-ui-text-literals-20260507T143220Z.log`
