# HAR-381 Validation - cover multiline raw contentDescription literals

Validation window (UTC): 20260505T103621Z - 20260505T103621Z

## Scope
- Added pass fixture coverage in `scripts/test-check-hardcoded-ui-text-literals.sh` for multiline raw interpolated `contentDescription` assignment.
- Added fail fixture coverage for multiline hardcoded raw `contentDescription` assignment:
  - `contentDescription =` followed by `"""Play track"""`.
- Scanner logic was unchanged in this slice; this check-in expands regression coverage for multiline assignment detection behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-381-test-check-hardcoded-ui-text-literals-20260505T103621Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-381-check-hardcoded-ui-text-literals-20260505T103621Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-381-option-regressions-runner-20260505T103621Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-381-compileDebugKotlin-20260505T103621Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-381-smoke-list-avds-20260505T103621Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-381-smoke-install-launch-20260505T103621Z.log`

## Outcome
HAR-381 adds explicit regression coverage for multiline raw `contentDescription` literals while keeping scanner, compile, and simulator gates green.
