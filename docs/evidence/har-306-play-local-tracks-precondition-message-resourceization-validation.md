# HAR-306 PlayLocalTracks Precondition Message Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlayLocalTracksUseCase`.
- Replaced hardcoded precondition failure literals with resource-backed messages:
  - `"No tracks to play"` -> `R.string.playback_error_no_tracks_to_play`
  - `"No player selected"` -> `R.string.playback_error_no_player_selected`
  - `"No active queue"` -> `R.string.playback_error_no_active_queue`
  - `"Track URI is required"` -> `R.string.playback_error_track_uri_required`
- Updated DI wiring in `UseCaseModule` to pass `@ApplicationContext` into `PlayLocalTracksUseCase`.
- Added new base/localized string key: `playback_error_no_tracks_to_play`.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"No tracks to play"|"No player selected"|"No active queue"|"Track URI is required"' app/src/main/java/com/harmonixia/android/domain/usecase/PlayLocalTracksUseCase.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in `PlayLocalTracksUseCase.kt`: no matches for targeted removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-306-compileDebugKotlin-20260503T160750Z.log`
- `docs/evidence/har-306-inline-local-tracks-message-scan-20260503T160750Z.log`
- `docs/evidence/har-306-smoke-command-20260503T160750Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize playlist validation/precondition literals in playlist domain use-cases (`ManagePlaylistTracksUseCase`, `RenamePlaylistUseCase`, `DeletePlaylistUseCase`) and add corresponding string keys with locale parity.
