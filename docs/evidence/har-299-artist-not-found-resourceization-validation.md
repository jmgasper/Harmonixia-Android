# HAR-299 Artist Not Found Fallback Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `ArtistDetailViewModel`.
- Replaced hardcoded "Artist not found." fallback strings with localized resource usage:
  - `R.string.now_playing_artist_not_found`
- Added `@ApplicationContext` injection for resource resolution in the view model.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin
rg -n "Artist not found\\." app/src/main/java/com/harmonixia/android/ui/screens/artists/ArtistDetailViewModel.kt
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan for hardcoded `"Artist not found."` literal: no matches.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-299-compileDebugKotlin-20260503T143737Z.log`
- `docs/evidence/har-299-inline-artist-fallback-scan-20260503T143737Z.log`
- `docs/evidence/har-299-smoke-command-20260503T143737Z.log`
