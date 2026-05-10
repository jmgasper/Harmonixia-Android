# HAR-393 Validation - appendLine named-arg close-line block-comment fixtures

Validation window (UTC): 20260506T001541Z - 20260506T001541Z

## Scope
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for `buildAnnotatedString` `appendLine` named-argument forms where a multiline block comment closes on the literal line:
  - `appendLine(text = /* ...` then `*/ "Now playing"`)
  - `appendLine(value = /* ...` then `*/ """Now playing"""`)
- This slice is fixture coverage only; scanner logic is unchanged.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-393-test-check-hardcoded-ui-text-literals-20260506T001541Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-393-check-hardcoded-ui-text-literals-20260506T001541Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-393-option-regressions-runner-20260506T001541Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-393-compileDebugKotlin-20260506T001541Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-393-smoke-list-avds-20260506T001541Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-393-smoke-install-launch-20260506T001541Z.log`

## Outcome
HAR-393 now explicitly covers close-line block-comment literal forms for `appendLine` named-argument usage inside `buildAnnotatedString` blocks.
