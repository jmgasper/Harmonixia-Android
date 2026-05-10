# HAR-291 Android Auto Category Localization Slice: Validation Evidence

## Scope
- Isolated app-layer modernization in `MediaLibraryBrowser` category title handling for Android Auto browse metadata.
- Replaced selected hardcoded English category titles with existing localized app string resources:
  - app root title (`app_name`)
  - Home (`nav_home`)
  - Albums (`nav_albums`)
  - Artists (`nav_artists`)
  - Playlists (`nav_playlists`)
  - Local Media (`section_local_media`)
  - Recently Played (`home_recently_played`)
  - New Albums section label mapped to existing `home_recently_added`
- Kept favorites and local subcategory (`Local Albums/Artists/Tracks`) labels unchanged in this slice to keep risk and review scope narrow.

## Environment
- Date (UTC): 2026-05-03
- Local JDK used: Temurin 17 (`~/.local/jdks/temurin-17`)

## Commands Run
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin

export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.service.playback.MediaLibraryBrowserCategoryMetadataTest

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Targeted unit test gate:
  `:app:testDebugUnitTest --tests com.harmonixia.android.service.playback.MediaLibraryBrowserCategoryMetadataTest`: passed (`BUILD SUCCESSFUL`).
- Simulator smoke proof: `scripts/smoke-debug-emulator.sh --list-avds` returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-291-compileDebugKotlin-20260503T110031Z.log`
- `docs/evidence/har-291-testDebugUnitTest-MediaLibraryBrowserCategoryMetadataTest-20260503T110031Z.log`
- `docs/evidence/har-291-smoke-command-20260503T120331Z.log`

## Next Action
- Follow-on modernization slice should resourceize the remaining hardcoded Android Auto labels:
  - `Favourites`
  - `Local Albums`
  - `Local Artists`
  - `Local Tracks`
  while preserving translation-key parity across all locale files.
