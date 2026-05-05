# HAR-366 Validation - detect `appendRange` UI literals

Validation window (UTC): 20260505T001455Z - 20260505T001455Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` scanner pattern inside `buildAnnotatedString` blocks from `append/appendLine` to `append/appendLine/appendRange`.
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - pass fixtures for non-literal `appendRange` positional and named-arg usage
  - fail fixtures for hardcoded literal `appendRange` positional and named-arg usage
  - explicit raw positional `appendLine("""...""")` fail fixture coverage retained in the same test slice.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-366-test-check-hardcoded-ui-text-literals-20260505T001455Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-366-check-hardcoded-ui-text-literals-20260505T001455Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-366-option-regressions-runner-20260505T001455Z.log`

## Outcome
HAR-366 adds scanner and regression coverage for `appendRange(...)` hardcoded literals in annotated string builders.
