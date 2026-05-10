# HAR-308 ConnectToServer Validation Message Resourceization: Validation Evidence

## Scope
- Isolated app-layer modernization in `ConnectToServerUseCase`.
- Replaced hardcoded validation literals with existing resource-backed messages:
  - `"Server URL cannot be empty"` -> `R.string.error_server_url_required`
  - `"Server URL is invalid"` -> `R.string.error_invalid_url`
  - `"Username cannot be empty"` -> `R.string.error_username_required`
  - `"Password cannot be empty"` -> `R.string.error_password_required`
- Added `@ApplicationContext Context` injection to `ConnectToServerUseCase` for string resolution.
- Reused already-localized string keys; no new resource keys required.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"Server URL cannot be empty"|"Server URL is invalid"|"Username cannot be empty"|"Password cannot be empty"' app/src/main/java/com/harmonixia/android/domain/usecase/ConnectToServerUseCase.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in `ConnectToServerUseCase.kt`: no matches for targeted removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-308-compileDebugKotlin-20260503T172227Z.log`
- `docs/evidence/har-308-inline-connect-validation-scan-20260503T172227Z.log`
- `docs/evidence/har-308-smoke-command-20260503T172227Z.log`

## Next Action
- Follow-on app-layer modernization slice: resourceize remaining query/input validation literals in `SearchLibraryUseCase` and adjacent lightweight domain input validators to continue shrinking hardcoded exception text in the domain layer.
