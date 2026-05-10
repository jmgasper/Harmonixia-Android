# HAR-452 Validation — Escaped-Dollar Raw Constructor Positional Multiline Trailing Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for positional `AnnotatedString(...)` constructor multiline forms where trailing block comments span multiple lines after literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_positional_multiline_trailing_block_comment_fail_dir`
  - `EscapedDollarRawConstructorPositionalMultilineTrailingBlockCommentFail.kt`
- Added assertion proving positional multiline constructor trailing block-comment path is flagged:
  - `"""Price \$5""" /* TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-452-test-check-hardcoded-ui-text-literals-20260508T080551Z.log`
- `docs/evidence/har-452-check-hardcoded-ui-text-literals-20260508T080551Z.log`
