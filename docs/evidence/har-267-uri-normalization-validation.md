# HAR-267 URI Normalization Slice: Validation Evidence

## Run metadata
- Date (UTC): 2026-05-03T01:38:50Z
- Repository: `/home/jmgasper/Documents/Git/Harmonixia-Android`
- Scope: Media browser URI normalization extraction + focused tests

## Environment preparation
- Set `JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17` (OpenJDK 17.0.19)
- Added SDK tools to `PATH` for emulator smoke:
  - `/home/jmgasper/Android/Sdk/platform-tools`
  - `/home/jmgasper/Android/Sdk/emulator`

## Commands executed
```bash
export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests "com.harmonixia.android.util.MediaBrowserUriNormalizerTest"

export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:compileDebugKotlin

export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:/home/jmgasper/Android/Sdk/platform-tools:/home/jmgasper/Android/Sdk/emulator:$PATH"
scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 180 --boot-timeout 240 --launch-wait 2 --keep-logs
```

## Results
- Focused unit test class passed:
  - `:app:testDebugUnitTest --tests com.harmonixia.android.util.MediaBrowserUriNormalizerTest`
  - `BUILD SUCCESSFUL`
- Compile gate passed:
  - `:app:compileDebugKotlin`
  - `BUILD SUCCESSFUL`
- Emulator smoke passed on `emulator-5554`:
  - APK installed via `:app:installDebug`
  - App launch verified (`com.harmonixia.android/.MainActivity`)
  - `Smoke test passed`

## Smoke artifacts copied into repo
- `docs/evidence/har-267-emulator-Medium_Phone-946913-6556.log`
- `docs/evidence/har-267-smoke-uninstall-946913-6556.log`
- `docs/evidence/har-267-smoke-monkey-946913-6556.log`
