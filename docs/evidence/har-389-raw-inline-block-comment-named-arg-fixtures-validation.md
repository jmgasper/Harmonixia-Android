# HAR-389 Validation - cover raw inline block-comment named-arg literals

Validation window (UTC): 20260505T162414Z - 20260505T162414Z

## Scope
- Added explicit regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for raw literal forms with inline block comments between named-arg `=` and the value:
  - `Text(text = /* ... */ """Now playing""")`
  - `Icon(... contentDescription = /* ... */ """Play track""")`
  - `AnnotatedString(text = /* ... */ """Now playing""")`
- This slice hardens test coverage for the scanner behavior introduced in HAR-388 (no additional scanner logic changes).

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-389-test-check-hardcoded-ui-text-literals-20260505T162414Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-389-check-hardcoded-ui-text-literals-20260505T162414Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-389-option-regressions-runner-20260505T162414Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-389-compileDebugKotlin-20260505T162414Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-389-smoke-list-avds-20260505T162414Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-389-smoke-install-launch-20260505T162414Z.log`

## Outcome
HAR-389 strengthens scanner regression coverage for raw inline block-comment named-arg literals across text, content-description, and annotated-string paths.
