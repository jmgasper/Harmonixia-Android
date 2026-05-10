# HAR-387 Validation - detect close-line literals after multiline block-comment starts

Validation window (UTC): 20260505T173007Z - 20260505T173007Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` multiline literal matcher to treat block-comment close tokens (`*/`) as valid prefixes before a literal on the same line.
- Added regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for close-line literal forms that were previously missed:
  - `contentDescription = /* ...` then `*/ "Play track"` / `*/ """Play track"""`
  - `Text( /* ...` then `*/ "Now playing"` / `*/ """Now playing"""`

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-test-check-hardcoded-ui-text-literals-20260505T173007Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-check-hardcoded-ui-text-literals-20260505T173007Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-option-regressions-runner-20260505T173007Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-387-compileDebugKotlin-20260505T173007Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-387-smoke-list-avds-20260505T173007Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-387-smoke-install-launch-20260505T173007Z.log`

## Outcome
HAR-387 closes a multiline scanner gap so close-line literals after block-comment starts are flagged consistently for both escaped and raw literal variants.
