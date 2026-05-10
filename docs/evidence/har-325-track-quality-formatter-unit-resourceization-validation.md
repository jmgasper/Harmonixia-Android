# HAR-325 Validation - TrackQualityFormatter unit resourceization

Timestamp (UTC): 20260504T064251Z

## Scope
- Resourceized hardcoded unit literals in `TrackQualityFormatter`: `kHz`, `kbps`, and `bit`.
- Kept `formatTrackQualityLabel` call-site API unchanged.
- Added targeted formatter unit tests to verify resolver-provided unit strings are used.
- Added locale key parity for unit resources across all track-quality locale files.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.ui.components.TrackQualityFormatterTest`
   - Result: PASS
   - Log: `docs/evidence/har-325-test-track-quality-formatter-20260504T064251Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-325-compileDebugKotlin-20260504T064251Z.log`
3. Inline scan in `TrackQualityFormatter.kt`
   - Result: PASS (hardcoded unit literals removed; `track_quality_unit_*` resources used)
   - Log: `docs/evidence/har-325-inline-track-quality-formatter-scan-20260504T064251Z.log`
4. Resource key parity scan across target locale files
   - Result: PASS (`track_quality_unit_khz`, `track_quality_unit_kbps`, `track_quality_unit_bit` present in all 13 target files)
   - Log: `docs/evidence/har-325-resource-key-parity-scan-20260504T064251Z.log`
5. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-325-smoke-list-avds-20260504T064251Z.log`

## Outcome
HAR-325 scoped checks passed with formatter behavior preserved and unit output now resource-backed.
