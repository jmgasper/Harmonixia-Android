# HAR-251 AGP 9 Migration Path (Hilt Warning Unblock)

## Goal

Unblock the Hilt processor warning fix by moving to an AGP 9-compatible toolchain, then aligning Hilt plugin/runtime/compiler to the same version.

## Current Baseline (repo state)

- AGP plugin: `9.1.1` (`build.gradle.kts`)
- Gradle wrapper: `9.3.1` (`gradle/wrapper/gradle-wrapper.properties`)
- Built-in Kotlin model active (no `org.jetbrains.kotlin.android` plugin)
- Legacy kapt plugin: `com.android.legacy-kapt` at `9.1.1`
- Hilt Gradle plugin: `2.59.2`
- Hilt runtime/compiler deps in `:app`: `2.59.2`
- `compileSdk`/`targetSdk`: `36`

## External Constraints Driving the Plan

- Hilt Gradle plugin AGP 9 support starts in Dagger `2.59` (AGP 9 required when using the Hilt Gradle plugin).
- AGP `9.0.1` requires at least Gradle `9.1.0` and JDK `17`.
- AGP `9.1.1` requires Gradle `9.3.1` and JDK `17`, and supports API level `37.0` and below.
- AGP 9 enables built-in Kotlin by default, and `kotlin-android` / `kotlin-kapt` require migration steps unless temporarily opted out.

## Recommended Delivery Strategy

Use a 2-phase migration so HAR-251 can unblock quickly without forcing all Kotlin/DSL changes in one PR.

## Phase 1 (HAR-251): Fast unblock with compatibility opt-out

1. Upgrade AGP/Gradle to AGP 9 baseline.
- Preferred: AGP `9.1.1` + Gradle `9.3.1`.
- Fallback if plugin compatibility is found during trial: AGP `9.0.1` + Gradle `9.1.0`.

2. Temporarily keep old Kotlin/DSL behavior.
- Add to `gradle.properties`:
  - `android.builtInKotlin=false`
  - `android.newDsl=false`
- Keep `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.kapt` during this phase.

3. Align Hilt versions.
- Update root Hilt plugin to `2.59.2` to match:
  - `implementation("com.google.dagger:hilt-android:2.59.2")`
  - `kapt("com.google.dagger:hilt-compiler:2.59.2")`

4. Run the warning target and core gates.
- Target task: `:app:kaptDebugUnitTestKotlin --warning-mode=all`
- Core gates:
  - `:app:compileDebugKotlin`
  - `:app:testDebugUnitTest`
  - `:app:lintDebug`

Expected Phase 1 outcome:
- Hilt plugin/runtime/compiler are version-aligned.
- AGP 9 prerequisite is satisfied.
- Warning from the HAR-251 target is either resolved or narrowed to a non-version-alignment root cause.

## Phase 2 (follow-up): Remove temporary opt-outs

1. Migrate to built-in Kotlin.
- Remove `org.jetbrains.kotlin.android` plugin.
- Replace `org.jetbrains.kotlin.kapt` with `com.android.legacy-kapt` (same version as AGP), or migrate remaining kapt usage to KSP.

2. Remove compatibility opt-outs.
- Remove `android.builtInKotlin=false`.
- Remove `android.newDsl=false`.

3. Re-run full validation including release path.
- `:app:assembleRelease`
- `:app:compileReleaseKotlin`
- `scripts/smoke-debug-emulator.sh --avd Medium_Phone`

## Concrete File Change Plan

Phase 1 files:
- `build.gradle.kts`
  - bump `com.android.application` to AGP 9 target
  - bump `com.google.dagger.hilt.android` to `2.59.2`
- `gradle/wrapper/gradle-wrapper.properties`
  - bump Gradle distribution to AGP-required 9.x
- `gradle.properties`
  - add temporary `android.builtInKotlin=false`
  - add temporary `android.newDsl=false`

Phase 2 files:
- `build.gradle.kts`
  - remove `org.jetbrains.kotlin.android` plugin declaration (if using built-in Kotlin globally)
- `app/build.gradle.kts`
  - remove `id("org.jetbrains.kotlin.android")`
  - replace `id("org.jetbrains.kotlin.kapt")` with `id("com.android.legacy-kapt")` or complete KSP migration
