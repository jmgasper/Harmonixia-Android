# HAR-435 Validation — Raw AnnotatedString Constructor Comment Fail Fixtures

## Scope
Added explicit fail-fixture coverage for raw escaped-dollar `AnnotatedString("""...""")` constructor paths with comment variants.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with a new fail fixture block:
  - `EscapedDollarRawConstructorCommentFail.kt`
- Added fail cases for:
  - trailing-inline comment constructor form
  - multiline close-line block-comment constructor form
- Added corresponding assertions for emitted scanner violations.

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
- `docs/evidence/har-435-test-check-hardcoded-ui-text-literals-20260507T111632Z.log`
- `docs/evidence/har-435-check-hardcoded-ui-text-literals-20260507T111632Z.log`
- `docs/evidence/har-435-option-regressions-runner-20260507T111632Z.log`
- `docs/evidence/har-435-compileDebugKotlin-20260507T111632Z.log`
- `docs/evidence/har-435-smoke-list-avds-20260507T111632Z.log`
- `docs/evidence/har-435-smoke-install-launch-20260507T111632Z.log`
