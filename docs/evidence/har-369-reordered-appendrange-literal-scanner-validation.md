# HAR-369 Validation - detect reordered named-arg `appendRange` literals

Validation window (UTC): 20260505T013521Z - 20260505T013521Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` `buildAnnotatedString` append-call matcher to detect string literals anywhere in `append(...)`, `appendLine(...)`, and `appendRange(...)` argument lists.
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for reordered named-arg `appendRange` calls:
  - pass case: `appendRange(startIndex = 0, endIndex = title.length, text = title)`
  - fail cases:
    - `appendRange(startIndex = 0, endIndex = 3, text = "Now playing")`
    - `appendRange(startIndex = 0, endIndex = 3, text = """Now playing""")`

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-test-check-hardcoded-ui-text-literals-20260505T013521Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-check-hardcoded-ui-text-literals-20260505T013521Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-option-regressions-runner-20260505T013521Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-369-compileDebugKotlin-20260505T013521Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-369-smoke-list-avds-20260505T013521Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-369-smoke-install-launch-20260505T013521Z.log`

## Outcome
HAR-369 closes the reordered-named-arg `appendRange` scanner gap and validates behavior with compile and simulator smoke evidence.
