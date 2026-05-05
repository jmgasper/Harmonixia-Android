# HAR-382 Validation — Commented Multiline Positional `appendLine(...)` Literals

## Scope
- Extended regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` to cover commented multiline positional `appendLine(...)` literals in `buildAnnotatedString`.
- Added fail fixture cases with:
  - inline comment on call start line (`appendLine( // ...`) 
  - inline comment on literal line (`"Now playing" // ...`)
  - comment-only line before a raw literal

## Commands Run
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
- All checks passed.
- Emulator smoke install/launch passed on `Medium_Phone` with app resumed (`com.harmonixia.android/.MainActivity`).

## Evidence Logs
- `docs/evidence/har-382-test-check-hardcoded-ui-text-literals-20260505T115142Z.log`
- `docs/evidence/har-382-check-hardcoded-ui-text-literals-20260505T115142Z.log`
- `docs/evidence/har-382-option-regressions-runner-20260505T115142Z.log`
- `docs/evidence/har-382-compileDebugKotlin-20260505T115142Z.log`
- `docs/evidence/har-382-smoke-list-avds-20260505T115142Z.log`
- `docs/evidence/har-382-smoke-install-launch-20260505T115142Z.log`
