# AGP 9 + Hilt Alignment Plan

## Objective

Unblock the build-tooling constraint tracked in `HAR-211` by migrating the Android build stack to Android Gradle Plugin (AGP) 9 and then aligning Hilt plugin/runtime/compiler versions.

## Current Constraint

- Current AGP: `8.13.2`
- Current Hilt Gradle plugin: `2.57.2`
- Current Hilt runtime/compiler deps: `2.59.2`
- Attempted Hilt plugin alignment to `2.59.2` failed because that plugin requires AGP `9.0.0+`.

## Scope

In scope:
- Build toolchain upgrade path
- Hilt version alignment
- Validation checklist
- Rollback strategy

Out of scope:
- Feature work unrelated to build tooling
- Kotlin major-version upgrades beyond AGP compatibility requirements

## Migration Steps

1. Create upgrade branch and freeze dependency churn
- Create a dedicated branch for toolchain migration.
- Avoid unrelated dependency upgrades during migration.

2. Upgrade AGP to 9.x baseline
- Update `com.android.application` plugin in root `build.gradle.kts` to a supported AGP 9 release.
- Upgrade Gradle wrapper to the AGP 9-required range.
- Confirm Java runtime/toolchain requirements for AGP 9 are met in CI and local dev.

3. Resolve AGP 9 API/config changes
- Update deprecated AGP DSL usage (if any) in module build scripts.
- Re-run sync and address plugin incompatibilities one-by-one.

4. Align Hilt versions
- Set `com.google.dagger.hilt.android` plugin version to match runtime/compiler coordinates.
- Keep `implementation("com.google.dagger:hilt-android:<same>")` and `kapt("com.google.dagger:hilt-compiler:<same>")` aligned.

5. Re-run warning target from HAR-211
- Execute `:app:testDebugUnitTest` and confirm whether `kaptDebugUnitTestKotlin` unrecognized-option warning is removed.

## Validation Checklist

Required gates after migration:
- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `scripts/smoke-debug-emulator.sh --avd Medium_Phone`

Suggested extra gates:
- `:app:assembleRelease`
- `:app:compileReleaseKotlin`

## Risk Register

1. AGP upgrade breaks third-party plugins
- Mitigation: upgrade in isolation and pin plugin versions explicitly.

2. Gradle wrapper incompatibility
- Mitigation: upgrade wrapper first, then AGP, and validate with clean builds.

3. Hilt codegen behavior changes
- Mitigation: run unit tests + smoke test and inspect generated sources tasks.

## Rollback Plan

If migration blocks delivery:
- Revert AGP/Hilt plugin changes together as one atomic rollback.
- Keep documentation and findings from migration attempts for future retry.

## Exit Criteria

The migration is complete when:
- AGP 9 is adopted and stable across local + CI validation gates.
- Hilt plugin/runtime/compiler are version-aligned.
- `HAR-211` warning target is either removed or documented with a new root cause.
