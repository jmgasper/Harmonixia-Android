# HAR-421 Validation — Escaped-Dollar Append/AppendRange Fail Coverage

## Scope
Extended scanner regression coverage to ensure hardcoded escaped-dollar literals are detected in `buildAnnotatedString` `appendRange(...)` and `append(...)` paths, including reordered named arguments and inline/multiline close-line block-comment variants.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` in `EscapedDollarNamedArgPathsFail.kt` with new fail fixtures for:
  - `appendRange(text = "Price \$5", startIndex = 0, endIndex = 3)`
  - `appendRange(endIndex = 3, text = "Price \$5", startIndex = 0)`
  - `appendRange(text = /* ... */ "Price \$5", ...)`
  - `appendRange(endIndex = 3, text = /* ... */ "Price \$5", startIndex = 0)`
  - `append(start = 0, end = 3, text = "Price \$5")`
  - `append(end = 3, text = "Price \$5", start = 0)`
  - `append(... text = /* ... */ "Price \$5")`
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
- `docs/evidence/har-421-test-check-hardcoded-ui-text-literals-20260506T202907Z.log`
- `docs/evidence/har-421-check-hardcoded-ui-text-literals-20260506T202907Z.log`
- `docs/evidence/har-421-option-regressions-runner-20260506T202907Z.log`
- `docs/evidence/har-421-compileDebugKotlin-20260506T202907Z.log`
- `docs/evidence/har-421-smoke-list-avds-20260506T202907Z.log`
- `docs/evidence/har-421-smoke-install-launch-20260506T202907Z.log`
