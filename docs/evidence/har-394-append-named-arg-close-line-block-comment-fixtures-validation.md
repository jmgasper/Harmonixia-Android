# HAR-394 Validation - append named-arg close-line block-comment fixtures

Validation window (UTC): 20260506T001957Z - 20260506T001957Z

## Scope
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for `buildAnnotatedString` `append(...)` named-arg forms where multiline block comments close on the same line as the literal:
  - `append(text = /* ...` then `*/ "Now playing"`)
  - `append(start/end/text = ... text = /* ...` then `*/ """Now playing"""`)
- This slice is fixture hardening only; scanner logic is unchanged.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-394-test-check-hardcoded-ui-text-literals-20260506T001957Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-394-check-hardcoded-ui-text-literals-20260506T001957Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-394-option-regressions-runner-20260506T001957Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-394-compileDebugKotlin-20260506T001957Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-394-smoke-list-avds-20260506T001957Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-394-smoke-install-launch-20260506T001957Z.log`

## Outcome
HAR-394 now explicitly covers close-line block-comment literal variants in `append(...)` named-arg usage within `buildAnnotatedString`.
