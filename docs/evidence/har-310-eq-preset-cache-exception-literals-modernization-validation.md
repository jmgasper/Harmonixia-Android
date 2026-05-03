# HAR-310 EQ Preset/Cache Exception Literals Modernization: Validation Evidence

## Scope
- Modernized hardcoded EQ preset/cache exception literals in:
  - `app/src/main/java/com/harmonixia/android/data/local/EqPresetCache.kt`
  - `app/src/main/java/com/harmonixia/android/data/repository/EqPresetRepositoryImpl.kt`
- Replaced inline exception messages with resource-backed strings.
- Added new string keys with locale-file parity across all current `values*` directories.
- Updated affected JVM tests for constructor/context changes:
  - `app/src/test/java/com/harmonixia/android/data/repository/EqPresetRepositoryTest.kt`
  - `app/src/test/java/com/harmonixia/android/data/local/EqPresetCacheIntegrationTest.kt`

## Added String Keys
- `eq_validation_opra_cache_missing`
- `eq_validation_opra_cache_empty`
- `eq_validation_opra_presets_empty`
- `eq_validation_opra_download_failed_http`
- `eq_validation_opra_response_body_empty`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "OPRA download failed|OPRA response body is empty|OPRA cache missing|OPRA cache is empty|OPRA presets are empty|Preset not found" \
  app/src/main/java/com/harmonixia/android/data/local/EqPresetCache.kt \
  app/src/main/java/com/harmonixia/android/data/repository/EqPresetRepositoryImpl.kt -S

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline literal scan for removed hardcoded exception messages in the two touched production files: no matches for targeted removed literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-310-compileDebugKotlin-20260503T183547Z.log`
- `docs/evidence/har-310-inline-eq-cache-scan-20260503T183547Z.log`
- `docs/evidence/har-310-smoke-command-20260503T183547Z.log`

## Next Action
- Follow-on modernization slice: resourceize remaining data-layer connection/validation literals, starting with `MusicAssistantWebSocketClient` (`"Server URL is not set"`) and then the highest-impact `MusicAssistantRepositoryImpl` literals.
