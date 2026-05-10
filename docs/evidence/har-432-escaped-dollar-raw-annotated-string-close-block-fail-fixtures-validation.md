# HAR-432 Validation — Escaped-Dollar Raw AnnotatedString Close-Block Fail Fixtures

## Scope
Extended escaped-dollar raw-literal fail coverage to include multiline close-block comment handling for `AnnotatedString(text = ...)` paths.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` in `EscapedDollarRawNamedArgPathsFail.kt` with an additional fail case:
  - `BasicText(text = AnnotatedString(text = /* TODO localize ... */ """Price \$5"""))`
- Added a matching assertion for the close-line emitted violation snippet:
  - `*/ """Price \$5"""`

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
- `docs/evidence/har-432-test-check-hardcoded-ui-text-literals-20260507T075301Z.log`
- `docs/evidence/har-432-check-hardcoded-ui-text-literals-20260507T075301Z.log`
- `docs/evidence/har-432-option-regressions-runner-20260507T075301Z.log`
- `docs/evidence/har-432-compileDebugKotlin-20260507T075301Z.log`
- `docs/evidence/har-432-smoke-list-avds-20260507T075301Z.log`
- `docs/evidence/har-432-smoke-install-launch-20260507T075301Z.log`
