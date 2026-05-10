# HAR-260 AGP 9 Phase 2 Slice 4: Real-Emulator Full-Path Validation Evidence

## Run metadata
- Date (UTC): 2026-05-02
- Host timezone: Australia/Hobart
- Repository: `/home/jmgasper/Documents/Git/Harmonixia-Android`
- Validation mode: `scripts/validate-local.sh --agp9-full-path --keep-logs`
- AVD used: `Medium_Phone` (auto-launch path)

## Environment preparation performed in this heartbeat
- Set `JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17`
- Installed Android SDK components needed for real-emulator execution:
  - `emulator`
  - `platforms;android-35`
  - `system-images;android-35;google_apis;x86_64`
- Created/updated AVD:
  - `Medium_Phone` via `avdmanager`

## Command executed
```bash
export JAVA_HOME=/home/jmgasper/.local/jdks/temurin-17
export PATH="$JAVA_HOME/bin:/home/jmgasper/Android/Sdk/platform-tools:/home/jmgasper/Android/Sdk/emulator:$PATH"
scripts/validate-local.sh --agp9-full-path --keep-logs
```

## Result summary
- AGP 9 static audit: `PASS` (`PASS=12 WARN=0 FAIL=0`)
- Gradle validation gates: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug` all succeeded
- Emulator smoke gate: passed on `emulator-5554`
- App launch verification: `com.harmonixia.android/.MainActivity` resumed and process detected
- Final status: `Local validation passed.`

## Evidence artifacts
- Full combined run log:
  - `docs/evidence/har-260-agp9-full-path-20260502T115417Z.log`
- Copied smoke/emulator per-run logs (preserved in-repo):
  - `docs/evidence/har-260-emulator-Medium_Phone-722114-23288.log`
  - `docs/evidence/har-260-smoke-uninstall-722114-23288.log`
  - `docs/evidence/har-260-smoke-monkey-722114-23288.log`

## Key proof lines (from combined run log)
- `Running AGP 9 full-path validation gate (audit + compile/test/lint + smoke)...`
- `Summary: PASS=12 WARN=0 FAIL=0`
- `BUILD SUCCESSFUL` (validation gates)
- `Installing APK 'app-debug.apk' on 'Medium_Phone(AVD) - 15'`
- `Smoke test passed`
- `Local validation passed.`
