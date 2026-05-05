# HAR-384 Validation - detect commented multiline start-line literals

Validation window (UTC): 20260505T104808Z - 20260505T104808Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` multiline start-line matchers to keep detection active when trailing comments appear on:
  - direct call start lines (for example `Text( // TODO`)
  - named-assignment start lines (for example `text = /* TODO */`)
- Added regression fixture coverage in `scripts/test-check-hardcoded-ui-text-literals.sh`:
  - pass fixtures for interpolated/value-based equivalents using commented start lines
  - fail fixtures for hardcoded literals when start lines include trailing comments

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-384-test-check-hardcoded-ui-text-literals-20260505T104808Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-384-check-hardcoded-ui-text-literals-20260505T104808Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-384-option-regressions-runner-20260505T104808Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-384-compileDebugKotlin-20260505T104808Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-384-smoke-list-avds-20260505T104808Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-384-smoke-install-launch-20260505T104808Z.log`

## Outcome
HAR-384 closes additional multiline bypasses by catching hardcoded literals when `Text(`/`text =` start lines include trailing comments, while preserving non-literal cases and keeping scanner/regression/simulator gates green.
