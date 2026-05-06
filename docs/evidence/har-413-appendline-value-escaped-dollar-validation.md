# HAR-413 Validation - appendLine(value=...) escaped-dollar detection

## Scope
- Files:
  - `scripts/check-hardcoded-ui-text-literals.sh`
  - `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Fix a scanner gap where `appendLine(value = "Price \$5")` was not reported even though escaped-dollar hardcoded literals should fail.

## Scanner change
- Added explicit regex pass for named `value` argument literals in `appendLine(...)`:
  - `annotated_append_line_named_value_pattern="appendLine\\([^)]*value\\s*=\\s*(/[*].*[*]/\\s*)?${literal_pattern}"`
- Wired the new pattern into the top-level `rg` violation collection pass.

## Regression fixture update
- Extended `EscapedDollarNamedArgPathsFail.kt` fixture to include:
  - `appendLine(value = "Price \$5")`
- Added assertion that failure output includes the new `appendLine(value = ...)` violation line.

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
- `docs/evidence/har-413-test-check-hardcoded-ui-text-literals-20260506T144734Z.log`
- `docs/evidence/har-413-check-hardcoded-ui-text-literals-20260506T144734Z.log`
- `docs/evidence/har-413-option-regressions-runner-20260506T144734Z.log`
- `docs/evidence/har-413-compileDebugKotlin-20260506T144734Z.log`
- `docs/evidence/har-413-smoke-list-avds-20260506T144734Z.log`
- `docs/evidence/har-413-smoke-install-launch-20260506T144734Z.log`
