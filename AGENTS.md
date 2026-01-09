# AGENTS.md

## Project snapshot
- Android (Jetpack Compose) client for Music Assistant with Media3 playback and Android Auto support.
- Single Gradle module: `:app` (see `settings.gradle.kts`).

## Dev requirements
- JDK 17.
- Android SDK 35 (compileSdk/targetSdk 35, minSdk 30).
- Android Studio or Gradle (`./gradlew`).

## Build, run, test
- Build debug APK: `./gradlew :app:assembleDebug`
- Install to device/emulator: `./gradlew :app:installDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests (device required): `./gradlew :app:connectedDebugAndroidTest`
- Lint: `./gradlew :app:lintDebug`

## Code map
- App entry + manifest: `app/src/main/AndroidManifest.xml`
- Android Auto descriptor: `app/src/main/res/xml/automotive_app_desc.xml`
- Playback service + Auto browsing: `app/src/main/java/com/harmonixia/android/service/playback`
- Data layer: `app/src/main/java/com/harmonixia/android/data`
- Domain models/use cases: `app/src/main/java/com/harmonixia/android/domain`
- UI (Compose): `app/src/main/java/com/harmonixia/android/ui`
- Hilt modules: `app/src/main/java/com/harmonixia/android/di`
- WorkManager: `app/src/main/java/com/harmonixia/android/work`

## Android Auto / Media browsing notes
- Auto relies on the Media3 `MediaLibraryService` in `PlaybackService`.
- Browse/search behavior is in `PlaybackSessionCallback` and `MediaLibraryBrowser`.
- `MediaLibraryBrowser` is the source of Auto browse categories and `MediaItem` metadata.
  Ensure `isBrowsable`/`isPlayable`, artwork, and extras like
  `EXTRA_STREAM_URI`/`EXTRA_PARENT_MEDIA_ID` are correct.
- If you add a new top-level category or change browse behavior, update both
  `MediaLibraryBrowser` and any corresponding UI navigation.
- Keep the manifest Auto metadata and `automotive_app_desc.xml` in sync.

## Playback pipeline
- `PlaybackService` owns ExoPlayer, the MediaLibrarySession, wake locks, and notifications.
- `PlaybackStateManager` + `QueueManager` keep local state aligned with the server.
- Network commands go through `MusicAssistantRepository`.
- `SendspinPlaybackManager` handles PCM streaming and sync; `EqualizerManager` attaches to the audio session.

## Data and networking
- Music Assistant connection uses OkHttp WebSocket; JSON via kotlinx.serialization.
- Offline content and downloads go through `DownloadRepository`/`OfflineLibraryRepository`
  (also used by Auto browsing).

## Change guidance
- Use Hilt for new dependencies; wire them in `app/src/main/java/com/harmonixia/android/di`.
- Keep long-running work off the main thread; use coroutines with `Dispatchers.IO`.
- Avoid editing `local.properties` and generated build outputs.
