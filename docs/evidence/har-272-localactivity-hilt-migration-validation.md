# HAR-272 LocalActivity/Hilt Import Migration: Validation Evidence

## Run metadata
- Date (UTC): 2026-05-03T04:13:18Z
- Repository: `/home/jmgasper/Documents/Git/Harmonixia-Android`
- Scope: LocalActivity migration + `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` import alignment across remaining Compose screens

## Environment preparation
- Set `JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17`
- Verified Java: `openjdk version "17.0.19" 2026-04-21`
- Added Android SDK tools to `PATH`:
  - `/home/jmgasper/Android/Sdk/platform-tools`
  - `/home/jmgasper/Android/Sdk/emulator`

## Commands executed
```bash
export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:compileDebugKotlin

export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:/home/jmgasper/Android/Sdk/platform-tools:/home/jmgasper/Android/Sdk/emulator:$PATH"
scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 180 --boot-timeout 240 --launch-wait 2 --keep-logs
```

## Results
- Compile gate passed:
  - `:app:compileDebugKotlin`
  - `BUILD SUCCESSFUL`
- Emulator smoke passed:
  - Target AVD: `Medium_Phone`
  - Serial: `emulator-5554`
  - App launch verified for `com.harmonixia.android/.MainActivity`
  - Script output: `Smoke test passed`

## Smoke artifacts copied into repo
- `docs/evidence/har-272-emulator-Medium_Phone-1062496-1138.log`
- `docs/evidence/har-272-smoke-uninstall-1062496-1138.log`
- `docs/evidence/har-272-smoke-monkey-1062496-1138.log`

## Commit context
- Existing migration commit in branch history: `7ceca46` (`ui: replace Activity casts with LocalActivity in Compose`)
- This validation run was executed against the current workspace state containing the remaining HAR-272 migration deltas.
