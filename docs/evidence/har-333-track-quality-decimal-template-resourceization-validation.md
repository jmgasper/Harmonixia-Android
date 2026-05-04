# HAR-333 Validation - TrackQuality decimal template resourceization

Timestamp (UTC): 20260504T103629Z

## Scope
- Resourceized remaining hardcoded decimal precision template (`"%.1f"`) in `TrackQualityFormatter` for kHz/kbps rounding.
- Added `track_quality_decimal_one_place_format` resource key and used resolver-backed formatting.
- Preserved whole-number behavior while routing decimal formatting through resource templates.
- Added focused unit tests for decimal sample-rate and decimal bitrate formatting paths.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:testDebugUnitTest --tests com.harmonixia.android.ui.components.TrackQualityFormatterTest`
   - Result: PASS
   - Log: `docs/evidence/har-333-test-track-quality-formatter-20260504T103629Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-333-compileDebugKotlin-20260504T103629Z.log`
3. Inline scan across `TrackQualityFormatter.kt` and `TrackQualityFormatterTest.kt`
   - Result: PASS (decimal paths use resource-backed template key; inline `"%.1f"` removed)
   - Log: `docs/evidence/har-333-inline-track-quality-decimal-scan-20260504T103629Z.log`
4. Resource key parity scan across target locale files
   - Result: PASS (`track_quality_decimal_one_place_format` present in all target files)
   - Log: `docs/evidence/har-333-resource-key-parity-scan-20260504T103629Z.log`
5. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-333-smoke-list-avds-20260504T103629Z.log`

## Outcome
HAR-333 scoped checks passed with TrackQuality decimal rounding now template/resource backed and unit-tested.
