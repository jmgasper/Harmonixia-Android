# HAR-296 Playback ViewModel Fallback Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PlaybackViewModel` by replacing hardcoded fallback UI strings with resource-backed messages.
- Added resourceized fallbacks for playback command and player-volume error events:
  - `playback_error_play`
  - `playback_error_pause`
  - `playback_error_skip`
  - `playback_error_seek`
  - `playback_error_repeat_mode`
  - `playback_error_shuffle_mode`
  - `playback_error_not_ready`
  - `playback_error_set_volume`
  - `playback_error_unmute_player`
  - `playback_error_update_mute`
- Replaced the hardcoded local player placeholder label `"Android Device"` with existing localized key `player_selection_this_device`.
- Added the new `playback_error_*` keys to all `values*/strings.xml` locale files to preserve translation-key parity.

## Environment
- Date (UTC): 2026-05-03
- Local JDK used: Temurin 17 (`~/.local/jdks/temurin-17`)

## Commands Run
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx3g :app:compileDebugKotlin

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.
- Playback fallback message paths now resolve through resource keys instead of inline English literals.

## Evidence Files
- `docs/evidence/har-296-compileDebugKotlin-20260503T122507Z.log`
- `docs/evidence/har-296-smoke-command-20260503T122422Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize user-visible custom action labels in playback-service custom actions (`Shuffle`/`Repeat`) to eliminate remaining inline English labels in Media3 controls.
