# HAR-374 Validation - cover reordered append/appendLine named-arg literals

Validation window (UTC): 20260505T034802Z - 20260505T034802Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with reordered named-arg non-literal `append(...)` coverage:
  - `append(start = 0, end = title.length, text = title)`
- Added explicit failing regression fixtures for reordered named-arg hardcoded `append(...)` literals inside `buildAnnotatedString { ... }`:
  - `append(start = 0, end = 3, text = "Now playing")`
  - `append(start = 0, end = 3, text = """Now playing""")`
- Kept named-arg `appendLine(...)` literal coverage aligned through existing fixture assertions.
- Updated docs wording in `docs/local-validation-workflow.md` to call out reordered range-arg named forms for `append(...)`/`appendRange(...)`.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-374-test-check-hardcoded-ui-text-literals-20260505T034802Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-374-check-hardcoded-ui-text-literals-20260505T034802Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-374-option-regressions-runner-20260505T034802Z.log`

## Outcome
HAR-374 now has explicit self-test regression coverage for reordered named-arg `append(...)` hardcoded literals while preserving named-arg `appendLine(...)` coverage expectations and passing scanner gates.
