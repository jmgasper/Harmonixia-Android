# HAR-315 — Artist Detail Validation Message Resourceization

## Summary
- Replaced the remaining hardcoded validation fallback in `ArtistDetailViewModel`.
- Updated invalid-args handling (`artistId`/`provider` blank) to use a localized resource-backed message instead of inline English text.
- Kept scope intentionally narrow to one production file for an isolated check-in.

## Production Change
- File:
  - `app/src/main/java/com/harmonixia/android/ui/screens/artists/ArtistDetailViewModel.kt`
- Change:
  - Replaced `ArtistDetailUiState.Error("Missing artist details.")`
  - With `ArtistDetailUiState.Error(context.getString(R.string.now_playing_artist_not_found))`

## Why
- Removes a remaining user-facing hardcoded literal from production flow.
- Keeps artist-detail failure messaging aligned with existing localized string resources.

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

## Evidence Files
- `docs/evidence/har-315-compileDebugKotlin-20260503T221506Z.log`
- `docs/evidence/har-315-smoke-list-avds-20260503T221506Z.log`
- `docs/evidence/har-315-smoke-install-launch-20260503T221506Z.log`
