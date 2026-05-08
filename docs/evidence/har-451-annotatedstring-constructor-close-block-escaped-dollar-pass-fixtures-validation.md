# HAR-451 Validation — AnnotatedString Constructor Close-Block Escaped-Dollar Pass Fixture

## Scope
Extended pass-fixture coverage for escaped-string interpolation + escaped-dollar currency in `AnnotatedString(...)` constructor form with close-line block-comment formatting.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString(/* localized ... */ "Now ${title} costs \$5"))`
- Purpose: ensure this constructor-style interpolated escaped-string layout is not flagged by the scanner.

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
- `docs/evidence/har-451-test-check-hardcoded-ui-text-literals-20260508T055716Z.log`
- `docs/evidence/har-451-check-hardcoded-ui-text-literals-20260508T055716Z.log`
- `docs/evidence/har-451-option-regressions-runner-20260508T055716Z.log`
- `docs/evidence/har-451-compile-debug-kotlin-20260508T055716Z.log`
- `docs/evidence/har-451-smoke-debug-emulator-list-avds-20260508T055716Z.log`
- `docs/evidence/har-451-smoke-debug-emulator-install-debug-20260508T055716Z.log`
