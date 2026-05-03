# HAR-292 Android Auto Local Label Resourceization: Validation Evidence

## Scope
- Replaced remaining hardcoded Android Auto browse labels in `MediaLibraryBrowser` with string resources:
  - `home_favorites`
  - `section_local_albums`
  - `section_local_artists`
  - `section_local_tracks`
- Added these keys across all existing `values*` locale string files to preserve locale-key parity.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:testDebugUnitTest --tests com.harmonixia.android.service.playback.MediaLibraryBrowserCategoryMetadataTest
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- `:app:testDebugUnitTest --tests com.harmonixia.android.service.playback.MediaLibraryBrowserCategoryMetadataTest`: passed (`BUILD SUCCESSFUL`).
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-292-compileDebugKotlin-20260503T120847Z.log`
- `docs/evidence/har-292-testDebugUnitTest-MediaLibraryBrowserCategoryMetadataTest-20260503T120847Z.log`
- `docs/evidence/har-292-smoke-command-20260503T120847Z.log`
