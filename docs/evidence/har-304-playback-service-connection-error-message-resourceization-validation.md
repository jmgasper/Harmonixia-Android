# HAR-304 Playback Service Connection Error Message Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackServiceConnection`.
- Replaced hardcoded precondition failure messages used by `IllegalStateException`:
  - `"Not connected"` -> `R.string.playback_error_not_connected`
  - `"Queue ID unavailable"` -> `R.string.playback_error_queue_id_unavailable`
- Introduced helper failure builders in `PlaybackServiceConnection` to centralize resource-backed exceptions.
- Added both new string keys to base and localized `values*/strings.xml` resource sets.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"Not connected"|"Queue ID unavailable"' app/src/main/java/com/harmonixia/android/service/playback/PlaybackServiceConnection.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in `PlaybackServiceConnection.kt`: no matches for the removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-304-compileDebugKotlin-20260503T160020Z.log`
- `docs/evidence/har-304-inline-connection-message-scan-20260503T160020Z.log`
- `docs/evidence/har-304-smoke-command-20260503T160020Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize repeated playback precondition failure literals in domain playback use-cases (`PlayAlbumUseCase`, `PlayPlaylistUseCase`, `PlayTrackUseCase`, `ControlPlaybackUseCase`) to keep runtime error messages locale-ready and consistent.
