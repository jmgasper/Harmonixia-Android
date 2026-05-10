# HAR-362 Validation - cover triple-quoted Text named-arg literals

Validation window (UTC): 20260504T205847Z - 20260504T205847Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with an explicit failing fixture for a triple-quoted hardcoded `Text(text = """...""")` literal.
- Kept scanner logic unchanged; this slice hardens regression coverage for existing named-text triple-quoted detection behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-362-test-check-hardcoded-ui-text-literals-20260504T205847Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-362-check-hardcoded-ui-text-literals-20260504T205847Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-362-option-regressions-runner-20260504T205847Z.log`

## Outcome
HAR-362 adds explicit regression coverage for triple-quoted `Text(text = ...)` literal detection without changing scanner behavior.
