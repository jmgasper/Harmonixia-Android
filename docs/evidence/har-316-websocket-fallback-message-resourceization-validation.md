# HAR-316 — WebSocket Fallback Message Resourceization

## Summary
- Resourceized remaining hardcoded connection/auth fallback messages in `MusicAssistantWebSocketClient` using existing localized status/error keys.
- Kept scope to one production file for an isolated check-in.

## Production Change
- File:
  - `app/src/main/java/com/harmonixia/android/data/remote/MusicAssistantWebSocketClient.kt`
- Replacements:
  - `"WebSocket failure"` -> `context.getString(R.string.status_connection_failed)`
  - `"Connection timeout"` -> `context.getString(R.string.error_connection_timeout)`
  - `"Disconnected"` -> `context.getString(R.string.status_disconnected)`
  - `"Authentication failed. Please update your token."` -> `context.getString(R.string.status_auth_failed)` via `authErrorMessage()` helper.

## Why
- Removes remaining hardcoded user-visible fallback literals in the WebSocket connection path.
- Ensures failure/status messaging follows existing localization coverage.

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
  - No matches for removed hardcoded WebSocket fallback literals.

## Evidence Files
- `docs/evidence/har-316-compileDebugKotlin-20260503T221751Z.log`
- `docs/evidence/har-316-smoke-list-avds-20260503T221751Z.log`
- `docs/evidence/har-316-smoke-install-launch-20260503T221751Z.log`
- `docs/evidence/har-316-inline-websocket-fallback-scan-20260503T221751Z.log`
