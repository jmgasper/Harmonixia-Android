# HAR-349 Validation - shared preview demo server URL constant

Timestamp (UTC): 20260504T180755Z

## Scope
- Added a shared preview constant in `ui/screens`:
  - `app/src/main/java/com/harmonixia/android/ui/screens/PreviewDefaults.kt`
- Replaced per-file preview URL constants in onboarding/settings preview files with `PREVIEW_DEMO_SERVER_URL`.
- Kept preview behavior unchanged.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-349-compileDebugKotlin-20260504T180755Z.log`
2. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-349-smoke-list-avds-20260504T180755Z.log`
3. `rg -n "PREVIEW_SERVER_URL|PREVIEW_DEMO_SERVER_URL|http://192\\.168\\.1\\.29:8095" app/src/main/java/com/harmonixia/android/ui/screens`
   - Result: PASS (only shared constant remains as literal source)
   - Log: `docs/evidence/har-349-preview-server-url-constant-scan-20260504T180755Z.log`

## Outcome
HAR-349 shared preview server URL constant migration is complete and validated.
