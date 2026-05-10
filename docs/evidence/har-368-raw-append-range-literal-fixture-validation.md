# HAR-368 Validation - cover raw `appendRange` literals

Validation window (UTC): 20260505T002000Z - 20260505T002000Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with an explicit failing fixture for raw-string `appendRange` literals inside `buildAnnotatedString { ... }`:
  - `appendRange("""Now playing""", 0, 3)`
  - `appendRange(text = """Now playing""", startIndex = 0, endIndex = 3)`
- Added assertions that scanner output includes both raw positional and raw named-arg `appendRange` literals.
- Scanner implementation was unchanged in this slice; this is regression coverage for behavior already enforced by the scanner.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-368-test-check-hardcoded-ui-text-literals-20260505T002000Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-368-check-hardcoded-ui-text-literals-20260505T002000Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-368-option-regressions-runner-20260505T002000Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-368-compileDebugKotlin-20260505T002000Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-368-smoke-list-avds-20260505T002000Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-368-smoke-install-launch-20260505T002000Z.log`

## Outcome
HAR-368 adds regression coverage for raw `appendRange(...)` literals while preserving scanner pass status and emulator-verified app install/launch behavior.
