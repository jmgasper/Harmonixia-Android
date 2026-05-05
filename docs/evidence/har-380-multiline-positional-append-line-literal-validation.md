# HAR-380 Validation — Multiline Positional `appendLine(...)` Literal Coverage

## Scope
- Expanded regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` to explicitly cover multiline positional `appendLine(...)` usage in `buildAnnotatedString`.
- Added both pass fixtures (variable/interpolated) and fail fixtures (hardcoded literals).

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
- `docs/evidence/har-380-test-check-hardcoded-ui-text-literals-20260505T092451Z.log`
- `docs/evidence/har-380-check-hardcoded-ui-text-literals-20260505T092451Z.log`
- `docs/evidence/har-380-option-regressions-runner-20260505T092451Z.log`
- `docs/evidence/har-380-compileDebugKotlin-20260505T092451Z.log`
- `docs/evidence/har-380-smoke-list-avds-20260505T092451Z.log`
- `docs/evidence/har-380-smoke-install-launch-20260505T092451Z.log`
