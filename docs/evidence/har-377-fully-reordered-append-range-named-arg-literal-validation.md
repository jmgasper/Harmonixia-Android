# HAR-377 Validation - cover fully reordered appendRange named-arg literals

Validation window (UTC): 20260505T045935Z - 20260505T050136Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with fully reordered named-arg non-literal `appendRange(...)` forms:
  - `appendRange(endIndex = title.length, text = title, startIndex = 0)`
  - `appendRange(endIndex = title.length, text = """$title""", startIndex = 0)`
- Extended reordered named-arg fail fixtures for hardcoded literals in the same arg order:
  - `appendRange(endIndex = 3, text = "Now playing", startIndex = 0)`
  - `appendRange(endIndex = 3, text = """Now playing""", startIndex = 0)`
- Kept scanner logic unchanged; this slice hardens regression coverage for argument-order-independent `appendRange(...)` literal detection.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-377-test-check-hardcoded-ui-text-literals-20260505T045935Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-377-check-hardcoded-ui-text-literals-20260505T045935Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-377-option-regressions-runner-20260505T045935Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-377-compileDebugKotlin-20260505T050136Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-377-smoke-list-avds-20260505T050136Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-377-smoke-install-launch-20260505T050136Z.log`

## Outcome
HAR-377 now explicitly validates that reordered named-arg `appendRange(...)` literal detection remains stable across additional argument-order permutations.
