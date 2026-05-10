# HAR-379 Validation - detect multiline positional call literals

Validation window (UTC): 20260505T081949Z - 20260505T081949Z

## Scope
- Added multiline positional-call literal detection in `scripts/check-hardcoded-ui-text-literals.sh` for call-start lines ending with `Text(`, `BasicText(`, or `AnnotatedString(` where the next non-empty line is a hardcoded string literal.
- Added pass fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for multiline interpolated positional usage.
- Added fail fixtures for multiline positional hardcoded literals in `Text`, `BasicText`, and `AnnotatedString` constructor call sites.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-379-test-check-hardcoded-ui-text-literals-20260505T081949Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-379-check-hardcoded-ui-text-literals-20260505T081949Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-379-option-regressions-runner-20260505T081949Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-379-compileDebugKotlin-20260505T081949Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-379-smoke-list-avds-20260505T081949Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-379-smoke-install-launch-20260505T081949Z.log`

## Outcome
HAR-379 closes multiline positional literal gaps for key Compose text APIs while preserving scanner/regression correctness and simulator-verified runtime behavior.
