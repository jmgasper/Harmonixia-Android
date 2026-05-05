# HAR-390 Validation - cover semantics inline block-comment literals

Validation window (UTC): 20260505T173458Z - 20260505T173458Z

## Scope
- Added explicit semantics-path regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` for:
  - hardcoded `contentDescription = /* ... */ "Volume control"`
  - hardcoded `contentDescription = /* ... */ """Volume control"""`
  inside `Modifier.semantics { ... }`.
- Added a pass fixture for non-literal semantics form:
  - `contentDescription = /* localized */ title`
- This slice is fixture hardening only (scanner logic unchanged).

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-390-test-check-hardcoded-ui-text-literals-20260505T173458Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-390-check-hardcoded-ui-text-literals-20260505T173458Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-390-option-regressions-runner-20260505T173458Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-390-compileDebugKotlin-20260505T173458Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-390-smoke-list-avds-20260505T173458Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-390-smoke-install-launch-20260505T173458Z.log`

## Outcome
HAR-390 strengthens regression coverage for semantics `contentDescription` inline block-comment literal forms and verifies non-literal semantics remain accepted.
