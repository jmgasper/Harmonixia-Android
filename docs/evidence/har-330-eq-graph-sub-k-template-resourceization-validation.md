# HAR-330 Validation - EqGraph sub-kHz label template resourceization

Timestamp (UTC): 20260504T092015Z

## Scope
- Resourceized EqGraph sub-kHz frequency label fallback by routing through `eq_graph_frequency_sub_k_format`.
- Ensured `EqGraphLabelFormatter` uses template formatting for the `< 1000Hz` path.
- Kept locale parity for `eq_graph_frequency_sub_k_format` across translated `values*/strings.xml` files.
- Preserved formatter behavior with unit coverage for sub-kHz labels.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:testDebugUnitTest --tests com.harmonixia.android.ui.components.EqGraphLabelFormatterTest`
   - Result: PASS
   - Log: `docs/evidence/har-330-test-eq-graph-label-formatter-20260504T092015Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-330-compileDebugKotlin-20260504T092015Z.log`
3. Inline scan across `EqGraph.kt`, `EqGraphLabelFormatter.kt`, and `EqGraphLabelFormatterTest.kt`
   - Result: PASS (sub-kHz path uses template, no `freq.toString()` fallback)
   - Log: `docs/evidence/har-330-inline-eq-graph-sub-k-scan-20260504T092015Z.log`
4. Resource key parity scan across target locale files
   - Result: PASS (`eq_graph_frequency_sub_k_format` present in all target files)
   - Log: `docs/evidence/har-330-resource-key-parity-scan-20260504T092015Z.log`
5. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-330-smoke-list-avds-20260504T092015Z.log`

## Outcome
HAR-330 scoped checks passed. EqGraph sub-kHz label formatting is template/resource backed and validated.
