# HAR-395 Validation - AnnotatedString raw interpolation fixture coverage

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add regression coverage ensuring raw interpolation with non-leading `$` remains non-violating in `AnnotatedString` constructor/named-text and `buildAnnotatedString` append paths.

## Added fixtures
- `BasicText(text = AnnotatedString("""Track: $title"""))`
- `BasicText(text = AnnotatedString(text = """Now $title"""))`
- `buildAnnotatedString { append("""Track: $title"""); appendLine(text = """Now $title""") }`

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
- `docs/evidence/har-395-test-check-hardcoded-ui-text-literals-20260505T231228Z.log`
- `docs/evidence/har-395-check-hardcoded-ui-text-literals-20260505T231228Z.log`
- `docs/evidence/har-395-option-regressions-runner-20260505T231228Z.log`
- `docs/evidence/har-395-compileDebugKotlin-20260505T231228Z.log`
- `docs/evidence/har-395-smoke-list-avds-20260505T231228Z.log`
- `docs/evidence/har-395-smoke-install-launch-20260505T231228Z.log`
