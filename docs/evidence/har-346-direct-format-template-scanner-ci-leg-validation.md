# HAR-346 Validation - direct format-template scanner CI matrix leg

Timestamp (UTC): 20260504T164835Z

## Scope
- Added a dedicated `format-template-scanner-direct` matrix mode in `.github/workflows/option-regressions.yml`:
  - `./scripts/check-hardcoded-format-templates.sh`
- Updated `docs/local-validation-workflow.md` CI automation description to include the new direct format-template scanner mode.

## Validation Commands and Results
1. `./scripts/check-hardcoded-format-templates.sh`
   - Result: PASS
   - Log: `docs/evidence/har-346-check-hardcoded-format-templates-20260504T164835Z.log`
2. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-346-test-local-validation-option-regressions-runner-20260504T164835Z.log`
3. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-346-test-local-validation-option-regressions-dry-run-20260504T164835Z.log`
4. `rg -n "format-template-scanner-direct|check-hardcoded-format-templates.sh" .github/workflows/option-regressions.yml`
   - Result: PASS (workflow includes the direct matrix leg and scanner script path filters)
   - Log: `docs/evidence/har-346-option-regressions-workflow-matrix-20260504T164835Z.log`

## Outcome
HAR-346 direct format-template scanner CI matrix coverage is complete and validated.
