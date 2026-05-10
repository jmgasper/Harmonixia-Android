# HAR-371 Validation - add interpolated raw `appendRange` pass fixtures

Validation window (UTC): 20260505T013153Z - 20260505T013215Z

## Scope
- Extended the `pass` fixture in `scripts/test-check-hardcoded-ui-text-literals.sh` with interpolated raw-string `appendRange(...)` forms inside `buildAnnotatedString { ... }`:
  - `appendRange("""$title""", 0, title.length)`
  - `appendRange(text = """$title""", startIndex = 0, endIndex = title.length)`
- Scanner logic was unchanged in this slice; this adds regression coverage that interpolated raw `appendRange` usage remains non-literal and should pass.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-371-test-check-hardcoded-ui-text-literals-20260505T013153Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-371-check-hardcoded-ui-text-literals-20260505T013153Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-371-option-regressions-runner-20260505T013153Z.log`

## Outcome
HAR-371 adds explicit pass-fixture coverage for interpolated raw `appendRange(...)` forms and validates scanner behavior remains green.
