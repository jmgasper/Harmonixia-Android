# HAR-423 Validation — Append/AppendRange Mixed Interpolation Escaped-Dollar Pass Fixtures

## Scope
Added pass-fixture coverage to ensure the hardcoded-literal scanner does not overmatch `buildAnnotatedString` `append(...)` and `appendRange(...)` paths when strings include real interpolation (`${...}`) plus escaped-dollar currency text (`\$5`).

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixture (`Pass.kt`) with additional `buildAnnotatedString` pass cases for:
  - `append("Now ${title} costs \$5")` and trailing-comment variant.
  - `appendRange(text = "Now ${title} costs \$5", ...)` and trailing-comment variant.
  - Reordered named-arg `appendRange(endIndex = ..., text = "Now ${title} costs \$5", startIndex = ...)` and trailing-comment variant.
  - Reordered named-arg `append(end = ..., text = "Now ${title} costs \$5", start = ...)` and trailing-comment variant.
  - Multiline close-line block-comment forms for both `appendRange` and `append` with `"Now ${title} costs \$5"`.

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
- `docs/evidence/har-423-test-check-hardcoded-ui-text-literals-20260506T224619Z.log`
- `docs/evidence/har-423-check-hardcoded-ui-text-literals-20260506T224619Z.log`
- `docs/evidence/har-423-option-regressions-runner-20260506T224619Z.log`
- `docs/evidence/har-423-compileDebugKotlin-20260506T224619Z.log`
- `docs/evidence/har-423-smoke-list-avds-20260506T224619Z.log`
- `docs/evidence/har-423-smoke-install-launch-20260506T224619Z.log`
