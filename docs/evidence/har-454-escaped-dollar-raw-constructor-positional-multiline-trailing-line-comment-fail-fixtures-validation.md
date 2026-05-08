# HAR-454 Validation — Escaped-Dollar Raw Constructor Positional Multiline Trailing Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for positional `AnnotatedString(...)` constructor multiline forms with trailing line comments after literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_positional_multiline_trailing_line_comment_fail_dir`
  - `EscapedDollarRawConstructorPositionalMultilineTrailingLineCommentFail.kt`
- Added assertion proving positional multiline constructor trailing line-comment path is flagged:
  - `"""Price \$5""" // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-454-test-check-hardcoded-ui-text-literals-20260508T101622Z.log`
- `docs/evidence/har-454-check-hardcoded-ui-text-literals-20260508T101622Z.log`
