# HAR-383 Validation - detect inline-comment multiline UI literals

Validation window (UTC): 20260505T104450Z - 20260505T104450Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` multiline literal matcher to allow trailing inline comments after the literal line (e.g. `"Now playing" // TODO localize`) while still flagging hardcoded values.
- Added regression fixture coverage in `scripts/test-check-hardcoded-ui-text-literals.sh`:
  - pass fixtures for interpolated multiline literals with trailing inline comments
  - fail fixtures for hardcoded multiline literals with trailing inline comments in both positional and named-assignment forms
- Kept the comment-separated multiline detection introduced in HAR-382, and removed awk regex warnings from the inline-comment extension.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-383-test-check-hardcoded-ui-text-literals-20260505T104450Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-383-check-hardcoded-ui-text-literals-20260505T104450Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-383-option-regressions-runner-20260505T104450Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-383-compileDebugKotlin-20260505T104450Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-383-smoke-list-avds-20260505T104450Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-383-smoke-install-launch-20260505T104450Z.log`

## Outcome
HAR-383 closes another multiline detection bypass by catching hardcoded literals that include trailing inline comments, while preserving non-literal interpolated cases and keeping scanner output warning-free.
