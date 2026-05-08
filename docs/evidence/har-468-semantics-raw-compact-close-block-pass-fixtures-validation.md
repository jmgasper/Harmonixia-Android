# HAR-468 Validation — Semantics Raw Compact Close-Block Pass Fixture

## Scope
Extended pass-fixture coverage for semantics `contentDescription` with raw-string interpolation + escaped-dollar currency and compact close-line block-comment indentation.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription = /* localized ... */ """Volume ${title} costs \$5"""` in compact close-block indentation form inside `Modifier.semantics`.
- Purpose: ensure this compact close-block raw interpolation shape is not flagged by the scanner.

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
- `docs/evidence/har-468-test-check-hardcoded-ui-text-literals-20260508T211650Z.log`
- `docs/evidence/har-468-check-hardcoded-ui-text-literals-20260508T211650Z.log`
- `docs/evidence/har-468-option-regressions-runner-20260508T211650Z.log`
- `docs/evidence/har-468-compile-debug-kotlin-20260508T211650Z.log`
- `docs/evidence/har-468-smoke-debug-emulator-list-avds-20260508T211650Z.log`
- `docs/evidence/har-468-smoke-debug-emulator-install-debug-20260508T211650Z.log`
