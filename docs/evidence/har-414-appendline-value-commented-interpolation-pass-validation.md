# HAR-414 Validation - appendLine(value=...) commented interpolation pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass coverage for `appendLine(value = ...)` braced interpolation when comments are present, so scanner comment-handling changes do not overmatch dynamic strings.

## Added pass fixtures
- `appendLine(value = /* localized ... */ "Now ${title}")`
- `appendLine(value = """Now ${title}""" /* localized */)`

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
- `docs/evidence/har-414-test-check-hardcoded-ui-text-literals-20260506T155441Z.log`
- `docs/evidence/har-414-check-hardcoded-ui-text-literals-20260506T155441Z.log`
- `docs/evidence/har-414-option-regressions-runner-20260506T155441Z.log`
- `docs/evidence/har-414-compileDebugKotlin-20260506T155441Z.log`
- `docs/evidence/har-414-smoke-list-avds-20260506T155441Z.log`
- `docs/evidence/har-414-smoke-install-launch-20260506T155441Z.log`
