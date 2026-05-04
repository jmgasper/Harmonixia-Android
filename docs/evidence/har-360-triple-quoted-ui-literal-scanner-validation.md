# HAR-360 Validation - detect triple-quoted UI text literals

Validation window (UTC): 20260504T195044Z - 20260504T195059Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` to detect hardcoded triple-quoted literals (`"""..."""`) for:
  - `Text(...)`
  - `BasicText(...)`
  - `AnnotatedString(...)` constructor/named `text=`
  - `text = ...` and `contentDescription = ...` assignments
  - `append(...)` / `appendLine(...)` literals inside `buildAnnotatedString { ... }`
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` fixtures to cover:
  - passing triple-quoted interpolation forms (`"""$title"""`)
  - failing hardcoded triple-quoted forms for `BasicText`, `AnnotatedString(text = ...)`, and `appendLine(value = ...)`

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-test-check-hardcoded-ui-text-literals-20260504T195044Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-check-hardcoded-ui-text-literals-20260504T195044Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-option-regressions-runner-20260504T195044Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-360-compileDebugKotlin-20260504T195059Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-360-smoke-list-avds-20260504T195059Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-360-smoke-install-launch-20260504T195059Z.log`

## Outcome
HAR-360 triple-quoted UI literal scanner coverage is implemented and validated.
