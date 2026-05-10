# HAR-303 Playback Notification Channel Name Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackNotificationManager`.
- Replaced hardcoded notification channel display name literal:
  - `"Playback"` -> `context.getString(R.string.playback_notification_channel_name)`.
- Added `playback_notification_channel_name` string resource to base and localized `values*/strings.xml` resource sets.
- Kept notification channel ID stable (`harmonixia_playback`) to avoid channel migration regressions.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin
rg -n 'NOTIFICATION_CHANNEL_NAME|"Playback"' app/src/main/java/com/harmonixia/android/service/playback/PlaybackNotificationManager.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in `PlaybackNotificationManager.kt`:
  - no `NOTIFICATION_CHANNEL_NAME` constant remains,
  - no hardcoded `"Playback"` channel display literal remains.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-303-compileDebugKotlin-20260503T155512Z.log`
- `docs/evidence/har-303-inline-channel-name-scan-20260503T155512Z.log`
- `docs/evidence/har-303-smoke-command-20260503T155512Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize repeated playback service connection failure messages (`"Not connected"`, `"Queue ID unavailable"`) in `PlaybackServiceConnection` by introducing string resources and wiring usages through `context.getString(...)`.
