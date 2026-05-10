# HAR-379 Validation — Multiline `appendLine(...)` Named-Arg Literal Coverage

## Scope
- Extended regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` to explicitly cover multiline named-arg `appendLine(...)` forms.
- Added pass fixtures for multiline variable/interpolated usage and fail fixtures for multiline hardcoded literals.

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
- `docs/evidence/har-379-test-check-hardcoded-ui-text-literals-20260505T081544Z.log`
- `docs/evidence/har-379-check-hardcoded-ui-text-literals-20260505T081544Z.log`
- `docs/evidence/har-379-option-regressions-runner-20260505T081544Z.log`
- `docs/evidence/har-379-compileDebugKotlin-20260505T081544Z.log`
- `docs/evidence/har-379-smoke-list-avds-20260505T081544Z.log`
- `docs/evidence/har-379-smoke-install-launch-20260505T081544Z.log`
