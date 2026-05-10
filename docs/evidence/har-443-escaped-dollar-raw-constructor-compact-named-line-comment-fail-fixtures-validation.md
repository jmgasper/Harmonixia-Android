# HAR-443 Validation — Escaped-Dollar Raw Constructor Compact Named Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for compact `AnnotatedString(text = ...)` constructor calls that use trailing `// TODO localize` comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_compact_named_line_comment_fail_dir`
  - `EscapedDollarRawConstructorCompactNamedLineCommentFail.kt`
- Added assertion proving compact named constructor line-comment path is flagged:
  - `AnnotatedString(text = """Price \$5""" // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-443-test-check-hardcoded-ui-text-literals-20260507T220713Z.log`
- `docs/evidence/har-443-check-hardcoded-ui-text-literals-20260507T220713Z.log`
