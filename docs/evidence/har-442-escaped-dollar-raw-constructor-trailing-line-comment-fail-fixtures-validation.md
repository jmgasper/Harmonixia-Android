# HAR-442 Validation — Escaped-Dollar Raw Constructor Trailing Line-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for `AnnotatedString` constructor calls that use trailing `// TODO localize` comments on positional and named constructor arguments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_constructor_line_comment_fail_dir`
  - `EscapedDollarRawConstructorLineCommentFail.kt`
- Added assertions proving constructor trailing line-comment paths are flagged:
  - `AnnotatedString("""Price \$5""" // TODO localize`
  - `text = """Price \$5""" // TODO localize`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-442-test-check-hardcoded-ui-text-literals-20260507T210052Z.log`
- `docs/evidence/har-442-check-hardcoded-ui-text-literals-20260507T210052Z.log`
