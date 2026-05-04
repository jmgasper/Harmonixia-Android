# HAR-343 Validation - run validate-local option tests directly in CI matrix

Timestamp (UTC): 20260504T152847Z

## Scope
- Updated `.github/workflows/option-regressions.yml` matrix to run validate-local option regressions directly via `./scripts/test-validate-local-options.sh`.
- Replaced the prior wrapper matrix leg (`./scripts/validate-local.sh --option-tests`) with a direct option-test leg.
- Updated `docs/local-validation-workflow.md` CI automation summary to reflect direct execution mode.

## Validation Commands and Results
1. `./scripts/test-validate-local-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-343-test-validate-local-options-20260504T152847Z.log`

## Outcome
HAR-343 now executes validate-local option tests directly in the CI option-regression matrix and keeps docs aligned with the workflow behavior.
