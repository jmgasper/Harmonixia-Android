# HAR-355 Validation - detect `AnnotatedString(text = "...")` literals

Timestamp (UTC): 20260504T182759Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` to detect hardcoded named-argument annotated-string literals:
  - `AnnotatedString(text = "...")`
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - passing non-literal named-arg fixture (`AnnotatedString(text = title)`)
  - failing literal named-arg fixture (`AnnotatedString(text = "Now playing")`)

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-355-test-check-hardcoded-ui-text-literals-20260504T182759Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-355-check-hardcoded-ui-text-literals-20260504T182759Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-355-option-regressions-runner-20260504T182759Z.log`

## Outcome
HAR-355 named-argument annotated-string scanner coverage is complete and validated.
