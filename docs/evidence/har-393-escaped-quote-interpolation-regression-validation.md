# HAR-393 Validation - fix escaped-quote interpolation false positives

## Scope
- Files:
  - `scripts/check-hardcoded-ui-text-literals.sh`
  - `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Prevent false positives for interpolated strings that contain escaped quotes (for example `"$title"`).
  - Preserve detection of true hardcoded escaped-quote literals.

## Changes
- Updated escaped string literal regex to support escaped characters while rejecting unescaped interpolation markers.
- Reused the shared literal regex in multiline direct-literal matching and `buildAnnotatedString` append-literal matching for consistency.
- Added regression fixtures:
  - pass: interpolated escaped-quote forms (`Text`, named `text =`, and `contentDescription =`)
  - fail: hardcoded escaped-quote literal form

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
- `docs/evidence/har-393-test-check-hardcoded-ui-text-literals-20260505T205815Z.log`
- `docs/evidence/har-393-check-hardcoded-ui-text-literals-20260505T205815Z.log`
- `docs/evidence/har-393-option-regressions-runner-20260505T205815Z.log`
- `docs/evidence/har-393-compileDebugKotlin-20260505T205815Z.log`
- `docs/evidence/har-393-smoke-list-avds-20260505T205815Z.log`
- `docs/evidence/har-393-smoke-install-launch-20260505T205815Z.log`
