# HAR-382 Validation - detect comment-separated multiline UI literals

Validation window (UTC): 20260505T103936Z - 20260505T103936Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` to keep multiline literal detection pending across blank **and comment-only** lines for:
  - multiline positional calls (`Text(` / `BasicText(` / `AnnotatedString(`)
  - multiline named assignments (`text =` / `contentDescription =`)
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` fixtures:
  - pass fixtures for comment-separated interpolated/variable values
  - fail fixtures for comment-separated hardcoded literals

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-382-test-check-hardcoded-ui-text-literals-20260505T103936Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-382-check-hardcoded-ui-text-literals-20260505T103936Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-382-option-regressions-runner-20260505T103936Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-382-compileDebugKotlin-20260505T103936Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-382-smoke-list-avds-20260505T103936Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-382-smoke-install-launch-20260505T103936Z.log`

## Outcome
HAR-382 closes a multiline scanner blind spot by treating comment-only lines as ignorable while pending literal checks, preventing hardcoded literals from slipping through when comments are inserted between assignment/call start and value.
