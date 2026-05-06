# HAR-399 Validation - direct-path close-line raw pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass fixtures ensuring close-line block-comment handling does not overmatch interpolated raw-string values in direct/named UI text paths.

## Added pass fixtures
- `Text(text = /* localized ... */ """$title""")`
- `BasicText(text = AnnotatedString(text = /* localized ... */ """$title"""))`
- `Icon(... contentDescription = /* localized ... */ """$title""")`

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
- `docs/evidence/har-399-test-check-hardcoded-ui-text-literals-20260506T035724Z.log`
- `docs/evidence/har-399-check-hardcoded-ui-text-literals-20260506T035724Z.log`
- `docs/evidence/har-399-option-regressions-runner-20260506T035724Z.log`
- `docs/evidence/har-399-compileDebugKotlin-20260506T035724Z.log`
- `docs/evidence/har-399-smoke-list-avds-20260506T035724Z.log`
- `docs/evidence/har-399-smoke-install-launch-20260506T035724Z.log`
