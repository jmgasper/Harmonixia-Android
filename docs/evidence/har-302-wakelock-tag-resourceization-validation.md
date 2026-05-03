# HAR-302 Playback Wake-Lock Tag Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackWakeLockController` and `PlaybackService`.
- Replaced hardcoded wake-lock tag literal usage with a derived tag built from app resources:
  - `"Harmonixia:PlaybackWakeLock"` -> `PlaybackWakeLockController.buildWakeLockTag(appName)`
  - `PlaybackService` now passes `getString(R.string.app_name)` into `PlaybackWakeLockController.create(...)`.
- Added unit coverage for tag derivation:
  - `PlaybackWakeLockControllerTest.buildWakeLockTag_usesAppNamePrefix`.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.service.playback.PlaybackWakeLockControllerTest
rg -n "Harmonixia:PlaybackWakeLock" app/src/main/java/com/harmonixia/android/service/playback -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:testDebugUnitTest` targeted suite: passed (`BUILD SUCCESSFUL`).
- Inline scan for `Harmonixia:PlaybackWakeLock` in playback service sources: no matches.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-302-testDebugUnitTest-20260503T144943Z.log`
- `docs/evidence/har-302-inline-wakelock-tag-scan-20260503T144943Z.log`
- `docs/evidence/har-302-smoke-command-20260503T144943Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize hardcoded notification channel display name `"Playback"` in `PlaybackNotificationManager` so channel UI text is localized while keeping the channel ID stable.
