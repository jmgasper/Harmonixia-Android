# HAR-392 Validation - Preserve first-seen scanner violation order

## Scope
- Change: `scripts/check-hardcoded-ui-text-literals.sh`
- Purpose: dedupe scanner violations while preserving first-seen order (replace `sort -u` with stable first-occurrence dedupe via `awk '!seen[$0]++'`).

## Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
- All commands passed.
- Emulator smoke verified app launch on `Medium_Phone` with `topResumedActivity=com.harmonixia.android/.MainActivity`.

## Logs
- `docs/evidence/har-392-test-check-hardcoded-ui-text-literals-20260505T194955Z.log`
- `docs/evidence/har-392-check-hardcoded-ui-text-literals-20260505T194955Z.log`
- `docs/evidence/har-392-option-regressions-runner-20260505T194955Z.log`
- `docs/evidence/har-392-compileDebugKotlin-20260505T194955Z.log`
- `docs/evidence/har-392-smoke-list-avds-20260505T194955Z.log`
- `docs/evidence/har-392-smoke-install-launch-20260505T194955Z.log`
