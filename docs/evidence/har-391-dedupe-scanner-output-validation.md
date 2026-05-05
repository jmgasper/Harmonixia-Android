# HAR-391 Validation - deduplicate scanner output lines

Validation window (UTC): 20260505T184249Z - 20260505T184249Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` to `sort -u` collected violations before reporting so overlapping regex passes do not emit duplicate lines.
- Added regression coverage in `scripts/test-check-hardcoded-ui-text-literals.sh`:
  - a dedicated fixture where one source line matches multiple scanners (`AnnotatedString(text = "...")` plus generic `text = ...`), with an assertion that the file appears exactly once in output.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-391-test-check-hardcoded-ui-text-literals-20260505T184249Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-391-check-hardcoded-ui-text-literals-20260505T184249Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-391-option-regressions-runner-20260505T184249Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-391-compileDebugKotlin-20260505T184249Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-391-smoke-list-avds-20260505T184249Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-391-smoke-install-launch-20260505T184249Z.log`

## Outcome
HAR-391 removes duplicate scanner output entries from overlapping pattern matches and locks behavior with explicit regression coverage.
