# HAR-449 Validation — Escaped-Dollar Raw Constructor Compact Named Multiline Trailing Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor multiline forms where trailing block comments span multiple lines after the literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_multiline_trailing_block_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedMultilineTrailingBlockCommentFail.kt`
- Added assertion proving compact named multiline constructor trailing block-comment path is flagged:
  - `text = """Price \$5""" /* TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-449-test-check-hardcoded-ui-text-literals-20260508T045004Z.log`
- `docs/evidence/har-449-check-hardcoded-ui-text-literals-20260508T045004Z.log`
