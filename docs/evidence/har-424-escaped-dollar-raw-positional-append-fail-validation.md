# HAR-424 Validation — Escaped-Dollar Raw Positional Append Fail Fixtures

## Scope
Added explicit regression coverage to ensure the scanner flags hardcoded escaped-dollar **raw-string** literals in `buildAnnotatedString` positional `append(...)` and `appendRange(...)` paths.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with new fail fixture block:
  - `escaped_dollar_raw_positional_append_paths_fail_dir`
  - `EscapedDollarRawPositionalAppendPathsFail.kt`
- Added fail cases for:
  - `append("""Price \$5""")`
  - `append("""Price \$5""" /* ... */)`
  - `append(/* ... */ """Price \$5""")`
  - `appendRange("""Price \$5""", 0, 3)`
  - `appendRange("""Price \$5""" /* ... */, 0, 3)`
  - `appendRange(/* ... */ """Price \$5""", 0, 3)`
- Added corresponding assertions for all emitted snippets.

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
- `docs/evidence/har-424-test-check-hardcoded-ui-text-literals-20260506T235339Z.log`
- `docs/evidence/har-424-check-hardcoded-ui-text-literals-20260506T235339Z.log`
- `docs/evidence/har-424-option-regressions-runner-20260506T235339Z.log`
- `docs/evidence/har-424-compileDebugKotlin-20260506T235339Z.log`
- `docs/evidence/har-424-smoke-list-avds-20260506T235339Z.log`
- `docs/evidence/har-424-smoke-install-launch-20260506T235339Z.log`
