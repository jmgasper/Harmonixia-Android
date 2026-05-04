# HAR-363 Validation - cover raw `AnnotatedString("""...""")` constructor literals

Validation window (UTC): 20260504T210129Z - 20260504T210220Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with an explicit failing fixture for a hardcoded raw constructor literal:
  - `AnnotatedString("""Now playing""")`
- Added assertions confirming the scanner output includes the fixture file and the exact triple-quoted constructor call.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-363-test-check-hardcoded-ui-text-literals-20260504T210129Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-363-check-hardcoded-ui-text-literals-20260504T210129Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-363-option-regressions-runner-20260504T210220Z.log`

## Outcome
HAR-363 raw annotated-string constructor literal coverage is now explicitly exercised and validated.
