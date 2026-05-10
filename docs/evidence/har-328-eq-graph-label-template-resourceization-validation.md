# HAR-328 Validation - EqGraph label template resourceization

Timestamp (UTC): 20260504T080422Z

## Scope
- Resourceized hardcoded EqGraph axis label templates for frequency and gain labels.
- Extracted label formatting into `EqGraphLabelFormatter` to enable JVM unit tests.
- Kept visual labeling behavior unchanged while sourcing templates from string resources.
- Added locale key parity for new EqGraph label format keys across all target `values-*` files.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.ui.components.EqGraphLabelFormatterTest`
   - Result: PASS
   - Log: `docs/evidence/har-328-test-eq-graph-label-formatter-20260504T080422Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-328-compileDebugKotlin-20260504T080422Z.log`
3. Inline scan across `EqGraph.kt` and `EqGraphLabelFormatter.kt`
   - Result: PASS (hardcoded label templates removed; `eq_graph_*` format resources used)
   - Log: `docs/evidence/har-328-inline-eq-graph-label-scan-20260504T080422Z.log`
4. Resource key parity scan across target locale files
   - Result: PASS (`eq_graph_frequency_integer_k_format`, `eq_graph_frequency_decimal_k_format`, `eq_graph_gain_positive_format`, `eq_graph_gain_default_format` present in all 13 target files)
   - Log: `docs/evidence/har-328-resource-key-parity-scan-20260504T080422Z.log`
5. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-328-smoke-list-avds-20260504T080422Z.log`

## Outcome
HAR-328 scoped checks passed with EqGraph axis labels now resource/template backed and unit-testable.
