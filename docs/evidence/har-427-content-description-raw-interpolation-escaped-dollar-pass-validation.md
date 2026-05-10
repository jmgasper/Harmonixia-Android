# HAR-427 Validation — ContentDescription Raw Interpolation Escaped-Dollar Pass Fixtures

## Scope
Added pass-fixture coverage for raw-string `contentDescription` paths that combine interpolation (`${...}`) and escaped-dollar currency (`\$5`) so scanner checks do not overmatch localized/interpolated accessibility text.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixture (`Pass.kt`) with additional `Icon(... contentDescription = ...)` pass cases:
  - `contentDescription = """Play ${title} for \$5"""`
  - trailing-comment variant: `"""Play ${title} for \$5""" /* localized */`
  - multiline close-line block-comment variant:
    - `contentDescription = /* localized` newline `*/ """Play ${title} for \$5"""`

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
- `docs/evidence/har-427-test-check-hardcoded-ui-text-literals-20260507T031352Z.log`
- `docs/evidence/har-427-check-hardcoded-ui-text-literals-20260507T031352Z.log`
- `docs/evidence/har-427-option-regressions-runner-20260507T031352Z.log`
- `docs/evidence/har-427-compileDebugKotlin-20260507T031352Z.log`
- `docs/evidence/har-427-smoke-list-avds-20260507T031352Z.log`
- `docs/evidence/har-427-smoke-install-launch-20260507T031352Z.log`
