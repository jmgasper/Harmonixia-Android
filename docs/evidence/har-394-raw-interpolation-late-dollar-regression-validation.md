# HAR-394 Validation - fix raw interpolation late-dollar false positives

## Scope
- Files:
  - `scripts/check-hardcoded-ui-text-literals.sh`
  - `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Prevent false positives for raw strings where interpolation appears after leading text (for example `"""Now $title"""`).

## Changes
- Tightened raw-string literal regex to reject `$` anywhere in the raw-string body for conservative interpolation-safe matching.
- Added targeted regression fixture coverage for non-leading-dollar raw interpolation in:
  - `Text(text = ... )`
  - `BasicText(text = ... )`
  - `Icon(... contentDescription = ... )`
  - `buildAnnotatedString { appendLine(...) }`

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
- `docs/evidence/har-394-test-check-hardcoded-ui-text-literals-20260505T220454Z.log`
- `docs/evidence/har-394-check-hardcoded-ui-text-literals-20260505T220454Z.log`
- `docs/evidence/har-394-option-regressions-runner-20260505T220454Z.log`
- `docs/evidence/har-394-compileDebugKotlin-20260505T220454Z.log`
- `docs/evidence/har-394-smoke-list-avds-20260505T220454Z.log`
- `docs/evidence/har-394-smoke-install-launch-20260505T220454Z.log`
