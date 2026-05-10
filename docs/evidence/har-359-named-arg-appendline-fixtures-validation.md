# HAR-359 Validation - broaden named-arg `appendLine` literal fixtures

Validation window (UTC): 20260504T194259Z - 20260504T194610Z

## Scope
- Expanded `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures to include `appendLine(value = title)` so named-arg non-literal coverage includes both `text` and `value` forms.
- Broadened named-arg `appendLine` literal fail fixtures to include both:
  - `appendLine(text = "Now playing")`
  - `appendLine(value = "Now playing")`
- Added explicit assertions for both literal forms in the scanner self-test output.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-359-test-check-hardcoded-ui-text-literals-20260504T194259Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-359-check-hardcoded-ui-text-literals-20260504T194259Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-359-option-regressions-runner-20260504T194408Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-359-smoke-list-avds-20260504T194610Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-359-smoke-install-launch-20260504T194610Z.log`

## Outcome
HAR-359 named-arg `appendLine` fixture coverage is broadened and validated.
