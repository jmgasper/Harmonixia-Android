# HAR-341 Validation - `validate-local --option-tests` success path coverage

Initial timestamp (UTC): 20260504T141816Z  
Continuation timestamp (UTC): 20260504T142130Z

## Scope
- Added a hermetic success-path assertion to `scripts/test-validate-local-options.sh` for `validate-local.sh --option-tests`.
- Introduced a script-path-aware `run_script_expect_exit` helper so the test can execute a temp copied `validate-local.sh` safely.
- Stubbed `test-local-validation-option-regressions.sh` inside a temp directory to verify that `--option-tests` dispatches correctly without recursive wrapper execution.
- Extended the success-path assertion to pin key forwarded markers from `test-local-validation-option-regressions.sh`, including:
  - `Running validate-local option regressions...`
  - `Running hardcoded UI text-literal scanner self-test...`
  - `check-hardcoded-ui-text-literals tests passed.`
  - `All local validation option regressions passed.`

## Validation Commands and Results
1. `bash scripts/test-validate-local-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-341-test-validate-local-options-20260504T142130Z.log`
2. `bash scripts/test-local-validation-option-regressions.sh --behavior-only`
   - Result: PASS
   - Log: `docs/evidence/har-341-option-regressions-behavior-only-20260504T142130Z.log`
3. `bash scripts/validate-local.sh --option-tests`
   - Result: PASS
   - Log: `docs/evidence/har-341-validate-local-option-tests-20260504T142130Z.log`

## Outcome
HAR-341 now covers the `--option-tests` success path and pins output propagation markers from the option-regression runner, including the UI literal scanner self-test marker.

## Next Action
Prepare one atomic HAR-341 check-in containing the test update and evidence artifacts.
