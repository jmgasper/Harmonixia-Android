# HAR-431 Validation — Escaped-Dollar Raw AnnotatedString Fail Fixtures

## Scope
Extended escaped-dollar raw-literal fail coverage to explicitly include `AnnotatedString(...)` constructor and named-text paths.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` in `EscapedDollarRawNamedArgPathsFail.kt` with additional fail cases:
  - `BasicText(text = AnnotatedString("""Price \$5"""))`
  - `BasicText(text = AnnotatedString(text = """Price \$5"""))`
  - `BasicText(text = AnnotatedString(text = /* TODO localize */ """Price \$5"""))`
- Added matching assertions for all three emitted scanner violations.

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
- `docs/evidence/har-431-test-check-hardcoded-ui-text-literals-20260507T074904Z.log`
- `docs/evidence/har-431-check-hardcoded-ui-text-literals-20260507T074904Z.log`
- `docs/evidence/har-431-option-regressions-runner-20260507T074904Z.log`
- `docs/evidence/har-431-compileDebugKotlin-20260507T074904Z.log`
- `docs/evidence/har-431-smoke-list-avds-20260507T074904Z.log`
- `docs/evidence/har-431-smoke-install-launch-20260507T074904Z.log`
