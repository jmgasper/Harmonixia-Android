# HAR-486 Validation — Icon Raw Reordered Named No-Comment Pass Fixture

## Scope
Extended pass-fixture coverage for `Icon(...)` using reordered named arguments with raw-string interpolation + escaped-dollar currency and no localization comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play ${title} for \$5""", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for reordered raw `contentDescription` without comment tokens.

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
- `docs/evidence/har-486-test-check-hardcoded-ui-text-literals-20260509T150752Z.log`
- `docs/evidence/har-486-check-hardcoded-ui-text-literals-20260509T150752Z.log`
- `docs/evidence/har-486-option-regressions-runner-20260509T150752Z.log`
- `docs/evidence/har-486-compile-debug-kotlin-20260509T150752Z.log`
- `docs/evidence/har-486-smoke-debug-emulator-list-avds-20260509T150752Z.log`
- `docs/evidence/har-486-smoke-debug-emulator-install-debug-20260509T150752Z.log`

## Follow-up Matrix Audit (2026-05-10)
- Audited raw reordered named `Icon(contentDescription = ..., imageVector = ...)` pass fixtures across comment styles and call-layout variants.
- Uncovered before this follow-up: compact single-line no-comment raw reordered named variant.

### Follow-up Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play ${title} for \$5""", imageVector = Icons.Outlined.PlayArrow)`

### Follow-up Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

### Follow-up Result
Both commands passed.

### Follow-up Evidence Logs
- `docs/evidence/har-486-test-check-hardcoded-ui-text-literals-20260509T162523Z.log`
- `docs/evidence/har-486-check-hardcoded-ui-text-literals-20260509T162523Z.log`
