# HAR-409 Validation - braced interpolation pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add explicit pass coverage for braced Kotlin interpolation (`${...}`) so scanner changes do not regress and incorrectly flag dynamic UI strings.

## Added pass fixtures
- Direct/named UI text paths:
  - `Text(text = "Now ${title}")`
  - `Text(text = "Now ${title.uppercase()}")`
  - `Text(text = """Now ${title}""")`
  - `BasicText(text = "Track: ${title}")`
  - `BasicText(text = """Track: ${title}""")`
  - `Icon(..., contentDescription = "Play ${title}")`
- `buildAnnotatedString` append paths:
  - `appendLine(text = "Now ${title}")`
  - `appendLine(text = """Now ${title}""")`

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
- `docs/evidence/har-409-test-check-hardcoded-ui-text-literals-20260506T090843Z.log`
- `docs/evidence/har-409-check-hardcoded-ui-text-literals-20260506T090843Z.log`
- `docs/evidence/har-409-option-regressions-runner-20260506T090843Z.log`
- `docs/evidence/har-409-compileDebugKotlin-20260506T090843Z.log`
- `docs/evidence/har-409-smoke-list-avds-20260506T090843Z.log`
- `docs/evidence/har-409-smoke-install-launch-20260506T090843Z.log`
