# HAR-311 Data-Layer Validation Literals Modernization: Validation Evidence

## Scope
- Modernized remaining high-impact data-layer validation/connection literals in:
  - `app/src/main/java/com/harmonixia/android/data/remote/MusicAssistantWebSocketClient.kt`
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Replaced hardcoded validation strings with resource-backed lookups.
- Added `@ApplicationContext Context` injection where required and updated DI/test wiring:
  - `app/src/main/java/com/harmonixia/android/di/DataModule.kt`
  - `app/src/test/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImplTest.kt`
- Added new validation keys with locale-file parity across all current `values*` directories.

## Added String Keys
- `connection_validation_server_url_not_set`
- `track_validation_metadata_required`
- `track_validation_item_id_required`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "Server URL is not set|Playlist details are required|Media URI is required|Missing track metadata for playback reporting|Playlist id is required|Missing track metadata for favorites|Track item id is required" \
  app/src/main/java/com/harmonixia/android/data/remote/MusicAssistantWebSocketClient.kt \
  app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt -S

rg -n "connection_validation_server_url_not_set|track_validation_metadata_required|track_validation_item_id_required" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline literal scan in scoped data-layer files: no matches for targeted removed hardcoded literals.
- Resource key parity scan: new keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-311-compileDebugKotlin-20260503T194603Z.log`
- `docs/evidence/har-311-inline-data-validation-scan-20260503T194603Z.log`
- `docs/evidence/har-311-resource-key-parity-scan-20260503T194603Z.log`
- `docs/evidence/har-311-smoke-command-20260503T194603Z.log`

## Next Action
- Follow-on data-layer modernization slice: resourceize remaining local media scanner/WebSocket fallback literals that still expose validation-style messages (`LocalMediaScanner` and any remaining user-facing connection fallback strings), then run a focused `:app:compileDebugKotlin` pass with scoped literal scans.
