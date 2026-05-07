# HAR-428 Validation — Escaped-Dollar Raw Reordered Named-Arg Trailing-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for reordered named-arg `appendRange` and `append` calls with trailing inline comments on raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_reordered_named_arg_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawReorderedNamedArgTrailingInlineCommentFail.kt`
- Added assertions for emitted snippets such as:
  - `endIndex = 3, text = """Price \$5""" /* TODO localize */, startIndex = 0`
  - `end = 3, text = """Price \$5""" /* TODO localize */, start = 0`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `./gradlew --no-daemon :app:compileDebugKotlin`
5. `./scripts/smoke-debug-emulator.sh --list-avds`
6. `./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
All commands passed.

## Evidence Logs
- `docs/evidence/har-428-test-check-hardcoded-ui-text-literals-20260507T052422Z.log`
- `docs/evidence/har-428-check-hardcoded-ui-text-literals-20260507T052422Z.log`
- `docs/evidence/har-428-option-regressions-runner-20260507T052422Z.log`
- `docs/evidence/har-428-compileDebugKotlin-20260507T052422Z.log`
- `docs/evidence/har-428-smoke-list-avds-20260507T052422Z.log`
- `docs/evidence/har-428-smoke-install-launch-20260507T052422Z.log`
