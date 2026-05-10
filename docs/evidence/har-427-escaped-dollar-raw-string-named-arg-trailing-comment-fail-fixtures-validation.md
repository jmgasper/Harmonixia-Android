# HAR-427 Validation — Escaped-Dollar Raw-String Named-Arg Trailing-Comment Fail Fixtures

## Scope
Added escaped-dollar raw-string fail coverage for named-arg annotated paths with trailing inline comments on raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_trailing_inline_comment_fail_dir`
  - `EscapedDollarRawNamedArgTrailingInlineCommentFail.kt`
- Added assertions for emitted snippets such as:
  - `value = """Price \$5""" /* TODO localize */`
  - `text = """Price \$5""" /* TODO localize */`

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
- `docs/evidence/har-427-test-check-hardcoded-ui-text-literals-20260507T041735Z.log`
- `docs/evidence/har-427-check-hardcoded-ui-text-literals-20260507T041735Z.log`
- `docs/evidence/har-427-option-regressions-runner-20260507T041735Z.log`
- `docs/evidence/har-427-compileDebugKotlin-20260507T041735Z.log`
- `docs/evidence/har-427-smoke-list-avds-20260507T041735Z.log`
- `docs/evidence/har-427-smoke-install-launch-20260507T041735Z.log`
