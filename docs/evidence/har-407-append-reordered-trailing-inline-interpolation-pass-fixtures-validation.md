# HAR-407 Validation - append reordered trailing-inline interpolation pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass fixtures ensuring trailing inline block-comment handling does not overmatch interpolated strings when named arguments are reordered in `buildAnnotatedString` append APIs.

## Added pass fixtures
- `append(end = title.length, text = "Now \"$title\"" /* localized */, start = 0)`
- `appendRange(endIndex = title.length, text = "Now \"$title\"" /* localized */, startIndex = 0)`
- `appendRange(endIndex = title.length, text = """Now $title""" /* localized */, startIndex = 0)`

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
- `docs/evidence/har-407-test-check-hardcoded-ui-text-literals-20260506T074434Z.log`
- `docs/evidence/har-407-check-hardcoded-ui-text-literals-20260506T074434Z.log`
- `docs/evidence/har-407-option-regressions-runner-20260506T074434Z.log`
- `docs/evidence/har-407-compileDebugKotlin-20260506T074434Z.log`
- `docs/evidence/har-407-smoke-list-avds-20260506T074434Z.log`
- `docs/evidence/har-407-smoke-install-launch-20260506T074434Z.log`
