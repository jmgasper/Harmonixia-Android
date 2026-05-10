# HAR-422 Validation — AWK Escaped-Dollar Positional Append Detection Fix

## Scope
Fixed a scanner gap where escaped-dollar hardcoded literals in `buildAnnotatedString` positional `append(...)` and `appendRange(...)` calls were missed by the AWK pass.

## Root Cause
Regex patterns were shared between `rg` and AWK, but AWK `-v` assignment processing collapsed escape sequences (`\.` and `\n`).
This caused AWK literal detection to under-match escaped-string cases like `"Price \$5"`.

## Code Changes
- Updated `scripts/check-hardcoded-ui-text-literals.sh`:
  - Added AWK-specific literal regex variables:
    - `awk_escaped_string_literal_pattern`
    - `awk_raw_string_literal_pattern`
    - `awk_literal_pattern`
  - Switched AWK-only paths to those patterns:
    - `annotated_append_literal_pattern`
    - `multiline_direct_literal_line_pattern`
  - Left `rg` patterns unchanged.
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh`:
  - Added new fail fixture `EscapedDollarPositionalAppendPathsFail.kt` covering:
    - `append("Price \$5")`
    - `append("Price \$5" /* ... */)`
    - `append(/* ... */ "Price \$5")`
    - multiline close-line block-comment `append`
    - `appendRange("Price \$5", 0, 3)`
    - inline/trailing/block-comment `appendRange` variants
  - Added corresponding `assert_contains` checks.

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
- `docs/evidence/har-422-test-check-hardcoded-ui-text-literals-20260506T213756Z.log`
- `docs/evidence/har-422-check-hardcoded-ui-text-literals-20260506T213756Z.log`
- `docs/evidence/har-422-option-regressions-runner-20260506T213756Z.log`
- `docs/evidence/har-422-compileDebugKotlin-20260506T213756Z.log`
- `docs/evidence/har-422-smoke-list-avds-20260506T213756Z.log`
- `docs/evidence/har-422-smoke-install-launch-20260506T213756Z.log`
