# HAR-289 Settings Local Media Snackbar Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `SettingsViewModel` by replacing two hardcoded local-media snackbar messages with string resources.
- Converted these messages to resource IDs:
  - `message_local_media_folder_updated`
  - `message_local_media_select_folder_first`
- Added both keys to all `values*/strings.xml` locale files to keep translation-key parity.

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
./gradlew --no-daemon :app:lintDebug
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- `:app:lintDebug`: passed (`BUILD SUCCESSFUL`).
- Resource lookup remains valid after replacing hardcoded snackbar text with localized keys.

## Evidence Files
- `docs/evidence/har-289-compileDebugKotlin-20260503T104808Z.log`
- `docs/evidence/har-289-lintDebug-20260503T104831Z.log`
