# HAR-321 — Repository Lossless Quality Label Resourceization

## Summary
- Completed the pending repository/test slice by replacing hardcoded `Lossless` literals in `MusicAssistantRepositoryImpl` with resource-backed text.
- Added focused unit coverage to verify both detailed and plain lossless label paths use localized string values.

## Production Change
- File:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Change:
  - In `formatTrackQuality(...)`, replaced hardcoded `"Lossless"` text with `context.getString(R.string.track_quality_lossless)`.
  - Detailed label now builds from localized prefix: `"<localized lossless> {rate}kHz/{bitDepth}-bit"`.

## Test Change
- File:
  - `app/src/test/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImplTest.kt`
- Added tests:
  - `getAlbumTracks_resourceizesDetailedLosslessQualityLabel`
  - `getAlbumTracks_resourceizesPlainLosslessQualityLabel`
- Both tests stub `context.getString(R.string.track_quality_lossless)` and assert the resulting track quality uses that localized value.

## Validation
- Targeted unit tests:
  - `./gradlew --no-daemon :app:testDebugUnitTest --tests "com.harmonixia.android.data.repository.MusicAssistantRepositoryImplTest.getAlbumTracks_resourceizesDetailedLosslessQualityLabel" --tests "com.harmonixia.android.data.repository.MusicAssistantRepositoryImplTest.getAlbumTracks_resourceizesPlainLosslessQualityLabel"`
  - Result: `BUILD SUCCESSFUL`
- Compile:
  - `./gradlew :app:compileDebugKotlin`
  - Result: `BUILD SUCCESSFUL`
- Simulator availability:
  - `scripts/smoke-debug-emulator.sh --list-avds`
  - Result: `Medium_Phone`
- Emulator install/launch smoke:
  - `scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
  - Result: `Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`.
- Targeted literal scan:
  - No matches for removed hardcoded `Lossless` literals in repository quality formatter.

## Evidence Files
- `docs/evidence/har-321-test-repository-lossless-label-20260504T042326Z.log`
- `docs/evidence/har-321-compileDebugKotlin-20260504T042326Z.log`
- `docs/evidence/har-321-smoke-list-avds-20260504T042326Z.log`
- `docs/evidence/har-321-smoke-install-launch-20260504T042326Z.log`
- `docs/evidence/har-321-inline-lossless-label-scan-20260504T042326Z.log`
