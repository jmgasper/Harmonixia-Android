# HAR-305 Playback Domain Precondition Message Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization across playback domain use-cases:
  - `PlayAlbumUseCase`
  - `PlayPlaylistUseCase`
  - `PlayTrackUseCase`
  - `ControlPlaybackUseCase`
- Replaced hardcoded precondition failure literals with resource-backed messages via `context.getString(...)`.
- Updated DI wiring in `UseCaseModule` to provide `@ApplicationContext` into these use-cases.
- Added six new playback error string keys to base and localized `values*/strings.xml` files:
  - `playback_error_no_player_selected`
  - `playback_error_no_active_queue`
  - `playback_error_album_has_no_tracks`
  - `playback_error_playlist_has_no_tracks`
  - `playback_error_track_uri_required`
  - `playback_error_seek_position_required`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"No player selected"|"No active queue"|"Album has no tracks"|"Playlist has no tracks"|"Track URI is required"|"Position required for SEEK"' \
  app/src/main/java/com/harmonixia/android/domain/usecase/PlayAlbumUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/PlayPlaylistUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/PlayTrackUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/ControlPlaybackUseCase.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in scoped use-cases: no matches for targeted removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-305-compileDebugKotlin-20260503T160517Z.log`
- `docs/evidence/har-305-inline-precondition-message-scan-20260503T160517Z.log`
- `docs/evidence/har-305-smoke-command-20260503T160517Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize remaining playback precondition literals in `PlayLocalTracksUseCase` (`"No tracks to play"`, `"No player selected"`, `"No active queue"`, `"Track URI is required"`) and reuse existing shared playback error keys where applicable.
