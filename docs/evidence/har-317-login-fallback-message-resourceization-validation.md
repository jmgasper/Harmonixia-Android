# HAR-317 — Login Fallback Message Resourceization

## Summary
- Resourceized remaining hardcoded login fallback/error literals in `MusicAssistantRepositoryImpl.loginWithCredentials`.
- Reused existing localized string keys only; no new resource keys added.

## Production Change
- File:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Replacements:
  - auth fallback (`"Invalid username or password"`) -> `R.string.status_auth_failed`
  - timeout fallback (`"Connection timeout. Please check your network."`) -> `R.string.error_connection_timeout`
  - invalid/failed response fallbacks (`"Invalid server response"`) -> `R.string.error_unknown`
  - generic connection fallback (`"Cannot connect to server. Please check the URL."` and generic login fallback) -> `R.string.status_connection_failed`

## Why
- Removes additional user-visible hardcoded login fallback text from production flow.
- Aligns credential/login error handling with existing localized UI status messages.

## Validation
- Compile:
  - `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH ./gradlew :app:compileDebugKotlin`
  - Result: `BUILD SUCCESSFUL`
- Simulator availability:
  - `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH scripts/smoke-debug-emulator.sh --list-avds`
  - Result: `Medium_Phone`
- Emulator install/launch smoke:
  - `JAVA_HOME=$HOME/.local/jdks/temurin-17 PATH=$HOME/.local/jdks/temurin-17/bin:$PATH scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
  - Result: `Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`.
- Targeted literal scan:
  - No matches for removed login fallback literals.

## Evidence Files
- `docs/evidence/har-317-compileDebugKotlin-20260503T222045Z.log`
- `docs/evidence/har-317-smoke-list-avds-20260503T222045Z.log`
- `docs/evidence/har-317-smoke-install-launch-20260503T222045Z.log`
- `docs/evidence/har-317-inline-login-fallback-scan-20260503T222045Z.log`
