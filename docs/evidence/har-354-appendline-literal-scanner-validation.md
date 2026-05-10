# HAR-354 Validation - detect `appendLine` hardcoded literals

Timestamp (UTC): 20260504T182537Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` scanner coverage to detect `appendLine("...")` literals inside `buildAnnotatedString { ... }` blocks.
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - passing non-literal `appendLine(title)` fixture
  - failing literal `appendLine("Now playing")` fixture

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-354-test-check-hardcoded-ui-text-literals-20260504T182537Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-354-check-hardcoded-ui-text-literals-20260504T182537Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-354-option-regressions-runner-20260504T182537Z.log`

## Outcome
HAR-354 `appendLine` literal scanner coverage is complete and validated.
