# HAR-387 Validation - detect prefixed block-comment literal lines

Validation window (UTC): 20260505T141000Z - 20260505T141000Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` multiline pending logic so literal matching is evaluated before comment-line skipping.
- Extended multiline literal regex to allow block-comment prefixes on the same line as a literal (for example `/* TODO */ "Now playing"`).
- Added targeted fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for:
  - pass cases using interpolated/value-based prefixed block-comment lines
  - fail cases where hardcoded literals are prefixed by block comments in positional and named-assignment forms

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-test-check-hardcoded-ui-text-literals-20260505T141000Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-check-hardcoded-ui-text-literals-20260505T141000Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-387-option-regressions-runner-20260505T141000Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-387-compileDebugKotlin-20260505T141000Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-387-smoke-list-avds-20260505T141000Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-387-smoke-install-launch-20260505T141000Z.log`

## Outcome
HAR-387 closes the prefixed-block-comment literal bypass so hardcoded UI literals are no longer missed when the literal line starts with block-comment text.
