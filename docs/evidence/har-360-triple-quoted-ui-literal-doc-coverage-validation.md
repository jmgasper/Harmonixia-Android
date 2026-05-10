# HAR-360 Validation - document triple-quoted UI literal scanner coverage

Validation window (UTC): 20260504T205506Z - 20260504T205506Z

## Scope
- Updated `docs/local-validation-workflow.md` UI literal scanner coverage text to explicitly state that both escaped (`"..."`) and triple-quoted (`"""..."""`) hardcoded literals are detected across Compose callsites and `buildAnnotatedString { ... }` append forms.
- Kept documentation aligned with the implemented scanner behavior from HAR-360 code changes.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-test-check-hardcoded-ui-text-literals-20260504T205506Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-check-hardcoded-ui-text-literals-20260504T205506Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-360-option-regressions-runner-20260504T205506Z.log`

## Outcome
HAR-360 documentation now explicitly covers triple-quoted UI literal scanner detection and remains validated.
