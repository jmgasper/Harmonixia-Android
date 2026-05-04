# HAR-321 Validation - ConnectionRecovery invalid-url matcher alignment

Timestamp (UTC): 20260504T041640Z

## Scope
- Removed hardcoded `INVALID_SERVER_URL_ERROR` comparison in `ConnectionRecoveryManager`.
- Injected `@ApplicationContext Context` and matched invalid URL failures via `context.getString(R.string.error_invalid_url)`.
- Updated `ConnectionRecoveryManagerTest` to use context-backed invalid URL message fixture.

## Validation Commands and Results
1. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:testDebugUnitTest --tests com.harmonixia.android.util.ConnectionRecoveryManagerTest`
   - Result: PASS
   - Log: `docs/evidence/har-321-targeted-connection-recovery-tests-20260504T041640Z.log`
2. `JAVA_HOME=/home/jmgasper/.local/jdk17 PATH=/home/jmgasper/.local/jdk17/bin:$PATH ./gradlew --no-daemon :app:compileDebugKotlin`
   - Result: PASS (`BUILD SUCCESSFUL`)
   - Log: `docs/evidence/har-321-compileDebugKotlin-20260504T041640Z.log`
3. Inline scan for invalid-url matcher in `ConnectionRecoveryManager.kt`
   - Result: PASS (hardcoded matcher removed, `R.string.error_invalid_url` usage present)
   - Log: `docs/evidence/har-321-inline-connection-recovery-scan-20260504T041640Z.log`
4. `scripts/smoke-debug-emulator.sh --list-avds`
   - Result: PASS (`Medium_Phone` listed)
   - Log: `docs/evidence/har-321-smoke-command-20260504T041640Z.log`

## Outcome
HAR-321 scoped checks passed with behavior-preserving matcher alignment.
