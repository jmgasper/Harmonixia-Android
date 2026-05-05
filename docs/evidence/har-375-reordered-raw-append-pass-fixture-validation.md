# HAR-375 Validation - add reordered raw `append` pass fixture

Validation window (UTC): 20260505T035046Z - 20260505T035233Z

## Scope
- Extended the `pass` fixture in `scripts/test-check-hardcoded-ui-text-literals.sh` with reordered named-arg raw interpolated `append(...)` coverage inside `buildAnnotatedString { ... }`:
  - `append(start = 0, end = title.length, text = """$title""")`
- Scanner matching logic was unchanged in this check-in; this slice adds regression coverage that reordered named-arg raw interpolated append usage remains non-literal and passes.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-375-test-check-hardcoded-ui-text-literals-20260505T035046Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-375-check-hardcoded-ui-text-literals-20260505T035046Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-375-option-regressions-runner-20260505T035046Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-375-compileDebugKotlin-20260505T035233Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-375-smoke-list-avds-20260505T035233Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-375-smoke-install-launch-20260505T035233Z.log`

## Outcome
HAR-375 now has explicit pass-fixture regression coverage for reordered named-arg raw interpolated `append(...)` usage, and scanner/local validation gates remain green.
