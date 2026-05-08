# HAR-446 Validation — Escaped-Dollar Raw Constructor Compact Named Trailing Inline-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor calls with trailing inline block comments after the literal.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedTrailingInlineCommentFail.kt`
- Added assertion proving compact named constructor trailing inline-comment path is flagged:
  - `AnnotatedString(text = """Price \$5""" /* TODO localize */)`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-446-test-check-hardcoded-ui-text-literals-20260508T012550Z.log`
- `docs/evidence/har-446-check-hardcoded-ui-text-literals-20260508T012550Z.log`
