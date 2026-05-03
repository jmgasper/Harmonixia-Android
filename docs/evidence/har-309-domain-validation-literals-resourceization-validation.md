# HAR-309 Domain Validation Literals Resourceization: Validation Evidence

## Scope
- Isolated app-layer/domain modernization in:
  - `SearchLibraryUseCase`
  - `ApplyEqPresetUseCase`
- Replaced remaining hardcoded validation/precondition literals with resource-backed messages:
  - `"Search query is required"` -> `R.string.search_validation_query_required`
  - `"Preset not found"` -> `R.string.eq_validation_preset_not_found`
- Added `@ApplicationContext Context` injection where required and updated `UseCaseModule` provider wiring.
- Added both new keys to base + localized `values*/strings.xml` files.

## Validation Commands
```bash
export JAVA_HOME="$HOME/.local/jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon :app:compileDebugKotlin
rg -n '"Search query is required"|"Preset not found"' \
  app/src/main/java/com/harmonixia/android/domain/usecase/SearchLibraryUseCase.kt \
  app/src/main/java/com/harmonixia/android/domain/usecase/ApplyEqPresetUseCase.kt -S
scripts/smoke-debug-emulator.sh --list-avds
```

## Results
- `:app:compileDebugKotlin`: passed (`BUILD SUCCESSFUL`).
- Inline scan in scoped use-cases: no matches for targeted removed hardcoded literals.
- `scripts/smoke-debug-emulator.sh --list-avds`: returned `Medium_Phone`.

## Evidence Files
- `docs/evidence/har-309-compileDebugKotlin-20260503T172530Z.log`
- `docs/evidence/har-309-inline-domain-validation-scan-20260503T172530Z.log`
- `docs/evidence/har-309-smoke-command-20260503T172530Z.log`

## Next Action
- Follow-on modernization slice: review data-layer EQ preset/cache hardcoded exception literals (`EqPresetCache`, `EqPresetRepositoryImpl`) and resourceize user-facing failure text where appropriate, with locale parity.
