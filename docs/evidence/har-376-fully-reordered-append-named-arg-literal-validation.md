# HAR-376 Validation - cover fully reordered append named-arg literals

Validation window (UTC): 20260505T045621Z - 20260505T045621Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with fully reordered named-arg non-literal `append(...)` forms:
  - `append(end = title.length, text = title, start = 0)`
  - `append(end = title.length, text = """$title""", start = 0)`
- Extended reordered named-arg fail fixtures for hardcoded literals in the same arg order:
  - `append(end = 3, text = "Now playing", start = 0)`
  - `append(end = 3, text = """Now playing""", start = 0)`
- Kept scanner logic unchanged; this slice hardens regression coverage for argument-order-independent detection.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-376-test-check-hardcoded-ui-text-literals-20260505T045621Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-376-check-hardcoded-ui-text-literals-20260505T045621Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-376-option-regressions-runner-20260505T045621Z.log`

## Outcome
HAR-376 now explicitly validates that reordered named-arg `append(...)` literal detection remains stable across arg-order permutations.
