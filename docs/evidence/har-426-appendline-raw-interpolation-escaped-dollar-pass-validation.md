# HAR-426 Validation — AppendLine Raw Interpolation Escaped-Dollar Pass Fixtures

## Scope
Added pass-fixture coverage for raw-string `appendLine(...)` paths that combine interpolation (`${...}`) with escaped-dollar currency (`\$5`) to prevent false positives in scanner checks.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixture (`Pass.kt`) with additional `buildAnnotatedString` pass cases for:
  - `appendLine(text = """Now ${title} costs \$5""")`
  - `appendLine(text = """Now ${title} costs \$5""" /* localized */)`
  - `appendLine(text = /* localized ... */ """Now ${title} costs \$5""")`
  - positional `appendLine("""Now ${title} costs \$5""")`
  - `appendLine(value = """Now ${title} costs \$5""")`
  - `appendLine(value = """Now ${title} costs \$5""" /* localized */)`
  - `appendLine(value = /* localized ... */ """Now ${title} costs \$5""")`

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
- `docs/evidence/har-426-test-check-hardcoded-ui-text-literals-20260507T020651Z.log`
- `docs/evidence/har-426-check-hardcoded-ui-text-literals-20260507T020651Z.log`
- `docs/evidence/har-426-option-regressions-runner-20260507T020651Z.log`
- `docs/evidence/har-426-compileDebugKotlin-20260507T020651Z.log`
- `docs/evidence/har-426-smoke-list-avds-20260507T020651Z.log`
- `docs/evidence/har-426-smoke-install-launch-20260507T020651Z.log`
