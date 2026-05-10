# HAR-383 Validation — Commented Multiline Named-Arg `appendLine(...)` Literals

## Scope
- Extended regression fixtures in `scripts/test-check-hardcoded-ui-text-literals.sh` to cover commented multiline named-arg `appendLine(...)` literal detection in `buildAnnotatedString`.
- Added fail fixture cases with:
  - block-comment on call start line (`appendLine( /* ...`)
  - comment-only line before named-arg literal
  - inline comment on raw named-arg literal line

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
- `docs/evidence/har-383-test-check-hardcoded-ui-text-literals-20260505T125842Z.log`
- `docs/evidence/har-383-check-hardcoded-ui-text-literals-20260505T125842Z.log`
- `docs/evidence/har-383-option-regressions-runner-20260505T125842Z.log`
- `docs/evidence/har-383-compileDebugKotlin-20260505T125842Z.log`
- `docs/evidence/har-383-smoke-list-avds-20260505T125842Z.log`
- `docs/evidence/har-383-smoke-install-launch-20260505T125842Z.log`
