# HAR-487 Validation — Icon Raw Reordered Named Compact Leading Inline Block Pass Fixture

## Scope
Extended pass-fixture coverage for `Icon(...)` using reordered named arguments with raw-string interpolation + escaped-dollar currency and compact single-line leading inline block comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = /* localized */ """Play ${title} for \$5""", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for compact reordered-named raw-quote layout.

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
- `docs/evidence/har-487-test-check-hardcoded-ui-text-literals-20260509T162114Z.log`
- `docs/evidence/har-487-check-hardcoded-ui-text-literals-20260509T162114Z.log`
- `docs/evidence/har-487-option-regressions-runner-20260509T162114Z.log`
- `docs/evidence/har-487-compile-debug-kotlin-20260509T162114Z.log`
- `docs/evidence/har-487-smoke-debug-emulator-list-avds-20260509T162114Z.log`
- `docs/evidence/har-487-smoke-debug-emulator-install-debug-20260509T162114Z.log`
