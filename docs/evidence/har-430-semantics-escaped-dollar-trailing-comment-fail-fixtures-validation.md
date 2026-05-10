# HAR-430 Validation — Semantics Escaped-Dollar Trailing-Comment Fail Fixtures

## Scope
Extended escaped-dollar semantics fail coverage to include trailing-comment variants for both regular and raw-string literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` in `EscapedDollarSemanticsFail.kt` with additional fail cases:
  - `contentDescription = "Volume \$5" /* TODO localize */`
  - `contentDescription = """Volume \$5""" /* TODO localize */`
- Added corresponding assertions for both emitted scanner violations.

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
- `docs/evidence/har-430-test-check-hardcoded-ui-text-literals-20260507T064108Z.log`
- `docs/evidence/har-430-check-hardcoded-ui-text-literals-20260507T064108Z.log`
- `docs/evidence/har-430-option-regressions-runner-20260507T064108Z.log`
- `docs/evidence/har-430-compileDebugKotlin-20260507T064108Z.log`
- `docs/evidence/har-430-smoke-list-avds-20260507T064108Z.log`
- `docs/evidence/har-430-smoke-install-launch-20260507T064108Z.log`
