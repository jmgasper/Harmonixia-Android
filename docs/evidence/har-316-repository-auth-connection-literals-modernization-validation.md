# HAR-316 Repository Auth/Connection Literals Modernization: Validation Evidence

## Scope
- Resourceized remaining hardcoded authentication/connection validation literals in:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Updated `deletePlaylist()` network error recovery to use resource-backed messages for:
  - Authentication failure token update prompt
  - Server connection failure prompt
- Added new string keys to base and localized `values*/strings.xml` files to preserve locale key parity.

## Added String Keys
- `data_validation_authentication_failed_update_token`
- `data_validation_unable_to_connect_to_server`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "Authentication failed\\. Please update your token\\.|Unable to connect to server\\." \
  app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt -S

rg -n "data_validation_authentication_failed_update_token|data_validation_unable_to_connect_to_server" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline targeted literal scan in `MusicAssistantRepositoryImpl.kt`: no matches.
- Resource key parity scan: both new keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-316-compileDebugKotlin-20260503T233359Z.log`
- `docs/evidence/har-316-inline-repository-auth-connection-scan-20260503T233359Z.log`
- `docs/evidence/har-316-resource-key-parity-scan-20260503T233359Z.log`
- `docs/evidence/har-316-smoke-command-20260503T233359Z.log`

## Next Action
- Continue repository/domain modernization by resourceizing the next remaining hardcoded user-facing validation/fallback literals outside this auth/connection slice.
