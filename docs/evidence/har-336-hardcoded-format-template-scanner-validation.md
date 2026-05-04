# HAR-336 Validation - Hardcoded format-template scanner

Timestamp (UTC): 20260504T114820Z

## Scope
- Added repository scanner script: `scripts/check-hardcoded-format-templates.sh`.
- Scanner checks Kotlin main sources for hardcoded formatting templates:
  - `String.format(..., "...%...")`
  - `"...%...".format(...)`
- Script fails the run when violations are detected and passes clean baselines.

## Validation Commands and Results
1. `./scripts/check-hardcoded-format-templates.sh`
   - Result: PASS (no hardcoded Kotlin format templates found in `app/src/main/java`)
   - Log: `docs/evidence/har-336-check-hardcoded-format-templates-20260504T114820Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-336-compileDebugKotlin-20260504T114820Z.log`
3. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-336-smoke-list-avds-20260504T114820Z.log`

## Outcome
HAR-336 scanner is in place and validated; it can now catch regressions introducing hardcoded Kotlin format templates in main source paths.
