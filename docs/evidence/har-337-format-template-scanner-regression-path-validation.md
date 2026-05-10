# HAR-337 Validation - Integrate format-template scanner into regression path

Timestamp (UTC): 20260504T115244Z

## Scope
- Integrated `scripts/check-hardcoded-format-templates.sh` into the local validation regression runner:
  - Added shell syntax coverage in `scripts/test-local-validation-option-regressions.sh --syntax-only`.
  - Added runtime execution in behavioral/default regression paths.
- Updated runner self-test assertions to enforce scanner presence in behavioral/default modes and absence in syntax-only/dry-run modes.
- Updated option regression workflow path filters so scanner-script edits trigger CI.
- Updated local validation workflow documentation to reflect scanner coverage in the umbrella regression path.

## Validation Commands and Results
1. `scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS (`local validation option regression runner tests passed.`)
   - Log: `docs/evidence/har-337-local-validation-option-regressions-runner-20260504T115226Z.log`
2. `scripts/test-local-validation-option-regressions.sh --behavior-only`
   - Result: PASS (includes `Running hardcoded format-template scanner regression...` and scanner PASS output)
   - Log: `docs/evidence/har-337-behavior-only-regressions-20260504T115244Z.log`
3. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-337-smoke-list-avds-20260504T115454Z.log`
4. `scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`)
   - Log: `docs/evidence/har-337-smoke-install-launch-20260504T115454Z.log`

## Outcome
HAR-337 regression-path integration is complete: the hardcoded format-template scanner now runs as part of the local behavioral regression suite used by both direct invocation and `validate-local.sh --option-tests` wrapper flows.
