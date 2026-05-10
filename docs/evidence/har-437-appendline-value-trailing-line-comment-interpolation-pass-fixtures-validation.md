# HAR-437 Validation — appendLine(value) Trailing Line-Comment Interpolation Pass Fixtures

## Scope
Extended pass-fixture coverage for `appendLine(value = ...)` interpolation paths with escaped-dollar currency when trailing `//` comments are present.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `appendLine(value = "Now ${title} costs \$5" // localized)`
  - `appendLine(value = """Now ${title} costs \$5""" // localized)`
- Purpose: ensure these interpolated string forms remain non-violations in the scanner.

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
- `docs/evidence/har-437-test-check-hardcoded-ui-text-literals-20260507T153744Z.log`
- `docs/evidence/har-437-check-hardcoded-ui-text-literals-20260507T153744Z.log`
- `docs/evidence/har-437-option-regressions-runner-20260507T153744Z.log`
- `docs/evidence/har-437-compileDebugKotlin-20260507T153744Z.log`
- `docs/evidence/har-437-smoke-list-avds-20260507T153744Z.log`
- `docs/evidence/har-437-smoke-install-launch-20260507T153744Z.log`
