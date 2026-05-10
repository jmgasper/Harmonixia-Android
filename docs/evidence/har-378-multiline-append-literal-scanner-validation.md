# HAR-378 Validation - detect multiline append-call literals

Validation window (UTC): 20260505T061114Z - 20260505T061114Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` to track `append(...)`, `appendLine(...)`, and `appendRange(...)` calls across multiple lines inside `buildAnnotatedString { ... }` blocks.
- Added multiline pass fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` where reordered named args remain non-literal.
- Added multiline fail fixtures where literals appear on subsequent lines in append-call argument lists:
  - `text = "Now playing"`
  - `text = """Now playing"""`

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-378-test-check-hardcoded-ui-text-literals-20260505T061114Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-378-check-hardcoded-ui-text-literals-20260505T061114Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-378-option-regressions-runner-20260505T061114Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-378-compileDebugKotlin-20260505T061114Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-378-smoke-list-avds-20260505T061114Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-378-smoke-install-launch-20260505T061114Z.log`

## Outcome
HAR-378 closes multiline append-call literal detection gaps while keeping local regression gates and simulator validation green.
