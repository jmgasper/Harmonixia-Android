# HAR-426 Validation — Escaped-Dollar Raw-String Positional Close-Block Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for positional annotated paths with multiline close-block-comment-before-literal forms.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_positional_close_block_comment_fail_dir`
  - `EscapedDollarRawPositionalCloseBlockCommentFail.kt`
- Added assertions for emitted snippets such as:
  - `*/ """Price \$5"""`

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
- `docs/evidence/har-426-test-check-hardcoded-ui-text-literals-20260507T031022Z.log`
- `docs/evidence/har-426-check-hardcoded-ui-text-literals-20260507T031022Z.log`
- `docs/evidence/har-426-option-regressions-runner-20260507T031022Z.log`
- `docs/evidence/har-426-compileDebugKotlin-20260507T031022Z.log`
- `docs/evidence/har-426-smoke-list-avds-20260507T031022Z.log`
- `docs/evidence/har-426-smoke-install-launch-20260507T031022Z.log`
