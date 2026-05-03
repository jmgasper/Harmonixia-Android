# HAR-312 Scanner/Fallback Literals Modernization: Validation Evidence

## Scope
- Modernized remaining scanner/fallback literals in:
  - `app/src/main/java/com/harmonixia/android/data/local/LocalMediaScanner.kt`
  - `app/src/main/java/com/harmonixia/android/ui/screens/settings/SettingsViewModel.kt`
- Replaced hardcoded local-media scan validation and metadata fallback strings with resource-backed lookups.
- Added new local-media scanner keys across all current `values*` directories to preserve key parity.

## Added String Keys
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

rg -n "Local media folder URI is blank|Invalid local media folder URI|Unable to access local media folder|Local media folder is not accessible|Permission denied for local media folder|Unknown scan error|\"Unknown\"" \
  app/src/main/java/com/harmonixia/android/data/local/LocalMediaScanner.kt \
  app/src/main/java/com/harmonixia/android/ui/screens/settings/SettingsViewModel.kt -S

rg -n "local_media_scan_validation_folder_uri_blank|local_media_scan_validation_folder_uri_invalid|local_media_scan_validation_folder_access_failed|local_media_scan_validation_folder_not_accessible|local_media_scan_validation_permission_denied|local_media_scan_validation_unknown_error|local_media_scan_fallback_unknown_value" \
  app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scanner/fallback literal scan: no matches for targeted removed hardcoded literals.
- Resource key parity scan: new keys present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-312-compileDebugKotlin-20260503T215408Z.log`
- `docs/evidence/har-312-inline-scanner-fallback-scan-20260503T215408Z.log`
- `docs/evidence/har-312-resource-key-parity-scan-20260503T215408Z.log`
- `docs/evidence/har-312-smoke-command-20260503T215408Z.log`

## Next Action
- Continue fallback-literal modernization outside scanner scope (remaining `"Unknown error"` fallbacks in non-local-media view models) in a separate follow-up slice.
