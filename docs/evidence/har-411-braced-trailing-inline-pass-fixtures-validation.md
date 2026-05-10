# HAR-411 Validation - braced interpolation trailing-inline pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Add explicit pass coverage for `${...}` interpolation with trailing inline comments so scanner regex passes do not overmatch localized dynamic strings.

## Added pass fixtures
- Direct UI text:
  - `Text(text = "Now ${title}" /* localized */)`
- `buildAnnotatedString` append paths:
  - `appendLine(text = "Now ${title}" /* localized */)`
  - `appendRange(endIndex = title.length, text = "Now ${title}" /* localized */, startIndex = 0)`
- Content description:
  - `Icon(..., contentDescription = "Play ${title}" /* localized */)`

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
- `docs/evidence/har-411-test-check-hardcoded-ui-text-literals-20260506T112318Z.log`
- `docs/evidence/har-411-check-hardcoded-ui-text-literals-20260506T112318Z.log`
- `docs/evidence/har-411-option-regressions-runner-20260506T112318Z.log`
- `docs/evidence/har-411-compileDebugKotlin-20260506T112318Z.log`
- `docs/evidence/har-411-smoke-list-avds-20260506T112318Z.log`
- `docs/evidence/har-411-smoke-install-launch-20260506T112318Z.log`
