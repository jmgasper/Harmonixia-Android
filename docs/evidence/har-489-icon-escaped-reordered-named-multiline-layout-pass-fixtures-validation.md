# HAR-489 Validation — Icon Escaped Reordered Named Multiline Layout Pass Fixtures

## Scope
Extended pass-fixture coverage for `Icon(...)` using reordered named arguments with escaped-string interpolation + escaped-dollar currency in multiline assignment layouts.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with four escaped reordered-named multiline variants:
  - `contentDescription =\n    "Play ${title} for \$5"`
  - `contentDescription =\n    "Play ${title} for \$5" /* localized */`
  - `contentDescription =\n    /* localized */ "Play ${title} for \$5"`
  - `contentDescription =\n    /* localized ... */ "Play ${title} for \$5"`
- Purpose: mirror existing raw-string multiline coverage and ensure scanner pass behavior is stable for escaped-string multiline formatting.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew :app:compileDebugKotlin`
5. `$HOME/Android/Sdk/emulator/emulator -list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$HOME/Android/Sdk/emulator:$HOME/Android/Sdk/platform-tools:$PATH ./scripts/smoke-debug-emulator.sh --task :app:installDebug`

## Result
All commands passed.

## Evidence Logs
- `docs/evidence/har-489-test-check-hardcoded-ui-text-literals-20260509T183739Z.log`
- `docs/evidence/har-489-check-hardcoded-ui-text-literals-20260509T183739Z.log`
- `docs/evidence/har-489-test-local-validation-option-regressions-runner-20260509T183739Z.log`
- `docs/evidence/har-489-gradlew-app-compileDebugKotlin-20260509T183739Z.log`
- `docs/evidence/har-489-emulator-list-avds-20260509T183739Z.log`
- `docs/evidence/har-489-smoke-debug-emulator-installDebug-20260509T183739Z.log`

## Follow-up Matrix Audit (2026-05-10)
- Audited escaped reordered named `Icon(contentDescription = ..., imageVector = ...)` pass fixtures across compact single-line and multiline layouts.
- Uncovered before this follow-up: compact single-line no-comment escaped reordered named variant.

### Follow-up Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = "Play ${title} for \$5", imageVector = Icons.Outlined.PlayArrow)`

### Follow-up Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

### Follow-up Result
Both commands passed.

### Follow-up Evidence Logs
- `docs/evidence/har-489-test-check-hardcoded-ui-text-literals-20260509T194815Z.log`
- `docs/evidence/har-489-check-hardcoded-ui-text-literals-20260509T194815Z.log`
