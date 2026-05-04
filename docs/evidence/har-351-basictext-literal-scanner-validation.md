# HAR-351 Validation - detect `BasicText` hardcoded literals

Timestamp (UTC): 20260504T181326Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` to detect `BasicText("...")` literals.
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with a failing `BasicText` fixture and passing non-literal `BasicText(text = title)` usage.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-351-test-check-hardcoded-ui-text-literals-20260504T181326Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-351-check-hardcoded-ui-text-literals-20260504T181326Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-351-option-regressions-runner-20260504T181326Z.log`

## Outcome
HAR-351 `BasicText` literal scanner coverage is complete and validated.
