# HAR-450 Validation — Escaped-Dollar Raw Constructor Positional Inline Block-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for positional `AnnotatedString(...)` constructor calls with inline block comments before literals, and fixed scanner detection for this constructor form.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_positional_inline_block_comment_fail_dir`
  - `EscapedDollarRawConstructorPositionalInlineBlockCommentFail.kt`
- Added assertion proving positional constructor inline block-comment path is flagged:
  - `AnnotatedString(/* TODO localize */ """Price \$5""")`
- Updated `scripts/check-hardcoded-ui-text-literals.sh` regex:
  - `annotated_string_constructor_pattern` now allows optional inline block comments before constructor literals.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-450-test-check-hardcoded-ui-text-literals-20260508T055535Z.log`
- `docs/evidence/har-450-check-hardcoded-ui-text-literals-20260508T055535Z.log`
