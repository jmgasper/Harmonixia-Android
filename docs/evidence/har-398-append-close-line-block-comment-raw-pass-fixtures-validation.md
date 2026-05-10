# HAR-398 Validation - append close-line block-comment raw pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass fixtures ensuring close-line block-comment handling does not overmatch interpolated raw-string `text` values in `buildAnnotatedString` append APIs.

## Added pass fixtures
- `appendRange(endIndex = title.length, text = /* localized ... */ """$title""", startIndex = 0)`
- `append(end = title.length, text = /* localized ... */ """$title""", start = 0)`
- `appendLine(text = /* localized ... */ """$title""")`

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
- `docs/evidence/har-398-test-check-hardcoded-ui-text-literals-20260506T025047Z.log`
- `docs/evidence/har-398-check-hardcoded-ui-text-literals-20260506T025047Z.log`
- `docs/evidence/har-398-option-regressions-runner-20260506T025047Z.log`
- `docs/evidence/har-398-compileDebugKotlin-20260506T025047Z.log`
- `docs/evidence/har-398-smoke-list-avds-20260506T025047Z.log`
- `docs/evidence/har-398-smoke-install-launch-20260506T025047Z.log`
