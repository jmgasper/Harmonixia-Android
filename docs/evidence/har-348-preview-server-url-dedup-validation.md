# HAR-348 Validation - preview server URL literal dedup

Timestamp (UTC): 20260504T180519Z

## Scope
- Replaced repeated preview URL literals in onboarding/settings preview files with a shared `PREVIEW_SERVER_URL` constant per file.
- No runtime behavior changes; preview inputs remain the same URL value.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-348-compileDebugKotlin-20260504T180519Z.log`
2. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone`)
   - Log: `docs/evidence/har-348-smoke-list-avds-20260504T180519Z.log`
3. `rg -n "http://192\\.168\\.1\\.29:8095" app/src/main/java/com/harmonixia/android/ui/screens/onboarding/OnboardingScreenPreview.kt app/src/main/java/com/harmonixia/android/ui/screens/settings/SettingsScreenPreview.kt`
   - Result: PASS (literal appears only in file-level constants)
   - Log: `docs/evidence/har-348-preview-server-url-literal-scan-20260504T180519Z.log`

## Outcome
HAR-348 preview URL literal dedup is complete and validated.
