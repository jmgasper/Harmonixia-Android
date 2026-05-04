# HAR-342 Validation - validate-local --option-tests failure propagation

Timestamp (UTC): 20260504T152543Z

## Scope
- Extended `scripts/test-validate-local-options.sh` with a hermetic `--option-tests` failure-path case.
- Failure case stubs `test-local-validation-option-regressions.sh` to return exit `23` and verifies:
  - `validate-local.sh --option-tests` exits non-zero with the same code.
  - failure output is surfaced.
  - AGP compile/test/lint gate output is not emitted.

## Validation Commands and Results
1. `./scripts/test-validate-local-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-342-test-validate-local-options-20260504T152543Z.log`
2. `./scripts/validate-local.sh --option-tests`
   - Result: PASS
   - Log: `docs/evidence/har-342-validate-local-option-tests-20260504T152543Z.log`
3. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-342-option-regressions-dry-run-20260504T152543Z.log`

## Outcome
HAR-342 option-tests failure-path coverage is complete and validated.
