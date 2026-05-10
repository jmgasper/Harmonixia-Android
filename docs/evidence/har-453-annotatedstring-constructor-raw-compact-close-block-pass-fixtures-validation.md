# HAR-453 Validation — AnnotatedString Constructor Raw Compact Close-Block Pass Fixture

## Scope
Extended pass-fixture coverage for raw-string interpolation + escaped-dollar currency in `AnnotatedString(...)` constructor form with compact close-line block-comment formatting.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - compact-indent close-block variant of `BasicText(text = AnnotatedString(/* localized ... */ """Now ${title} costs \$5"""))`.
- Purpose: ensure this compact raw constructor-style interpolated layout is not flagged by the scanner.

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
- `docs/evidence/har-453-test-check-hardcoded-ui-text-literals-20260508T080730Z.log`
- `docs/evidence/har-453-check-hardcoded-ui-text-literals-20260508T080730Z.log`
- `docs/evidence/har-453-option-regressions-runner-20260508T080730Z.log`
- `docs/evidence/har-453-compile-debug-kotlin-20260508T080730Z.log`
- `docs/evidence/har-453-smoke-debug-emulator-list-avds-20260508T080730Z.log`
- `docs/evidence/har-453-smoke-debug-emulator-install-debug-20260508T080730Z.log`
