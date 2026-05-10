# HAR-347 — Preview Connection Status Resourceization

## Summary
- Replaced remaining hardcoded connection-status preview literals in onboarding/settings preview surfaces.
- Switched preview error/success messages to existing localized string resources.

## Production Change
- Files:
  - `app/src/main/java/com/harmonixia/android/ui/screens/onboarding/OnboardingScreenPreview.kt`
  - `app/src/main/java/com/harmonixia/android/ui/screens/settings/SettingsScreenPreview.kt`
- Changes:
  - Added `stringResource` + `R` imports in both files.
  - Replaced preview literals:
    - `"Connection failed"` -> `stringResource(R.string.status_connection_failed)`
    - `"Connected successfully"` -> `stringResource(R.string.status_connected)`
  - Reused these localized values for both `message` and `ConnectionState.Error(...)` preview payloads.

## Why
- Keeps preview content aligned with production localized status strings.
- Removes remaining inline English status text from Compose preview data.

## Validation
- Compile:
  - `./gradlew --no-daemon :app:compileDebugKotlin`
  - Result: `BUILD SUCCESSFUL`
- Simulator availability:
  - `scripts/smoke-debug-emulator.sh --list-avds`
  - Result: `Medium_Phone`
- Emulator install/launch smoke:
  - `scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
  - Result: `Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`.
- Targeted literal scan:
  - No matches for removed preview literals (`Connection failed`, `Connected successfully`).

## Evidence Files
- `docs/evidence/har-347-compileDebugKotlin-20260504T165846Z.log`
- `docs/evidence/har-347-smoke-list-avds-20260504T165846Z.log`
- `docs/evidence/har-347-smoke-install-launch-20260504T165846Z.log`
- `docs/evidence/har-347-inline-preview-connection-literals-scan-20260504T165846Z.log`
