# HAR-327 Validation - EqPresetDetails value template resourceization

Timestamp (UTC): 20260504T075929Z

## Scope
- Resourceized hardcoded EQ value templates used by `EqPresetDetails` (`kHz`, `Hz`, `dB`, and Q precision layout).
- Extracted formatting into `EqPresetValueFormatter` helper to support JVM tests without Compose runtime coupling.
- Kept displayed value behavior equivalent while sourcing templates from string resources.
- Added locale key parity for new EQ value format keys across all `values-*` files containing EQ detail strings.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.ui.components.EqPresetValueFormatterTest`
   - Result: PASS
   - Log: `docs/evidence/har-327-test-eq-preset-value-formatter-20260504T075929Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-327-compileDebugKotlin-20260504T075929Z.log`
3. Inline scan across `EqPresetDetails.kt` and `EqPresetValueFormatter.kt`
   - Result: PASS (hardcoded EQ value templates removed; new `eq_value_*` resource keys used)
   - Log: `docs/evidence/har-327-inline-eq-value-formatter-scan-20260504T075929Z.log`
4. Resource key parity scan across target locale files
   - Result: PASS (`eq_value_frequency_khz_format`, `eq_value_frequency_hz_format`, `eq_value_gain_db_format`, `eq_value_q_format` present in all 13 target files)
   - Log: `docs/evidence/har-327-resource-key-parity-scan-20260504T075929Z.log`
5. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-327-smoke-list-avds-20260504T075929Z.log`

## Outcome
HAR-327 scoped checks passed with EQ value formatting now template/resource backed and unit-testable.
