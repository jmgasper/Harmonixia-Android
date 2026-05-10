# HAR-415 Validation - mixed interpolation + escaped-dollar pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add pass coverage for strings that combine `${...}` interpolation with escaped-dollar currency text, especially in paths sensitive to named-arg literal scanning.

## Added pass fixtures
- `BasicText(text = AnnotatedString(text = "Now ${title} costs \$5"))`
- `BasicText(text = AnnotatedString(text = "Now ${title} costs \$5" /* localized */))`
- `appendLine(value = "Now ${title} costs \$5")`
- `appendLine(value = "Now ${title} costs \$5" /* localized */)`
- `appendLine(value = /* localized ... */ "Now ${title} costs \$5")`

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
- `docs/evidence/har-415-test-check-hardcoded-ui-text-literals-20260506T170236Z.log`
- `docs/evidence/har-415-check-hardcoded-ui-text-literals-20260506T170236Z.log`
- `docs/evidence/har-415-option-regressions-runner-20260506T170236Z.log`
- `docs/evidence/har-415-compileDebugKotlin-20260506T170236Z.log`
- `docs/evidence/har-415-smoke-list-avds-20260506T170236Z.log`
- `docs/evidence/har-415-smoke-install-launch-20260506T170236Z.log`
