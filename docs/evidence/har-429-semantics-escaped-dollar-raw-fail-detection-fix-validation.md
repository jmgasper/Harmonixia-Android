# HAR-429 Validation — Semantics Escaped-Dollar Raw Fail Detection Fix

## Scope
Fixed a scanner gap where hardcoded escaped-dollar **raw** `contentDescription` literals in semantics and similar named-arg paths were missed, and added explicit regression coverage.

## Root Cause
`raw_string_literal_pattern` excluded all `$` characters, so raw literals like `"""Volume \$5"""` were never matched by the direct `rg` contentDescription detection path.

## Code Changes
- Updated `scripts/check-hardcoded-ui-text-literals.sh`:
  - `raw_string_literal_pattern` now allows safe escaped-dollar fragments (`\\$` followed by non-identifier-start chars) while still rejecting interpolation markers (`$name`, `${...}`).
  - Mirrored the same rule in `awk_raw_string_literal_pattern` for AWK pass consistency.
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh`:
  - Added `escaped_dollar_semantics_fail_dir` fixture block covering:
    - `contentDescription = "Volume \$5"`
    - `contentDescription = """Volume \$5"""`
    - inline block-comment raw variant
    - multiline close-line block-comment raw variant
  - Added explicit assertions for expected scanner output lines.

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
- `docs/evidence/har-429-test-check-hardcoded-ui-text-literals-20260507T053035Z.log`
- `docs/evidence/har-429-check-hardcoded-ui-text-literals-20260507T053035Z.log`
- `docs/evidence/har-429-option-regressions-runner-20260507T053035Z.log`
- `docs/evidence/har-429-compileDebugKotlin-20260507T053035Z.log`
- `docs/evidence/har-429-smoke-list-avds-20260507T053035Z.log`
- `docs/evidence/har-429-smoke-install-launch-20260507T053035Z.log`
