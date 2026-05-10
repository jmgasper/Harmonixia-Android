# HAR-423 Validation — Escaped-Dollar Raw-String Annotated Named-Arg Fail Fixtures

## Scope
Added explicit escaped-dollar raw-string fail coverage for annotated named-arg `appendLine/appendRange/append` paths.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `escaped_dollar_raw_named_arg_paths_fail_dir`
  - `EscapedDollarRawNamedArgPathsFail.kt`
- Added assertions for emitted raw-string snippets such as:
  - `appendLine(text = """Price \$5""")`
  - `appendRange(text = """Price \$5""", startIndex = 0, endIndex = 3)`
  - `append(end = 3, text = """Price \$5""", start = 0)`

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
- `docs/evidence/har-423-test-check-hardcoded-ui-text-literals-20260506T234946Z.log`
- `docs/evidence/har-423-check-hardcoded-ui-text-literals-20260506T234946Z.log`
- `docs/evidence/har-423-option-regressions-runner-20260506T234946Z.log`
- `docs/evidence/har-423-compileDebugKotlin-20260506T234946Z.log`
- `docs/evidence/har-423-smoke-list-avds-20260506T234946Z.log`
- `docs/evidence/har-423-smoke-install-launch-20260506T234946Z.log`
