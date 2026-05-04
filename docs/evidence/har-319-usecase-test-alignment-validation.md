# HAR-319 Use-Case Test Alignment: Validation Evidence

## Scope
- Resolved test-only drift in:
  - `app/src/test/java/com/harmonixia/android/domain/usecase/ApplyEqPresetUseCaseTest.kt`
  - `app/src/test/java/com/harmonixia/android/domain/usecase/ConnectToServerUseCaseTest.kt`
- Updated tests to align with context-backed validation string access and constructor signatures in production code.
- Kept scope test-only.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon :app:testDebugUnitTest \
  --tests com.harmonixia.android.domain.usecase.ApplyEqPresetUseCaseTest \
  --tests com.harmonixia.android.domain.usecase.ConnectToServerUseCaseTest

./gradlew --no-daemon :app:compileDebugKotlin

rg -n "Server URL is invalid|Server URL cannot be empty|Username cannot be empty|Password cannot be empty" \
  app/src/test/java/com/harmonixia/android/domain/usecase/ConnectToServerUseCaseTest.kt -S

scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- Targeted unit tests: passed.
- `:app:compileDebugKotlin`: passed.
- Inline targeted legacy-literal scan in `ConnectToServerUseCaseTest.kt`: no matches.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-319-targeted-usecase-tests-20260504T030102Z.log`
- `docs/evidence/har-319-compileDebugKotlin-20260504T030102Z.log`
- `docs/evidence/har-319-inline-usecase-test-scan-20260504T030102Z.log`
- `docs/evidence/har-319-smoke-command-20260504T030102Z.log`

## Next Action
- Continue bounded modernization with the next incremental child slice after HAR-319 closure.
