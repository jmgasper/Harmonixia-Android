# HAR-380 Validation - detect multiline assignment literals

Validation window (UTC): 20260505T092946Z - 20260505T092946Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` to detect multiline assignment literals for:
  - `text =` followed by a hardcoded literal on a subsequent non-empty line
  - `contentDescription =` followed by a hardcoded literal on a subsequent non-empty line
- Added pass fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for multiline variable assignments.
- Added fail fixtures for multiline literal assignments in `Text(...)` and `Icon(...)` call sites.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-380-test-check-hardcoded-ui-text-literals-20260505T092946Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-380-check-hardcoded-ui-text-literals-20260505T092946Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-380-option-regressions-runner-20260505T092946Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-380-compileDebugKotlin-20260505T092946Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-380-smoke-list-avds-20260505T092946Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-380-smoke-install-launch-20260505T092946Z.log`

## Outcome
HAR-380 closes multiline assignment literal gaps for `text` and `contentDescription` while preserving scanner/regression gates and simulator-verified behavior.
