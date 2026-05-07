# HAR-425 Validation — Append/AppendRange Raw Interpolation Escaped-Dollar Pass Fixtures

## Scope
Added pass-fixture coverage for raw-string `buildAnnotatedString` `append(...)` and `appendRange(...)` paths that combine interpolation (`${...}`) and escaped-dollar currency (`\$5`), ensuring scanner behavior remains non-blocking for localized/interpolated content.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixture (`Pass.kt`) with additional raw-string pass cases:
  - `append("""Now ${title} costs \$5""")` and trailing-comment variant.
  - `appendRange(text = """Now ${title} costs \$5""", ...)` and trailing-comment variant.
  - Reordered named-arg `appendRange(endIndex = ..., text = """Now ${title} costs \$5""", startIndex = ...)` and trailing-comment variant.
  - Reordered named-arg `append(end = ..., text = """Now ${title} costs \$5""", start = ...)` and trailing-comment variant.
  - Multiline close-line block-comment variants for raw-string `appendRange` and `append`.

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
- `docs/evidence/har-425-test-check-hardcoded-ui-text-literals-20260507T010030Z.log`
- `docs/evidence/har-425-check-hardcoded-ui-text-literals-20260507T010030Z.log`
- `docs/evidence/har-425-option-regressions-runner-20260507T010030Z.log`
- `docs/evidence/har-425-compileDebugKotlin-20260507T010030Z.log`
- `docs/evidence/har-425-smoke-list-avds-20260507T010030Z.log`
- `docs/evidence/har-425-smoke-install-launch-20260507T010030Z.log`
