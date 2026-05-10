# HAR-448 Validation — Escaped-Dollar Raw Constructor Compact Named Multiline Inline Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor multiline forms with inline block comments before the literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_multiline_inline_block_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedMultilineInlineBlockCommentFail.kt`
- Added assertion proving compact named multiline constructor inline block-comment path is flagged:
  - `text = /* TODO localize */ """Price \$5"""`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-448-test-check-hardcoded-ui-text-literals-20260508T034326Z.log`
- `docs/evidence/har-448-check-hardcoded-ui-text-literals-20260508T034326Z.log`
