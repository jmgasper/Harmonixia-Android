# HAR-317 Recommendation + WebSocket Fallback Modernization: Validation Evidence

## Scope
- Completed recommendation fallback title resourceization in:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Completed websocket server-error fallback resourceization in:
  - `app/src/main/java/com/harmonixia/android/data/remote/MusicAssistantWebSocketClient.kt`
- Added and parity-synced resource keys across base + localized `values*/strings.xml`.

## Added String Keys
- `recommendation_section_default_title`
- `recommendation_fallback_title`
- `recommendation_fallback_unknown_album_title`
- `recommendation_fallback_untitled_playlist_title`
- `recommendation_fallback_unknown_artist_title`
- `recommendation_fallback_unknown_track_title`
- `connection_validation_server_error_with_code`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "Unknown Album|Unknown Artist|Unknown Track|Untitled Playlist|Server error \\\($errorCode\\\)" \
  app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt \
  app/src/main/java/com/harmonixia/android/data/remote/MusicAssistantWebSocketClient.kt -S

rg -n "recommendation_section_default_title|recommendation_fallback_title|recommendation_fallback_unknown_album_title|recommendation_fallback_untitled_playlist_title|recommendation_fallback_unknown_artist_title|recommendation_fallback_unknown_track_title|connection_validation_server_error_with_code" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline targeted literal scan: no matches.
- Resource key parity scan: all seven keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-317-compileDebugKotlin-20260504T014830Z.log`
- `docs/evidence/har-317-inline-recommendation-websocket-scan-20260504T014830Z.log`
- `docs/evidence/har-317-resource-key-parity-scan-20260504T014830Z.log`
- `docs/evidence/har-317-smoke-command-20260504T014830Z.log`

## Next Action
- Continue incremental app/data literal modernization with the next bounded user-visible fallback message slice.
