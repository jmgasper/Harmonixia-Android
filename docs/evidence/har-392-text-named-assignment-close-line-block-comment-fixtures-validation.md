# HAR-392 Validation - text named-assignment close-line block-comment fixtures

Validation window (UTC): 20260505T230826Z - 20260505T230826Z

## Scope
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for multiline `text = /* ...` close-line literals across named-assignment forms:
  - `Text(... text = /* ...` then `*/ "Now playing"`)
  - `BasicText(... text = /* ...` then `*/ """Now playing"""`)
- This slice is fixture hardening only; scanner logic is unchanged.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-392-test-check-hardcoded-ui-text-literals-20260505T230826Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-392-check-hardcoded-ui-text-literals-20260505T230826Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-392-option-regressions-runner-20260505T230826Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-392-compileDebugKotlin-20260505T230826Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-392-smoke-list-avds-20260505T230826Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-392-smoke-install-launch-20260505T230826Z.log`

## Outcome
HAR-392 now explicitly covers named-assignment close-line block-comment literal variants for both `Text` and `BasicText` flows.
