# HAR-388 Validation - detect inline block-comment named-arg literals

Validation window (UTC): 20260505T151747Z - 20260505T151747Z

## Scope
- Updated `scripts/check-hardcoded-ui-text-literals.sh` regexes for:
  - `text = ...`
  - `contentDescription = ...`
  - `AnnotatedString(text = ...)`
  so hardcoded literals are detected even when an inline block comment appears between `=` and the literal on the same line.
- Expanded `scripts/test-check-hardcoded-ui-text-literals.sh` fixtures to cover:
  - `Text(text = /* ... */ "literal")`
  - `Icon(... contentDescription = /* ... */ "literal")`
  - `AnnotatedString(text = /* ... */ "literal")`
  plus non-literal pass variants.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-388-test-check-hardcoded-ui-text-literals-20260505T151747Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-388-check-hardcoded-ui-text-literals-20260505T151747Z.log`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
   - Result: PASS
   - Log: `docs/evidence/har-388-option-regressions-runner-20260505T151747Z.log`
4. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: `BUILD SUCCESSFUL`
   - Log: `docs/evidence/har-388-compileDebugKotlin-20260505T151747Z.log`
5. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-388-smoke-list-avds-20260505T151747Z.log`
6. `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
   - Result: PASS (`Smoke test passed`, app resumed on `emulator-5554`)
   - Log: `docs/evidence/har-388-smoke-install-launch-20260505T151747Z.log`

## Outcome
HAR-388 closes same-line inline block-comment bypasses for named argument literals and strengthens scanner regression coverage for these forms.
