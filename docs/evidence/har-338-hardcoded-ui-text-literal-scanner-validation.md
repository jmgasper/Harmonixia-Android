# HAR-338 Validation - Hardcoded UI text-literal scanner

Timestamp (UTC): 20260504T125957Z

## Scope
- Added scanner script: `scripts/check-hardcoded-ui-text-literals.sh`.
- Integrated scanner into local validation behavior regressions (`test-local-validation-option-regressions.sh --behavior-only`) and syntax checks.
- Updated option-regressions workflow paths and local-validation docs to include the new scanner path.

## Validation Commands and Results
1. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-338-check-hardcoded-ui-text-literals-20260504T125957Z.log`
2. `./scripts/test-local-validation-option-regressions.sh --behavior-only`
   - Result: PASS (includes hardcoded format-template and hardcoded UI text-literal scanner regressions)
   - Log: `docs/evidence/har-338-option-regressions-behavior-only-20260504T125957Z.log`
3. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-338-compileDebugKotlin-20260504T125957Z.log`
4. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-338-smoke-list-avds-20260504T125957Z.log`
5. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-338-option-regressions-runner-self-test-20260504T125957Z.log`

## Outcome
HAR-338 scanner integration is complete and validated in the local behavior-regression path.
