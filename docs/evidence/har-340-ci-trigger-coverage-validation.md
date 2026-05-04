# HAR-340 Validation - CI trigger coverage for UI literal scanner self-test

Timestamp (UTC): 20260504T141355Z

## Scope
- Added `scripts/test-check-hardcoded-ui-text-literals.sh` to `.github/workflows/option-regressions.yml` path filters for both `push` and `pull_request`.
- Updated `docs/local-validation-workflow.md` to explicitly mention the UI text-literal scanner self-test in local and CI behavior coverage notes.

## Validation Commands and Results
1. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-340-option-regressions-runner-self-test-20260504T141355Z.log`
2. `./scripts/test-local-validation-option-regressions.sh --behavior-only`
   - Result: PASS
   - Log: `docs/evidence/har-340-option-regressions-behavior-only-20260504T141355Z.log`
3. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-340-option-regressions-dry-run-20260504T141355Z.log`

## Outcome
HAR-340 CI trigger coverage and workflow docs alignment are complete and validated.
