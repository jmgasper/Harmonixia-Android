# HAR-443 Validation — appendLine(text) Escaped-Dollar Compact Close-Block Pass Fixtures

## Scope
Extended pass-fixture coverage for escaped-string interpolation + escaped-dollar on `appendLine(text = ...)` using compact close-line block-comment formatting.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `appendLine(text = /* localized ... */ "Now ${title} costs \$5")` in compact close-block form.
- Purpose: ensure this interpolated escaped-string layout is not flagged by the scanner.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
All commands passed.

## Evidence Logs
- `docs/evidence/har-443-test-check-hardcoded-ui-text-literals-20260508T002234Z.log`
- `docs/evidence/har-443-check-hardcoded-ui-text-literals-20260508T002234Z.log`
- `docs/evidence/har-443-option-regressions-runner-20260508T002234Z.log`
- `docs/evidence/har-443-compileDebugKotlin-20260508T002234Z.log`
- `docs/evidence/har-443-smoke-list-avds-20260508T002234Z.log`
- `docs/evidence/har-443-smoke-install-launch-20260508T002234Z.log`
