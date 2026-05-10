# HAR-373 Validation - document reordered appendRange named-arg coverage

Validation window (UTC): 20260505T023940Z - 20260505T023940Z

## Scope
- Updated `docs/local-validation-workflow.md` UI literal scanner coverage text to explicitly note reordered named-arg `appendRange` detection, where `text` may appear after index args.
- Kept docs aligned with HAR-372 scanner matcher behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-373-test-check-hardcoded-ui-text-literals-20260505T023940Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-373-check-hardcoded-ui-text-literals-20260505T023940Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-373-option-regressions-runner-20260505T023940Z.log`

## Outcome
HAR-373 documentation now explicitly reflects reordered named-arg `appendRange` scanner coverage and is validated.
