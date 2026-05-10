# HAR-451 Validation — Escaped-Dollar Raw Constructor Positional Multiline Inline Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for positional `AnnotatedString(...)` constructor multiline forms with inline block comments before literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_positional_multiline_inline_block_comment_fail_dir`
  - `EscapedDollarRawConstructorPositionalMultilineInlineBlockCommentFail.kt`
- Added assertion proving positional multiline constructor inline block-comment path is flagged:
  - `/* TODO localize */ """Price \$5"""`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-451-test-check-hardcoded-ui-text-literals-20260508T070050Z.log`
- `docs/evidence/har-451-check-hardcoded-ui-text-literals-20260508T070050Z.log`
