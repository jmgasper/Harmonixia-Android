# HAR-396 Validation - appendRange reordered close-line block-comment fixtures

Validation window (UTC): 20260506T012903Z - 20260506T012903Z

## Scope
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for reordered `appendRange(endIndex=..., text=..., startIndex=...)` forms with multiline block comments closing on literal lines:
  - `text = /* ...` then `*/ "Now playing",`
  - `text = /* ...` then `*/ """Now playing""",`
- This slice is fixture coverage only; scanner logic is unchanged.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-396-test-check-hardcoded-ui-text-literals-20260506T012903Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-396-check-hardcoded-ui-text-literals-20260506T012903Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-396-option-regressions-runner-20260506T012903Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-396-compileDebugKotlin-20260506T012903Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-396-smoke-list-avds-20260506T012903Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-396-smoke-install-launch-20260506T012903Z.log`

## Outcome
HAR-396 now explicitly covers reordered `appendRange(...)` close-line block-comment literal variants in `buildAnnotatedString` flows.
