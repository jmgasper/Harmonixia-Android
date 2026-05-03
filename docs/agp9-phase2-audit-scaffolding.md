# HAR-256 AGP 9 Phase 2 Audit Scaffolding

This check-in adds a repeatable local audit for AGP 9 Phase 2 guardrails.

## Files

- `scripts/agp9-phase2-audit.sh`
- `docs/agp9-phase2-audit-scaffolding.md`

## What the audit checks

- Root plugin version alignment:
  - `com.android.application` is at the AGP 9 baseline (`9.1.1`)
  - `com.android.legacy-kapt` matches AGP
  - Hilt plugin version aligns with app runtime/compiler versions
- Gradle wrapper version baseline (`9.3.1`)
- App plugin migration state:
  - `org.jetbrains.kotlin.android` is absent
  - `org.jetbrains.kotlin.kapt` is absent
  - `com.android.legacy-kapt` is present
- Temporary opt-out flags are not reintroduced:
  - `android.builtInKotlin=false`
  - `android.newDsl=false`
- SDK levels remain at expected baseline (`compileSdk=36`, `targetSdk=36`)

## Commands

Run static AGP 9 Phase 2 audit:

```bash
scripts/agp9-phase2-audit.sh
```

Run audit option/regression checks:

```bash
scripts/test-agp9-phase2-audit-options.sh
```

Run audit plus validation gates:

```bash
scripts/agp9-phase2-audit.sh --with-gradle-checks
```

Run local validation (includes this static audit gate before Gradle compile/test/lint tasks):

```bash
scripts/validate-local.sh
```

Validation gates run by `--with-gradle-checks`:

```bash
./gradlew :app:kaptDebugUnitTestKotlin --warning-mode=all --rerun-tasks
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --warning-mode=all
```

`--with-gradle-checks` expects JDK 17. The script prefers existing `JAVA_HOME`,
then falls back to common JDK 17 env vars/paths used in this project.

## Interpreting results

- `FAIL`: guardrail violation that should block the next AGP 9 migration slice until fixed.
- `WARN`: baseline drift that may be intentional but must be reviewed and documented.
- `PASS`: check is consistent with the Phase 2 baseline.

The script exits non-zero when one or more `FAIL` checks are present.
