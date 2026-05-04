# HAR-350 Validation - direct UI literal scanner CI matrix leg

Timestamp (UTC): 20260504T181059Z

## Scope
- Added a dedicated `ui-literal-scanner-direct` matrix mode in `.github/workflows/option-regressions.yml`:
  - `./scripts/check-hardcoded-ui-text-literals.sh`
- Updated `docs/local-validation-workflow.md` CI automation description to include the new direct UI literal scanner mode.

## Validation Commands and Results
1. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-350-check-hardcoded-ui-text-literals-20260504T181059Z.log`
2. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-350-option-regressions-dry-run-20260504T181059Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-350-option-regressions-runner-20260504T181059Z.log`

## Outcome
HAR-350 direct UI literal scanner CI matrix coverage is complete and validated.
