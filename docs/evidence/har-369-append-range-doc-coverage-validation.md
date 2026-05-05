# HAR-369 Validation - document appendRange scanner coverage

Validation window (UTC): 20260505T012558Z - 20260505T012558Z

## Scope
- Updated `docs/local-validation-workflow.md` UI literal scanner coverage line to explicitly include `appendRange(...)` detection alongside `append(...)`/`appendLine(...)`.
- Added named-arg `appendRange(text = ..., startIndex = ..., endIndex = ...)` mention to keep documentation aligned with current scanner/self-test behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-test-check-hardcoded-ui-text-literals-20260505T012558Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-check-hardcoded-ui-text-literals-20260505T012558Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-369-option-regressions-runner-20260505T012558Z.log`

## Outcome
HAR-369 documentation now explicitly reflects `appendRange` scanner coverage and is validated.
