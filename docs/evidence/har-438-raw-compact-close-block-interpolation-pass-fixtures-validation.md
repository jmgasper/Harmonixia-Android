# HAR-438 Validation — Raw Compact Close-Block Interpolation Pass Fixtures

## Scope
Extended pass-fixture coverage for compact close-block comment formatting on raw interpolation + escaped-dollar paths in `buildAnnotatedString` named-arg calls.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with compact close-block variants for:
  - `appendRange(endIndex = ..., text = /* localized ... */ """Now ${title} costs \$5""", startIndex = ...)`
  - `append(end = ..., text = /* localized ... */ """Now ${title} costs \$5""", start = ...)`
  - `appendLine(value = /* localized ... */ """Now ${title} costs \$5""")`
- These mirror recent fail-side compact formatting cases while ensuring interpolated strings still pass.

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
- `docs/evidence/har-438-test-check-hardcoded-ui-text-literals-20260507T174653Z.log`
- `docs/evidence/har-438-check-hardcoded-ui-text-literals-20260507T174653Z.log`
- `docs/evidence/har-438-option-regressions-runner-20260507T174653Z.log`
- `docs/evidence/har-438-compileDebugKotlin-20260507T174653Z.log`
- `docs/evidence/har-438-smoke-list-avds-20260507T174653Z.log`
- `docs/evidence/har-438-smoke-install-launch-20260507T174653Z.log`
