# HAR-343 Validation - run validate-local option tests directly in CI matrix

Initial timestamp (UTC): 20260504T152847Z  
Continuation timestamp (UTC): 20260504T153325Z

## Scope
- Updated `.github/workflows/option-regressions.yml` matrix to run validate-local option regressions directly via `./scripts/test-validate-local-options.sh`.
- Replaced the prior wrapper matrix leg (`./scripts/validate-local.sh --option-tests`) with a direct option-test leg.
- Updated `docs/local-validation-workflow.md` CI automation summary to reflect direct execution mode.
- Followed up on GitHub Actions run `25327932474` and observed unrelated failures in `agp9-audit-options`, `behavior-only`, and `runner-self-test` due to missing `rg` on runner image `ubuntu-24.04`.
- Added an `Ensure ripgrep is available` step to `.github/workflows/option-regressions.yml` so all matrix legs have the expected dependency before script execution.

## Validation Commands and Results
1. `./scripts/test-validate-local-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-343-test-validate-local-options-20260504T152847Z.log`
2. `./scripts/test-validate-local-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-343-test-validate-local-options-20260504T153325Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-343-test-local-validation-option-regressions-runner-20260504T153325Z.log`
4. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-343-test-local-validation-option-regressions-dry-run-20260504T153325Z.log`

## Outcome
HAR-343 now executes validate-local option tests directly in the CI option-regression matrix, keeps docs aligned, and includes a workflow safeguard that installs `ripgrep` when absent so the option-regression matrix can run reliably on current GitHub-hosted runners.
