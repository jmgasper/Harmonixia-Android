# HAR-410 Validation - braced interpolation close-block-comment pass fixtures

## Scope
- File: `scripts/test-check-hardcoded-ui-text-literals.sh`
- Purpose:
  - Extend pass coverage for `${...}` interpolation when text/contentDescription values are on close-line block-comment forms, to ensure multiline pending-line detection does not overmatch dynamic strings.

## Added pass fixtures
- `Text` close-line block-comment assignment:
  - `text = /* localized ... */ "Now ${title}"`
- `buildAnnotatedString` close-line block-comment calls:
  - `appendRange(... text = /* localized ... */ "Now ${title}", ...)`
  - `append(... text = /* localized ... */ "Now ${title}", ...)`
  - `appendLine(text = /* localized ... */ "Now ${title}")`
- `Icon` close-line block-comment named argument:
  - `contentDescription = /* localized ... */ "Play ${title}"`

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
- `docs/evidence/har-410-test-check-hardcoded-ui-text-literals-20260506T101620Z.log`
- `docs/evidence/har-410-check-hardcoded-ui-text-literals-20260506T101620Z.log`
- `docs/evidence/har-410-option-regressions-runner-20260506T101620Z.log`
- `docs/evidence/har-410-compileDebugKotlin-20260506T101620Z.log`
- `docs/evidence/har-410-smoke-list-avds-20260506T101620Z.log`
- `docs/evidence/har-410-smoke-install-launch-20260506T101620Z.log`
