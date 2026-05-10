# HAR-357 Validation - detect named-arg `append` literals

Validation window (UTC): 20260504T183332Z - 20260504T193516Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` scanner coverage inside `buildAnnotatedString { ... }` blocks to detect hardcoded literals passed via named arguments to:
  - `append(...)`
  - `appendLine(...)`
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - passing non-literal named-arg fixtures (`append(text = title)`, `appendLine(text = title)`)
  - failing literal named-arg fixtures (`append(text = "Now playing")`, `appendLine(value = "Now playing")`)

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-357-test-check-hardcoded-ui-text-literals-20260504T183332Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-357-check-hardcoded-ui-text-literals-20260504T183336Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-357-option-regressions-runner-20260504T193516Z.log`

## Outcome
HAR-357 named-arg `append`/`appendLine` literal scanner coverage is complete and validated.
