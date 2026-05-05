# HAR-365 Validation - cover raw positional `appendLine` literals

Validation window (UTC): 20260504T220850Z - 20260504T220850Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with a dedicated failing fixture for positional raw `appendLine` hardcoded literals inside `buildAnnotatedString { ... }`:
  - `appendLine("""Now playing""")`
- Added an explicit assertion to ensure scanner output includes that raw positional `appendLine` literal.
- Kept scanner logic unchanged; this slice adds explicit regression coverage for already-supported behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-365-test-check-hardcoded-ui-text-literals-20260504T220850Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-365-check-hardcoded-ui-text-literals-20260504T220850Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-365-option-regressions-runner-20260504T220850Z.log`

## Outcome
HAR-365 adds explicit regression coverage for raw positional `appendLine(...)` literal detection.
