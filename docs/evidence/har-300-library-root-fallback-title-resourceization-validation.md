# HAR-300 Library Root Fallback Title Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackSessionCallback.onGetLibraryRoot`.
- Replaced hardcoded fallback root title literal with localized app-name resource usage:
  - `"Harmonixia"` -> `context.getString(R.string.app_name)`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin
rg -n 'setTitle\("Harmonixia"\)' app/src/main/java/com/harmonixia/android/service/playback/PlaybackSessionCallback.kt
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan for `setTitle("Harmonixia")` in `PlaybackSessionCallback`: no matches.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-300-compileDebugKotlin-20260503T144039Z.log`
- `docs/evidence/har-300-inline-root-title-scan-20260503T144039Z.log`
- `docs/evidence/har-300-smoke-command-20260503T144039Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize the hardcoded wake-lock tag literal in `PlaybackWakeLockController` (`"Harmonixia:PlaybackWakeLock"`) by deriving its app prefix from resources/constants to avoid branded inline literals.
