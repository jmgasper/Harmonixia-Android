# HAR-433 Validation — AnnotatedString Raw Escaped-Dollar Interpolation Pass Fixtures

## Scope
Extended pass-fixture coverage for `AnnotatedString(text = ...)` raw-string interpolation paths that include escaped-dollar currency, ensuring no false positives.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with additional `BasicText(text = AnnotatedString(text = ...))` cases:
  - `"""Now ${title} costs \$5"""`
  - trailing-inline-comment variant
  - multiline close-line block-comment variant

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
- `docs/evidence/har-433-test-check-hardcoded-ui-text-literals-20260507T085917Z.log`
- `docs/evidence/har-433-check-hardcoded-ui-text-literals-20260507T085917Z.log`
- `docs/evidence/har-433-option-regressions-runner-20260507T085917Z.log`
- `docs/evidence/har-433-compileDebugKotlin-20260507T085917Z.log`
- `docs/evidence/har-433-smoke-list-avds-20260507T085917Z.log`
- `docs/evidence/har-433-smoke-install-launch-20260507T085917Z.log`