- `gradle.properties`
  - remove temporary opt-out flags

## Risks and Mitigations

1. Third-party plugin incompatibility with AGP 9/new DSL.
- Mitigation: keep `android.newDsl=false` in Phase 1 and upgrade plugins incrementally.

2. Kotlin plugin migration blast radius.
- Mitigation: defer built-in Kotlin migration to Phase 2; unblock Hilt first.

3. Slow/unstable incremental behavior with Hilt on AGP 9.
- Mitigation: stay on Hilt `2.59.2` (includes AGP 9 follow-up fixes) and run both clean + incremental validation.

## Rollback

If Phase 1 blocks delivery:
- Revert AGP, Gradle wrapper, Hilt plugin version, and `gradle.properties` opt-out changes together in one rollback commit.
- Keep captured logs and warning output attached to HAR-251 for the next attempt.

## Exit Criteria

HAR-251 is complete when:
- AGP 9 migration path is documented and approved.
- Phase 1 changes are applied and validated.
- Hilt plugin/runtime/compiler version alignment is achieved.
- HAR-251 warning target has a verified result (fixed or root-caused with evidence).

## HAR-252 Execution Notes (2026-05-02)

### What changed in code

- Root plugins:
  - removed `org.jetbrains.kotlin.android`
  - removed `org.jetbrains.kotlin.kapt`
  - added `com.android.legacy-kapt` at `9.1.1`
- App plugins:
  - removed `id("org.jetbrains.kotlin.android")`
  - replaced `id("org.jetbrains.kotlin.kapt")` with `id("com.android.legacy-kapt")`
- `gradle.properties`:
  - removed temporary opt-outs:
    - `android.builtInKotlin=false`
    - `android.newDsl=false`

### Why this diverged from the original Phase 1 plan

With AGP `9.1.1`, the compatibility opt-out path was not stable for this repo:
- keeping the opt-outs allowed configuration, but Hilt processing failed during `kaptDebugKotlin` with:
  - `Expected @AndroidEntryPoint to have a value. Did you forget to apply the Gradle Plugin?`
- removing opt-outs while still applying `org.jetbrains.kotlin.android` failed fast at configuration with:
  - `The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0`

This forced early adoption of the Phase 2 built-in Kotlin plugin model to restore Hilt task wiring.

### Validation result

Executed with local `JAVA_HOME` + local Android SDK environment variables:
- `:app:kaptDebugUnitTestKotlin`
- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:lintDebug`

Result:
- `BUILD SUCCESSFUL`
- Hilt transform/aggregation tasks now run in graph (`hiltSync*`, `hiltAggregateDeps*`, `hiltJavaCompile*`, `transform*ClassesWithAsm`)
- prior `@AndroidEntryPoint` / `@HiltAndroidApp` failures are resolved

## HAR-255 Phase 2 Execution Checklist (2026-05-02)

Purpose: convert Phase 2 migration state into repeatable, commit-sliced execution guardrails for future check-ins.

1. Preflight environment (required before each Phase 2 change)
- JDK 17 active (`java -version` reports 17)
- Android SDK includes:
  - `platform-tools`
  - `platforms;android-36`
  - `build-tools;36.0.0`

2. File-level guardrails
- Keep `build.gradle.kts` root plugin set aligned:
  - `com.android.application` `9.1.1`
  - `com.android.legacy-kapt` `9.1.1`
  - `com.google.dagger.hilt.android` `2.59.2`
- Keep app plugin block on built-in Kotlin model:
  - do not re-introduce `org.jetbrains.kotlin.android`
  - keep `id("com.android.legacy-kapt")` unless/until explicit KSP migration is finished
- Keep `gradle/wrapper/gradle-wrapper.properties` on `gradle-9.3.1-bin.zip` unless a coordinated version migration is scoped

3. Minimal validation gates per check-in
- `./gradlew :app:kaptDebugUnitTestKotlin --warning-mode=all --rerun-tasks`
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --warning-mode=all`

4. Exit signal for each check-in
- both gates above must end `BUILD SUCCESSFUL`
- no reappearance of prior Hilt wiring failures
- document deltas + evidence in the corresponding issue comment
