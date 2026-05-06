# HAR-404 Validation - append close-line escaped-quote false-positive fix

## Scope
- Files:
  - `scripts/check-hardcoded-ui-text-literals.sh`
  - `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Fix false positives where AWK regex matching in append/multiline paths could still flag interpolated strings containing `$` (especially close-line block-comment + escaped-quote forms).

## Changes
- Scanner hardening in AWK path:
  - Added `has_dollar_sign(...)` helper.
  - Gated append and multiline literal-reporting branches to skip matches on lines containing `$`, preserving conservative interpolation behavior.
- Added pass fixtures for close-line block-comment escaped-quote interpolation in `buildAnnotatedString` append APIs.

## Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
- All commands passed.
- Emulator smoke verified launch on `Medium_Phone` with `topResumedActivity=com.harmonixia.android/.MainActivity`.

## Logs
- `docs/evidence/har-404-test-check-hardcoded-ui-text-literals-20260506T052328Z.log`
- `docs/evidence/har-404-check-hardcoded-ui-text-literals-20260506T052328Z.log`
- `docs/evidence/har-404-option-regressions-runner-20260506T052328Z.log`
- `docs/evidence/har-404-compileDebugKotlin-20260506T052328Z.log`
- `docs/evidence/har-404-smoke-list-avds-20260506T052328Z.log`
- `docs/evidence/har-404-smoke-install-launch-20260506T052328Z.log`
