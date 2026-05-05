# HAR-378 Validation — Raw `appendLine(text = ...)` Named-Arg Literal Coverage

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` to cover raw triple-quoted named-arg usage for `appendLine(text = ...)`.
- Added both pass and fail fixtures/assertions:
  - pass: `appendLine(text = """$title""")`
  - fail: `appendLine(text = """Now playing""")`

## Commands Run
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
- All checks passed.
- Emulator smoke install/launch passed on `Medium_Phone` with app resumed (`com.harmonixia.android/.MainActivity`).

## Evidence Logs
- `docs/evidence/har-378-test-check-hardcoded-ui-text-literals-20260505T060646Z.log`
- `docs/evidence/har-378-check-hardcoded-ui-text-literals-20260505T060646Z.log`
- `docs/evidence/har-378-option-regressions-runner-20260505T060646Z.log`
- `docs/evidence/har-378-compileDebugKotlin-20260505T060646Z.log`
- `docs/evidence/har-378-smoke-list-avds-20260505T060646Z.log`
- `docs/evidence/har-378-smoke-install-launch-20260505T060646Z.log`
