# HAR-320 Validation - EQ attribution card literals resourceization

Timestamp (UTC): 20260504T041145Z

## Scope
- Resourceized remaining hardcoded attribution literals in `EqSettingsScreen.AttributionCard`:
  - `Licensed under CC BY-SA 4.0`
  - `Data sources: AutoEQ, oratory1990, and community contributors`
  - `EQ Implementation: Concepts from pulseaudio-equalizer-ladspa`
  - `Licensed under GPL-3.0`
- Added locale-parity string keys across `values*/strings.xml`:
  - `eq_attribution_opra_license`
  - `eq_attribution_data_sources`
  - `eq_attribution_ladspa_concepts`
  - `eq_attribution_ladspa_license`

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-320-compileDebugKotlin-20260504T041145Z.log`
2. Inline scan for targeted removed literals in `EqSettingsScreen.kt`
   - Result: PASS (no matches)
   - Log: `docs/evidence/har-320-inline-eq-attribution-scan-20260504T041145Z.log`
3. Resource key parity scan across target locale files
   - Result: PASS (all 4 keys present in all 13 target files)
   - Log: `docs/evidence/har-320-resource-key-parity-scan-20260504T041145Z.log`
4. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-320-smoke-command-20260504T041145Z.log`

## Outcome
HAR-320 acceptance checks passed with no scope regressions detected.
