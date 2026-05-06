# HAR-416 Validation - escaped-dollar appendLine(value=...) comment fail fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Extend escaped-dollar failure coverage for `appendLine(value = ...)` to include inline and close-line block-comment forms.

## Added fail fixtures
In `EscapedDollarNamedArgPathsFail.kt`:
- `appendLine(value = /* TODO localize */ "Price \$5")`
- `appendLine(value = /* TODO localize ... */ "Price \$5")`

## Added assertions
- Output must include:
  - `value = /* TODO localize */ "Price \\$5"`
  - `*/ "Price \\$5"`

## Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
- All commands passed.
- Emulator smoke verified launch on `Medium_Phone` with `topResumedActivity=com.harmonixia.android/.MainActivity`.

## Logs
- `docs/evidence/har-416-test-check-hardcoded-ui-text-literals-20260506T181527Z.log`
- `docs/evidence/har-416-check-hardcoded-ui-text-literals-20260506T181527Z.log`
- `docs/evidence/har-416-option-regressions-runner-20260506T181527Z.log`
- `docs/evidence/har-416-compileDebugKotlin-20260506T181527Z.log`
- `docs/evidence/har-416-smoke-list-avds-20260506T181527Z.log`
- `docs/evidence/har-416-smoke-install-launch-20260506T181527Z.log`
