# HAR-436 Validation — Reordered Raw Escaped-Dollar Trailing Line-Comment Pass Fixtures

## Scope
Extended pass-fixture coverage for reordered named-argument `append`/`appendRange` raw interpolation paths with escaped-dollar currency and trailing `//` line comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures to include:
  - `append(end = ..., text = """Now ${title} costs \$5""", // localized, start = ...)`
  - `appendRange(endIndex = ..., text = """Now ${title} costs \$5""", // localized, startIndex = ...)`
- Purpose: ensure scanner does not overmatch interpolated raw strings in this trailing line-comment form.

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
- `docs/evidence/har-436-test-check-hardcoded-ui-text-literals-20260507T132820Z.log`
- `docs/evidence/har-436-check-hardcoded-ui-text-literals-20260507T132820Z.log`
- `docs/evidence/har-436-option-regressions-runner-20260507T132820Z.log`
- `docs/evidence/har-436-compileDebugKotlin-20260507T132820Z.log`
- `docs/evidence/har-436-smoke-list-avds-20260507T132820Z.log`
- `docs/evidence/har-436-smoke-install-launch-20260507T132820Z.log`
