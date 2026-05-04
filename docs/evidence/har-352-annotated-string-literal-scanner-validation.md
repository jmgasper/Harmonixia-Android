# HAR-352 Validation - detect `AnnotatedString` hardcoded literals

Timestamp (UTC): 20260504T181847Z

## Scope
- Extended `scripts/check-hardcoded-ui-text-literals.sh` to detect:
  - `AnnotatedString("...")` constructor literals.
  - `append("...")` literals inside `buildAnnotatedString { ... }` blocks.
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - Passing non-literal `AnnotatedString(title)` and `buildAnnotatedString { append(title) }` fixtures.
  - Failing `AnnotatedString("...")` and `buildAnnotatedString { append("...") }` fixtures.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-352-test-check-hardcoded-ui-text-literals-20260504T181847Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-352-check-hardcoded-ui-text-literals-20260504T181847Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-352-option-regressions-runner-20260504T181847Z.log`
4. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-352-smoke-list-avds-20260504T182024Z.log`
5. `scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`)
   - Log: `docs/evidence/har-352-smoke-install-launch-20260504T182024Z.log`

## Outcome
HAR-352 `AnnotatedString` literal scanner coverage is complete and validated.
