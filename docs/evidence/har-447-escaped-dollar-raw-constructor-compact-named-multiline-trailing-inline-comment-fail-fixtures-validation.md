# HAR-447 Validation — Escaped-Dollar Raw Constructor Compact Named Multiline Trailing Inline-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor multiline forms with trailing inline block comments after the literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_multiline_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedMultilineTrailingInlineCommentFail.kt`
- Added assertion proving compact named multiline constructor trailing inline-comment path is flagged:
  - `text = """Price \$5""" /* TODO localize */`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-447-test-check-hardcoded-ui-text-literals-20260508T023818Z.log`
- `docs/evidence/har-447-check-hardcoded-ui-text-literals-20260508T023818Z.log`
