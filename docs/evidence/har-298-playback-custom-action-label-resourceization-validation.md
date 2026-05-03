# HAR-298 Playback Custom Action Label Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackService` Media3 media-button preferences.
- Replaced hardcoded custom action display labels:
  - `"Shuffle"` -> `R.string.action_shuffle`
  - `"Repeat"` -> `R.string.action_repeat`
- Added `action_repeat` string key across all existing `values*/strings.xml` locale files to preserve translation-key parity.

## Environment
- Date (UTC): 2026-05-03
- Local JDK used: Temurin 17 (`~/.local/jdks/temurin-17`)

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin
rg -n 'setDisplayName\("(Shuffle|Repeat)"\)' app/src/main/java/com/harmonixia/android/service/playback/PlaybackService.kt
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline label scan returned no matches for hardcoded `setDisplayName("Shuffle")`/`setDisplayName("Repeat")`.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-298-compileDebugKotlin-20260503T133223Z.log`
- `docs/evidence/har-298-inline-label-scan-20260503T133223Z.log`
- `docs/evidence/har-298-smoke-command-20260503T133223Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize the fallback root title literal `"Harmonixia"` in `PlaybackSessionCallback.onGetLibraryRoot` (failure path) by wiring it to `R.string.app_name`.
