# HAR-424 Validation — Escaped-Dollar Raw-String Positional Fail Fixtures

## Scope
Added explicit escaped-dollar raw-string fail coverage for positional `appendLine/appendRange/append` paths in `buildAnnotatedString`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_positional_append_paths_fail_dir`
  - `EscapedDollarRawPositionalAppendPathsFail.kt`
- Added assertions for emitted raw-string positional snippets such as:
  - `appendLine("""Price \$5""")`
  - `append("""Price \$5""" /* TODO localize */)`
  - `appendRange(/* TODO localize */ """Price \$5""", 0, 3)`

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
- `docs/evidence/har-424-test-check-hardcoded-ui-text-literals-20260507T005704Z.log`
- `docs/evidence/har-424-check-hardcoded-ui-text-literals-20260507T005704Z.log`
- `docs/evidence/har-424-option-regressions-runner-20260507T005704Z.log`
- `docs/evidence/har-424-compileDebugKotlin-20260507T005704Z.log`
- `docs/evidence/har-424-smoke-list-avds-20260507T005704Z.log`
- `docs/evidence/har-424-smoke-install-launch-20260507T005704Z.log`
