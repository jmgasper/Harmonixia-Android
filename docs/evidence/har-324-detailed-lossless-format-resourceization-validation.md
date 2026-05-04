# HAR-324 — Detailed Lossless Format Resourceization

## Summary
- Resourceized detailed lossless quality formatting in `MusicAssistantRepositoryImpl.describeTrackQuality`.
- Replaced concatenated output (`"<lossless> {rate}kHz/{bitDepth}-bit"`) with `R.string.track_quality_lossless_detail_format`.
- Added locale-key parity for the new detailed lossless format key across all existing `values*` track-quality string files.
- Updated targeted repository tests to assert localized formatted output is sourced from the new string resource.

## Production Change
- File:
  - `app/src/main/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImpl.kt`
- Change:
  - In the lossless path with sample rate + bit depth present, replaced inline string construction with:
    - `context.getString(R.string.track_quality_lossless_detail_format, losslessLabel, rateText, bitDepth)`

## Resource Change
- Added `track_quality_lossless_detail_format` to:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-bn/strings.xml`
  - `app/src/main/res/values-de/strings.xml`
  - `app/src/main/res/values-es/strings.xml`
  - `app/src/main/res/values-fa/strings.xml`
  - `app/src/main/res/values-fi/strings.xml`
  - `app/src/main/res/values-fr/strings.xml`
  - `app/src/main/res/values-hi/strings.xml`
  - `app/src/main/res/values-it/strings.xml`
  - `app/src/main/res/values-nb/strings.xml`
  - `app/src/main/res/values-pt/strings.xml`
  - `app/src/main/res/values-sv/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`

## Test Change
- File:
  - `app/src/test/java/com/harmonixia/android/data/repository/MusicAssistantRepositoryImplTest.kt`
- Updated test:
  - `getAlbumTracks_resourceizesDetailedLosslessQualityLabel`
- Coverage added by update:
  - Stubs `R.string.track_quality_lossless_detail_format` and verifies the track quality output comes from formatted resource text.

## Validation
- `./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.data.repository.MusicAssistantRepositoryImplTest`
  - Result: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon :app:compileDebugKotlin`
  - Result: `BUILD SUCCESSFUL`
- `scripts/smoke-debug-emulator.sh --list-avds`
  - Result: `Medium_Phone`
- `scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`
  - Result: `Smoke test passed` on `emulator-5554`, app resumed in `com.harmonixia.android/.MainActivity`.

## Evidence Files
- `docs/evidence/har-324-test-repository-20260504T053441Z.log`
- `docs/evidence/har-324-compileDebugKotlin-20260504T053441Z.log`
- `docs/evidence/har-324-smoke-list-avds-20260504T053441Z.log`
- `docs/evidence/har-324-smoke-install-launch-20260504T053738Z.log`
