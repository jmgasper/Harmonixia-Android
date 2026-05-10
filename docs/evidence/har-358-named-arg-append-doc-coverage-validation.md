# HAR-358 Validation - document named-arg append scanner coverage

Validation window (UTC): 20260504T193840Z - 20260504T193840Z

## Scope
- Updated `docs/local-validation-workflow.md` UI literal scanner coverage text to explicitly include named-arg append forms detected inside `buildAnnotatedString { ... }` blocks:
  - `append(text = "...")`
  - `appendLine(value = "...")`

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-358-test-check-hardcoded-ui-text-literals-20260504T193840Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-358-check-hardcoded-ui-text-literals-20260504T193840Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-358-option-regressions-runner-20260504T193840Z.log`

## Outcome
HAR-358 documentation now reflects named-arg append literal scanner coverage and is validated.
