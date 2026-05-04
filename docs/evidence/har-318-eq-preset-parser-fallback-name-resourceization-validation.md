# HAR-318 EqPresetParser Fallback Name Resourceization: Validation Evidence

## Scope
- Resourceized the hardcoded fallback preset name in:
  - `app/src/main/java/com/harmonixia/android/data/local/EqPresetParser.kt`
- Injected application context into `EqPresetParser` and resolved fallback through:
  - `R.string.eq_fallback_preset_name`
- Updated DI wiring and parser tests to match constructor/resource behavior.
- Added `eq_fallback_preset_name` key across base + localized `values*/strings.xml` files.

## Added String Key
- `eq_fallback_preset_name`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "\"EQ Preset\"" app/src/main/java/com/harmonixia/android/data/local/EqPresetParser.kt -S

rg -n "eq_fallback_preset_name" app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline targeted scan in `EqPresetParser.kt`: no hardcoded `"EQ Preset"` matches.
- Resource key parity scan: `eq_fallback_preset_name` present in base + localized `values*/strings.xml`.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-318-compileDebugKotlin-20260504T025916Z.log`
- `docs/evidence/har-318-inline-eq-preset-parser-scan-20260504T025916Z.log`
- `docs/evidence/har-318-resource-key-parity-scan-20260504T025916Z.log`
- `docs/evidence/har-318-smoke-command-20260504T025916Z.log`

## Next Action
- Continue bounded literal modernization with the next confirmed user-visible fallback literal in production code paths.
