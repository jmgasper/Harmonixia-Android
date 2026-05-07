# HAR-444 Validation — Escaped-Dollar Raw Constructor Compact Named Inline Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor calls that place an inline block comment before the hardcoded literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_inline_block_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedInlineBlockCommentFail.kt`
- Added assertion proving compact named constructor inline block-comment path is flagged:
  - `AnnotatedString(text = /* TODO localize */ """Price \$5""")`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-444-test-check-hardcoded-ui-text-literals-20260507T231419Z.log`
- `docs/evidence/har-444-check-hardcoded-ui-text-literals-20260507T231419Z.log`
