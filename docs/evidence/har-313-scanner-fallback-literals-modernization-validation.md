# HAR-313 Scanner/Fallback Literals Modernization: Validation Evidence

## Scope
- Finalized scanner/settings fallback literal modernization in:
  - `app/src/main/java/com/harmonixia/android/data/local/LocalMediaScanner.kt`
  - `app/src/main/java/com/harmonixia/android/ui/screens/settings/SettingsViewModel.kt`
- Replaced hardcoded local-media scan validation and metadata fallback strings with resource-backed lookups.
- Added scanner validation/fallback resource keys across current `values*` directories.
- Resolved a build-blocking resource compilation issue surfaced during simulator validation by adjusting problematic French/Italian playlist-validation phrasing:
  - `values-fr/strings.xml`
  - `values-it/strings.xml`

## Added Scanner String Keys
- `local_media_scan_validation_folder_uri_blank`
- `local_media_scan_validation_folder_uri_invalid`
- `local_media_scan_validation_folder_access_failed`
- `local_media_scan_validation_folder_not_accessible`
- `local_media_scan_validation_permission_denied`
- `local_media_scan_validation_unknown_error`
- `local_media_scan_fallback_unknown_value`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "local_media_scan_validation_folder_uri_blank|local_media_scan_validation_folder_uri_invalid|local_media_scan_validation_folder_access_failed|local_media_scan_validation_folder_not_accessible|local_media_scan_validation_permission_denied|local_media_scan_validation_unknown_error|local_media_scan_fallback_unknown_value" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Resource key parity scan: new scanner keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.
- Full simulator smoke install/launch: passed on `emulator-5554`; app resumed (`com.harmonixia.android/.MainActivity`).

## Evidence Files
- `docs/evidence/har-313-compileDebugKotlin-20260503T220056Z.log`
- `docs/evidence/har-313-inline-scanner-fallback-scan-20260503T215751Z.log`
- `docs/evidence/har-313-resource-key-parity-scan-20260503T215751Z.log`
- `docs/evidence/har-313-smoke-list-avds-20260503T220056Z.log`
- `docs/evidence/har-313-smoke-install-launch-20260503T220056Z.log`

## Next Action
- Continue fallback-literal modernization outside scanner/settings scope in a separate isolated slice.
