# HAR-478 Validation — Icon Escaped Reordered Named Trailing Inline Block Pass Fixture

## Scope
Extended pass-fixture coverage for `Icon(...)` using reordered named arguments with escaped-string interpolation + escaped-dollar currency and trailing inline block comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = "Play ${title} for \$5" /* localized */, imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct when escaped `contentDescription` appears before `imageVector` in named-arg form.

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
- `docs/evidence/har-478-test-check-hardcoded-ui-text-literals-20260509T070643Z.log`
- `docs/evidence/har-478-check-hardcoded-ui-text-literals-20260509T070643Z.log`
- `docs/evidence/har-478-option-regressions-runner-20260509T070643Z.log`
- `docs/evidence/har-478-compile-debug-kotlin-20260509T070643Z.log`
- `docs/evidence/har-478-smoke-debug-emulator-list-avds-20260509T070643Z.log`
- `docs/evidence/har-478-smoke-debug-emulator-install-debug-20260509T070643Z.log`
