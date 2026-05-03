# HAR-287 EQ Preset Details Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `PresetDetailsCard` (`EqPresetDetails.kt`) by replacing hardcoded detail labels/messages with string resources.
- Added dedicated EQ detail resource keys (`eq_detail_*`) and applied them in all `values*/strings.xml` locale files to avoid translation-key drift.
- Converted detail and filter display text to format resources (`eq_detail_line_format`, `eq_detail_filter_line`) to remove inline UI string assembly.

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
- No missing-key failures from the new `eq_detail_*` resource additions.

## Evidence Files
- `docs/evidence/har-287-compileDebugKotlin-20260503T103601Z.log`
- `docs/evidence/har-287-lintDebug-20260503T103624Z.log`
