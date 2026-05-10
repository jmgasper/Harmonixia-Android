# HAR-315 Repository Response/Fallback Literals Modernization: Validation Evidence

## Scope
- Resourceized remaining high-impact repository fallback/response literals in:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Replaced hardcoded literals for:
  - Recently played album/playlist fetch failure fallbacks
  - Unexpected album/artist/playlist response validation
  - Playlist not found validation
- Added matching string keys to base and localized `values*/strings.xml` files to preserve key parity.

## Added String Keys
- `data_validation_recently_played_albums_load_failed`
- `data_validation_recently_played_playlists_load_failed`
- `data_validation_unexpected_album_response`
- `data_validation_unexpected_artist_response`
- `data_validation_unexpected_playlist_response`
- `playlist_validation_not_found`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "Failed to load recently played albums\\.|Failed to load recently played playlists\\.|Unexpected album response|Unexpected artist response|Unexpected playlist response|Playlist not found" \
  app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt -S

rg -n "data_validation_recently_played_albums_load_failed|data_validation_recently_played_playlists_load_failed|data_validation_unexpected_album_response|data_validation_unexpected_artist_response|data_validation_unexpected_playlist_response|playlist_validation_not_found" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline targeted literal scan in `MusicAssistantRepositoryImpl.kt`: no matches.
- Resource key parity scan: all six new keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-315-compileDebugKotlin-20260503T232709Z.log`
- `docs/evidence/har-315-inline-repository-validation-scan-20260503T232709Z.log`
- `docs/evidence/har-315-resource-key-parity-scan-20260503T232709Z.log`
- `docs/evidence/har-315-smoke-command-20260503T232709Z.log`

## Next Action
- Continue repository-layer literal modernization by replacing remaining hardcoded user-visible and validation fallback messages in `MusicAssistantRepositoryImpl` and adjacent data-layer classes.
