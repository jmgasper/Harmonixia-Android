# HAR-425 Validation — Escaped-Dollar Raw-String Close-Block Named-Arg Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for annotated named-arg paths with multiline close-block-comment-before-literal forms.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_close_block_comment_fail_dir`
  - `EscapedDollarRawNamedArgCloseBlockCommentFail.kt`
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
- `docs/evidence/har-425-test-check-hardcoded-ui-text-literals-20260507T020349Z.log`
- `docs/evidence/har-425-check-hardcoded-ui-text-literals-20260507T020349Z.log`
- `docs/evidence/har-425-option-regressions-runner-20260507T020349Z.log`
- `docs/evidence/har-425-compileDebugKotlin-20260507T020349Z.log`
- `docs/evidence/har-425-smoke-list-avds-20260507T020349Z.log`
- `docs/evidence/har-425-smoke-install-launch-20260507T020349Z.log`
