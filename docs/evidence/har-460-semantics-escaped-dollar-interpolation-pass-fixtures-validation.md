# HAR-460 Validation — Semantics Escaped-Dollar Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for escaped-string interpolation + escaped-dollar currency in semantics `contentDescription`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Box(modifier = Modifier.semantics { contentDescription = "Volume ${title} costs \$5" })`
- Purpose: ensure escaped-string interpolated semantics text is not flagged by the scanner.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
All commands passed.

## Evidence Logs
- `docs/evidence/har-460-test-check-hardcoded-ui-text-literals-20260508T144038Z.log`
- `docs/evidence/har-460-check-hardcoded-ui-text-literals-20260508T144038Z.log`
- `docs/evidence/har-460-option-regressions-runner-20260508T144038Z.log`
- `docs/evidence/har-460-compile-debug-kotlin-20260508T144038Z.log`
- `docs/evidence/har-460-smoke-debug-emulator-list-avds-20260508T144038Z.log`
- `docs/evidence/har-460-smoke-debug-emulator-install-debug-20260508T144038Z.log`
