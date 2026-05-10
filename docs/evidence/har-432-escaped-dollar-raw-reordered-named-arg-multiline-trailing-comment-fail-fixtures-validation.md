# HAR-432 Validation — Escaped-Dollar Raw Reordered Named-Arg Multiline Trailing-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls that use trailing inline comments after raw literals in multiline call layouts.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_multiline_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgMultilineTrailingInlineCommentFail.kt`
- Added assertions proving multiline reordered trailing-comment raw literal lines are flagged for both `appendRange` and `append`.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-432-test-check-hardcoded-ui-text-literals-20260507T100508Z.log`
- `docs/evidence/har-432-check-hardcoded-ui-text-literals-20260507T100508Z.log`
