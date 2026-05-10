# HAR-422 Validation — Escaped-Dollar AppendRange Close-Block-Comment Fail Fixtures

## Scope
Added dedicated escaped-dollar fail coverage for `appendRange(...)` calls that use multiline close-block-comment-before-literal forms.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_append_range_close_block_comment_fail_dir`
  - `EscapedDollarAppendRangeCloseBlockCommentFail.kt`
- Added assertions for emitted snippets:
  - `*/ "Price \$5",`
  - plus filename-level failure verification for the dedicated fixture.

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
- `docs/evidence/har-422-test-check-hardcoded-ui-text-literals-20260506T224251Z.log`
- `docs/evidence/har-422-check-hardcoded-ui-text-literals-20260506T224251Z.log`
- `docs/evidence/har-422-option-regressions-runner-20260506T224251Z.log`
- `docs/evidence/har-422-compileDebugKotlin-20260506T224251Z.log`
- `docs/evidence/har-422-smoke-list-avds-20260506T224251Z.log`
- `docs/evidence/har-422-smoke-install-launch-20260506T224251Z.log`
