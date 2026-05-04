# HAR-364 Validation - cover raw named-arg append literals

Validation window (UTC): 20260504T220608Z - 20260504T220608Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` named-arg append failing fixture to include a triple-quoted hardcoded literal:
  - `append(text = """Now playing""")`
- Added assertion to ensure scanner output includes that raw named-arg append literal.
- Kept scanner logic unchanged; this slice hardens regression coverage for existing detection behavior.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-364-test-check-hardcoded-ui-text-literals-20260504T220608Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-364-check-hardcoded-ui-text-literals-20260504T220608Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-364-option-regressions-runner-20260504T220608Z.log`

## Outcome
HAR-364 adds explicit regression coverage for raw named-arg `append(text = ...)` literal detection.
