# HAR-307 Playlist Domain Validation Message Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in playlist domain use-cases:
  - `ManagePlaylistTracksUseCase`
  - `RenamePlaylistUseCase`
  - `DeletePlaylistUseCase`
- Replaced hardcoded playlist validation/precondition literals with resource-backed messages.
- Added `Context` wiring where needed and ensured DI provider parity in `UseCaseModule`.
- Added required playlist validation string keys to base + localized `values*/strings.xml` sets.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"Playlist is not editable"|"Playlist id is required"|"Track uri is required"|"Track position is required"|"Playlist details are required"|"Playlist name is required"' \
  app/src/main/java/com/harmonixia/android/domain/usecase/ManagePlaylistTracksUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/RenamePlaylistUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/DeletePlaylistUseCase.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`, up-to-date).
- Inline scan in scoped playlist use-cases: no matches for targeted removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-307-compileDebugKotlin-20260503T172000Z.log`
- `docs/evidence/har-307-inline-playlist-validation-scan-20260503T172000Z.log`
- `docs/evidence/har-307-smoke-command-20260503T172000Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize input validation literals in `ConnectToServerUseCase` (`Server URL cannot be empty/invalid`, `Username cannot be empty`, `Password cannot be empty`) with locale parity across `values*/strings.xml`.
