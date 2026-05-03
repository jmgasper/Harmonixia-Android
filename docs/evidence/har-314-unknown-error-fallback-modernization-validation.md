# HAR-314 Unknown Error Fallback Modernization: Validation Evidence

## Scope
- Modernized remaining non-resource `"Unknown error"` fallbacks in:
  - `app/src/main/java/com/harmonixia/android/ui/screens/home/HomeViewModel.kt`
  - `app/src/main/java/com/harmonixia/android/ui/screens/artists/ArtistDetailViewModel.kt`
- Added a shared string resource key `error_unknown` and wired fallback paths to use `context.getString(...)`.
- Added `error_unknown` to all current `values*` string resource files to keep key parity with prior modernization slices.

## Added String Key
- `error_unknown`

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "\"Unknown error\"" \
  app/src/main/java/com/harmonixia/android/ui/screens/home/HomeViewModel.kt \
  app/src/main/java/com/harmonixia/android/ui/screens/artists/ArtistDetailViewModel.kt -S

rg -n "error_unknown" app/src/main/res/values*/strings.xml

scripts/smoke-debug-emulator.sh --list-avds
scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline `"Unknown error"` scan (targeted files): no matches.
- Resource key parity scan: `error_unknown` present in base + localized `values*/strings.xml` files.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.
- Full simulator smoke install/launch: passed on `emulator-5554`; app resumed (`com.harmonixia.android/.MainActivity`).

## Evidence Files
- `docs/evidence/har-314-compileDebugKotlin-20260503T220649Z.log`
- `docs/evidence/har-314-inline-unknown-error-scan-20260503T220649Z.log`
- `docs/evidence/har-314-resource-key-parity-scan-20260503T220649Z.log`
- `docs/evidence/har-314-smoke-list-avds-20260503T220649Z.log`
- `docs/evidence/har-314-smoke-install-launch-20260503T220649Z.log`

## Next Action
- Continue app-layer literal modernization by replacing other hardcoded user-facing fallback text outside this slice (for example, non-resource validation/error messages in view models).
