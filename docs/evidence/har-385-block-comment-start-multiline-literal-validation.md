# HAR-385 Validation - detect block-comment-start multiline literals

Validation window (UTC): 20260505T115523Z - 20260505T115523Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` to keep multiline detection active when start lines include open block-comment tails without same-line closure:
  - direct call starts like `Text( /* TODO ...`
  - assignment starts like `text = /* TODO ...`
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with additional fixtures:
  - pass fixtures for interpolated/value-based counterparts using open block-comment start lines
  - fail fixtures for hardcoded literals under those same patterns

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-385-test-check-hardcoded-ui-text-literals-20260505T115523Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-385-check-hardcoded-ui-text-literals-20260505T115523Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-385-option-regressions-runner-20260505T115523Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-385-compileDebugKotlin-20260505T115523Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-385-smoke-list-avds-20260505T115523Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-385-smoke-install-launch-20260505T115523Z.log`

## Outcome
HAR-385 closes the open-block-comment start-line bypass so hardcoded literals are no longer missed when multiline calls/assignments begin with `/* ...` tails.
