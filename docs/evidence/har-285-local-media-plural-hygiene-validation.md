# HAR-285 Local Media Plural Resource Hygiene: Validation Evidence

## Scope
- Isolated app-layer modernization in string resources for local-media scanning plural definitions.
- Removed unused `local_media_scanning` plural resources from all `values*/strings.xml` files.
- Normalized `local_media_scanning_files` plural quantities:
  - Added missing `many` quantity in `values-es`, `values-fr`, `values-it`, and `values-pt`.
  - Removed irrelevant `one` quantity for Chinese in `values-zh-rCN/local_media_strings.xml`.
- Removed irrelevant `one` quantity for Chinese `artist_detail_album_count` plural.

## Environment
- Date (UTC): 2026-05-03
- Local JDK used: Temurin 17 (`~/.local/jdks/temurin-17`)
- Emulator target: `Medium_Phone`

## Commands Run
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin

export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 180 --boot-timeout 240 --launch-wait 2 --keep-logs

./gradlew --no-daemon :app:lintDebug
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Emulator smoke command: passed on `emulator-5554`.
  - App install task: `:app:installDebug`
  - Launch verification: `com.harmonixia.android/.MainActivity` top resumed activity.
- `:app:lintDebug`: passed (`BUILD SUCCESSFUL`).
  - Local-media plural warnings addressed in this slice no longer appear.
  - Remaining lint warnings are dependency/toolchain update notices.

## Evidence Files
- `docs/evidence/har-285-compileDebugKotlin-20260503T102529Z.log`
- `docs/evidence/har-285-smoke-command-20260503T102541Z.log`
- `docs/evidence/har-285-emulator-Medium_Phone-1479300-18651.log`
- `docs/evidence/har-285-smoke-uninstall-1479300-18651.log`
- `docs/evidence/har-285-smoke-monkey-1479300-18651.log`
- `docs/evidence/har-285-lintDebug-20260503T102640Z.log`
