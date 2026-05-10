# HAR-344 Validation - direct smoke option tests CI matrix leg

Timestamp (UTC): 20260504T164101Z

## Scope
- Added a dedicated `smoke-options-direct` matrix mode in `.github/workflows/option-regressions.yml`:
  - `./scripts/test-smoke-debug-emulator-options.sh`
- Updated `docs/local-validation-workflow.md` CI automation description to include the new direct smoke-options mode.

## Validation Commands and Results
1. `./scripts/test-smoke-debug-emulator-options.sh`
   - Result: PASS
   - Log: `docs/evidence/har-344-test-smoke-debug-emulator-options-20260504T164101Z.log`
2. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-344-test-local-validation-option-regressions-runner-20260504T164101Z.log`
3. `./scripts/test-local-validation-option-regressions.sh --dry-run`
   - Result: PASS
   - Log: `docs/evidence/har-344-test-local-validation-option-regressions-dry-run-20260504T164101Z.log`

## Outcome
HAR-344 direct smoke-option CI matrix coverage is complete and validated.
