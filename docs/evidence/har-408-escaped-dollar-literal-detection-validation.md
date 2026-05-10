# HAR-408 Validation - escaped dollar literal detection

## Scope
- Files:
  - `scripts/check-hardcoded-ui-text-literals.sh`
  - `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Fix false negatives where escaped-dollar hardcoded literals (for example `"Price \$5"`) were skipped because scanner logic ignored any line containing `$`.
  - Keep interpolation paths (for example `"Now $title costs \$5"`) as non-violations.

## Scanner change
- Replaced broad `has_dollar_sign(...)` filter with `has_kotlin_interpolation_marker(...)`.
- New interpolation detection only treats Kotlin template markers as interpolated (`$name` / `${...}`), while allowing escaped-dollar literals to be flagged.
- Applied this filter to multiline direct, multiline assignment, and `buildAnnotatedString` append checks.

## Fixture updates
- Added pass coverage:
  - `EscapedDollarInterpolationPass.kt` for interpolated strings that also include escaped-dollar currency text.
- Added fail coverage:
  - `EscapedDollarLiteralFail.kt` for hardcoded escaped-dollar `Text` and `contentDescription` literals.
  - `EscapedDollarAnnotatedAppendFail.kt` for hardcoded escaped-dollar `buildAnnotatedString.appendLine` literal.

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
- `docs/evidence/har-408-test-check-hardcoded-ui-text-literals-20260506T085445Z.log`
- `docs/evidence/har-408-check-hardcoded-ui-text-literals-20260506T085445Z.log`
- `docs/evidence/har-408-option-regressions-runner-20260506T085445Z.log`
- `docs/evidence/har-408-compileDebugKotlin-20260506T085445Z.log`
- `docs/evidence/har-408-smoke-list-avds-20260506T085445Z.log`
- `docs/evidence/har-408-smoke-install-launch-20260506T085445Z.log`
