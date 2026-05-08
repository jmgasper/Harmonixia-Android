# HAR-462 Validation — Semantics Raw Trailing Inline Comment Pass Fixture

## Scope
Extended pass-fixture coverage for semantics `contentDescription` with raw-string interpolation + escaped-dollar currency and a trailing inline localization comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Box(modifier = Modifier.semantics { contentDescription = """Volume ${title} costs \$5""" /* localized */ })`
- Purpose: ensure this raw-string trailing-inline-comment interpolation shape is not flagged by the scanner.

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
- `docs/evidence/har-462-test-check-hardcoded-ui-text-literals-20260508T154925Z.log`
- `docs/evidence/har-462-check-hardcoded-ui-text-literals-20260508T154925Z.log`
- `docs/evidence/har-462-option-regressions-runner-20260508T154925Z.log`
- `docs/evidence/har-462-compile-debug-kotlin-20260508T154925Z.log`
- `docs/evidence/har-462-smoke-debug-emulator-list-avds-20260508T154925Z.log`
- `docs/evidence/har-462-smoke-debug-emulator-install-debug-20260508T154925Z.log`
