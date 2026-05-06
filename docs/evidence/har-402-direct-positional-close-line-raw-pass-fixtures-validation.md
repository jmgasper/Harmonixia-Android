# HAR-402 Validation - direct positional close-line raw pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass fixtures to ensure close-line block-comment handling does not overmatch interpolated raw-string values in direct positional and `AnnotatedString(...)` constructor usage.

## Added pass fixtures
- `Text( /* localized ... */ """$title""" )`
- `BasicText( /* localized ... */ """$title""" )`
- `BasicText(text = AnnotatedString( /* localized ... */ """$title""" ))`

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
- `docs/evidence/har-402-test-check-hardcoded-ui-text-literals-20260506T040439Z.log`
- `docs/evidence/har-402-check-hardcoded-ui-text-literals-20260506T040439Z.log`
- `docs/evidence/har-402-option-regressions-runner-20260506T040439Z.log`
- `docs/evidence/har-402-compileDebugKotlin-20260506T040439Z.log`
- `docs/evidence/har-402-smoke-list-avds-20260506T040439Z.log`
- `docs/evidence/har-402-smoke-install-launch-20260506T040439Z.log`
