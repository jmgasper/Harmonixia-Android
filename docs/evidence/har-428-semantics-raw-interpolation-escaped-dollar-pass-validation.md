# HAR-428 Validation — Semantics Raw Interpolation Escaped-Dollar Pass Fixtures

## Scope
Added pass-fixture coverage for `Modifier.semantics { contentDescription = ... }` paths using interpolation plus escaped-dollar currency text, including raw-string and close-line block-comment forms.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixture (`Pass.kt`) with semantics contentDescription pass cases:
  - `"Volume ${title}"`
  - `"Volume ${title}" /* localized */`
  - `"""Volume ${title} costs \$5"""`
  - inline block-comment raw variant: `/* localized */ """Volume ${title} costs \$5"""`
  - multiline close-line block-comment raw variant.

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
- `docs/evidence/har-428-test-check-hardcoded-ui-text-literals-20260507T042109Z.log`
- `docs/evidence/har-428-check-hardcoded-ui-text-literals-20260507T042109Z.log`
- `docs/evidence/har-428-option-regressions-runner-20260507T042109Z.log`
- `docs/evidence/har-428-compileDebugKotlin-20260507T042109Z.log`
- `docs/evidence/har-428-smoke-list-avds-20260507T042109Z.log`
- `docs/evidence/har-428-smoke-install-launch-20260507T042109Z.log`
