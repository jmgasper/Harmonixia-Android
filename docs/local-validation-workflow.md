# Local Validation Workflow

This project includes two local helper scripts:

- `scripts/validate-local.sh`: primary entrypoint for AGP 9 Phase 2 static audit, compile/test/lint, and optional smoke runs.
- `scripts/smoke-debug-emulator.sh`: direct emulator smoke runner.

## Prerequisites

- JDK 17 for normal validation gates and non-list smoke execution.
- `scripts/smoke-debug-emulator.sh --list-avds` does not require Java preflight.
- Android SDK with `adb` for all smoke runs.
- Android emulator tool is required only for `--list-avds` and AVD auto-launch paths (not required for `--serial` + `--no-launch` runs).
- A configured AVD if you plan to auto-launch an emulator.

## Typical Validate-Local Commands

- Run all option-parsing regression checks:
  - `scripts/test-local-validation-option-regressions.sh`
  - Includes `bash -n` syntax checks, both option regression suites, the hardcoded format-template scanner regression, and the hardcoded UI text-literal scanner regression + scanner self-test.
  - UI literal scanner coverage currently includes literal detection for both escaped (`"..."`) and triple-quoted (`"""..."""`) hardcoded literals across `Text(...)`, `BasicText(...)`, `text = ...`, `contentDescription = ...`, `AnnotatedString(...)`, `AnnotatedString(text = ...)`, `append(...)` / `appendLine(...)` / `appendRange(...)`, and named-arg append forms such as `append(text = ...)`, `appendLine(value = ...)`, and `appendRange(text = ..., startIndex = ..., endIndex = ...)` (including reordered named-arg forms where `text` appears after range args in `append(...)`/`appendRange(...)`) inside `buildAnnotatedString { ... }`.
- Run option-parsing regression checks for AGP 9 audit scaffold:
  - `scripts/test-agp9-phase2-audit-options.sh`
  - Covers help output, unknown-argument handling, and static-audit baseline execution output.
- Run AGP gate-path simulator regression checks for `validate-local.sh`:
  - `scripts/test-validate-local-agp-gate-simulator.sh`
  - Uses local command simulators to validate gate ordering, fail-fast behavior, and smoke-only bypass without running real Gradle/emulator jobs.
  - Preview selected mode(s) without running checks: `scripts/test-local-validation-option-regressions.sh --dry-run [--syntax-only|--behavior-only]`
  - Syntax-only mode: `scripts/test-local-validation-option-regressions.sh --syntax-only`
  - Behavior-only mode: `scripts/test-local-validation-option-regressions.sh --behavior-only`
  - Runner-mode self-test: `scripts/test-local-validation-option-regressions-runner.sh` (covers dry-run mode selection, runtime mode summaries, plus conflict and unknown-argument handling).
  - CI automation: `.github/workflows/option-regressions.yml` runs syntax-only, `agp9-audit-options`, `validate-local-agp-gate-simulator`, behavior-only (including hardcoded format-template and hardcoded UI text-literal scanner regression + scanner self-test), dry-run-preview, runner-self-test, and direct `scripts/test-validate-local-options.sh` + `scripts/test-smoke-debug-emulator-options.sh` + `scripts/check-hardcoded-format-templates.sh` + `scripts/check-hardcoded-ui-text-literals.sh` + `scripts/test-check-hardcoded-ui-text-literals.sh` modes on pushes/PRs to `master` when local-validation workflow/scripts/docs files change.
  - The option-regression matrix is intentionally serialized (`max-parallel: 1`) to keep output ordering predictable and reduce hosted-runner contention.
- Run option-parsing regression checks via the top-level wrapper:
  - `scripts/validate-local.sh --option-tests`
- `validate-local.sh` prints a shell-escaped smoke command preview before launching `smoke-debug-emulator.sh`.
- Run option-parsing regression checks for `validate-local.sh`:
  - `scripts/test-validate-local-options.sh`
  - Covers both base smoke flags and `--smoke-*` alias forms for help, passthrough, conflict behavior, unknown-argument handling, and missing-value validation.
- Run standard local gates:
  - `scripts/validate-local.sh`
  - Includes AGP 9 static audit (`scripts/agp9-phase2-audit.sh`) before Gradle compile/test/lint tasks.
- Run AGP 9 full-path gate with simulator smoke targeting:
  - `scripts/validate-local.sh --agp9-full-path --no-launch --serial emulator-5554 --task :app:installDebug`
  - Expected markers include:
    - `Running AGP 9 full-path validation gate (audit + compile/test/lint + smoke)...`
    - `Running AGP 9 Phase 2 static audit gate...`
    - `Running Gradle validation gates: :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug`
    - `Running emulator smoke gate...`
- Run smoke only and list AVDs:
  - `scripts/validate-local.sh --list-avds`
- Show smoke-script help without running Gradle gates:
  - `scripts/validate-local.sh --smoke-help`
- Run smoke against an existing emulator serial:
  - `scripts/validate-local.sh --with-smoke --no-launch --serial emulator-5554 --skip-compile --skip-test --skip-lint`
- Run smoke against any already-online emulator (no auto-launch):
  - `scripts/validate-local.sh --with-smoke --no-launch --skip-compile --skip-test --skip-lint`
- Tune smoke timeouts and launch wait:
  - `scripts/validate-local.sh --with-smoke --serial emulator-5554 --connect-timeout 60 --boot-timeout 120 --launch-wait 2 --skip-compile --skip-test --skip-lint`
- Run smoke via `validate-local.sh` and keep per-run logs:
  - `scripts/validate-local.sh --with-smoke --no-launch --serial emulator-5554 --task help --keep-logs --skip-compile --skip-test --skip-lint`

## Typical Smoke-Script Commands

- Run option-parsing regression checks via the smoke script:
  - `scripts/smoke-debug-emulator.sh --option-tests`
- Run option-parsing regression checks for `smoke-debug-emulator.sh`:
  - `scripts/test-smoke-debug-emulator-options.sh`
  - Covers help output, selector/conflict validation, unknown-argument handling, and missing-value validation.
- List available AVDs:
  - `scripts/smoke-debug-emulator.sh --list-avds`
- Smoke run against an existing emulator:
  - `scripts/smoke-debug-emulator.sh --no-launch --serial emulator-5554 --connect-timeout 60 --boot-timeout 120 --launch-wait 2`
- Smoke run that auto-launches an AVD:
  - `scripts/smoke-debug-emulator.sh --avd Medium_Phone`
- Smoke run that retains all per-run logs:
  - `scripts/smoke-debug-emulator.sh --no-launch --serial emulator-5554 --task help --keep-logs`

## Smoke Settings Output

`scripts/smoke-debug-emulator.sh` prints a `Smoke settings:` line before execution.  
The `target=` value indicates how emulator selection will work:

- `target=serial:<id>`: uses the explicit serial from `--serial`.
- `target=avd:<name>`: auto-launch path will use the provided/default AVD.
- `target=first-online-emulator`: `--no-launch` mode without `--serial`; uses the first already-online emulator.

## Option-Conflict Rules

- In `validate-local.sh`, `smoke-debug-emulator.sh`, and `test-local-validation-option-regressions.sh`, `-h` is accepted as a short alias for `--help`.
- In `test-local-validation-option-regressions.sh`, `--help`/`-h` exits immediately when encountered; unknown arguments parsed before help still fail as unknown.
- `--serial` must be an emulator adb serial in `emulator-<port>` format (for example `emulator-5554`).
- `--serial` and `--avd` are mutually exclusive for non-list runs.
- `--no-launch --avd` requires also providing `--serial`; otherwise it is rejected.
- In `smoke-debug-emulator.sh`, `--option-tests` cannot be combined with smoke execution flags.
- In `validate-local.sh`, `--no-launch` without `--serial` targets the first already-online emulator (it no longer forces a default AVD selector).
- `--list-avds` cannot be combined with runtime smoke flags such as:
  - `--avd`
  - `--serial`
  - `--no-launch`
  - `--connect-timeout`
  - `--boot-timeout`
  - `--launch-wait`
  - `--app-id`
  - `--task`
- In `validate-local.sh`, smoke-specific flags require smoke mode (`--with-smoke` or `--smoke-only`).
- In `validate-local.sh`, `--agp9-full-path` enables AGP audit + compile/test/lint + smoke in one command.
- In `validate-local.sh`, `--agp9-full-path` cannot be combined with informational smoke-only modes (`--smoke-help`, `--list-avds`) or compile/test/lint skip flags.
- In `validate-local.sh`, `--option-tests` cannot be combined with compile/test/lint toggles or smoke execution flags.
- In `validate-local.sh`, selecting any compile/test/lint gate also runs the AGP 9 Phase 2 static audit gate first.
- In `validate-local.sh`, smoke passthrough flags also accept `--smoke-*` aliases (for example `--smoke-avd`, `--smoke-serial`, `--smoke-no-launch`, `--smoke-connect-timeout`, `--smoke-boot-timeout`, `--smoke-launch-wait`, `--smoke-list-avds`).
- In `validate-local.sh`, both `--smoke-app-id`/`--app-id` and `--smoke-task`/`--task` are accepted aliases.
- In `validate-local.sh`, both `--keep-logs` and `--smoke-keep-logs` are accepted aliases.
- In `validate-local.sh`, keep-logs passthrough is accepted with all smoke modes, including `--list-avds` and `--smoke-help` (informational modes do not produce uninstall/monkey logs).
- In `validate-local.sh`, `--list-avds` implies smoke-only mode and skips compile/test/lint gates.
- In `validate-local.sh`, `--list-avds` prints AVD output and exits without printing a validation-pass summary.
- In `validate-local.sh`, `--smoke-help` implies smoke-only mode and cannot be combined with runtime smoke flags.
- In `validate-local.sh`, `--smoke-help` prints smoke usage and exits without printing a validation-pass summary.
- In `validate-local.sh`, informational smoke modes print mode-specific headings (`Showing emulator smoke help...`, `Listing available AVDs via smoke validation...`).

## Troubleshooting

- Error: `No validation gates selected. Enable at least one gate or use --with-smoke/--smoke-only.`
  - Fix: enable at least one compile/test/lint gate, or run smoke mode via `--with-smoke` or `--smoke-only`.
- Error: `AGP 9 audit script is missing or not executable: .../scripts/agp9-phase2-audit.sh`
  - Fix: restore executable permissions and file presence (`chmod +x scripts/agp9-phase2-audit.sh` if needed), then rerun validation.
- Error: `--list-avds cannot be combined with runtime smoke options.`
  - Fix: run `--list-avds` alone (or with non-runtime flags only), without serial/AVD/launch/task/timeout overrides (including `--smoke-*` alias forms).
- Error: `--smoke-help cannot be combined with runtime smoke options.`
  - Fix: run `--smoke-help` alone to print smoke-script usage, without runtime selector/timeout/app/task flags (including `--smoke-*` alias forms).
- Error: `--option-tests cannot be combined with compile/test/lint toggles or smoke execution flags.`
  - Fix: run `scripts/validate-local.sh --option-tests` by itself.
- Error: `--option-tests cannot be combined with smoke execution flags.`
  - Fix: run `scripts/smoke-debug-emulator.sh --option-tests` by itself.
- Error: `--no-launch cannot be combined with --avd unless --serial is also provided.`
  - Fix: provide `--serial <id>` when using `--no-launch`, or remove `--no-launch` if you want AVD auto-launch.
- Error: `Cannot combine --avd with --serial. Choose one target selector.`
  - Fix: choose exactly one target mode: AVD name (`--avd`) or adb serial (`--serial`).
- Error: `Invalid value for --serial: ...`
  - Fix: provide an emulator serial in `emulator-<port>` format (for example `emulator-5554`).
- Error: `emulator not found. Ensure Android emulator is installed under ...`
  - Fix: install Android emulator tools for `--list-avds` or auto-launch runs, or use `--serial --no-launch` when only `adb` is available.
- Error: `Timed out waiting for emulator to connect.` or `Timed out waiting for adb serial '...' to become online.`
  - Fix: verify device visibility with `adb devices -l`, correct the serial/target mode, and increase `--connect-timeout` if emulator startup is slow.
  - Note: if auto-launch was used, inspect the printed per-run emulator launch log path (`/tmp/harmonixia-emulator-<avd>-*.log`).
- Error: `No emulator is online and --no-launch was specified.`
  - Fix: start an emulator first, provide `--serial <emulator-id>`, or remove `--no-launch` so the script can auto-launch an AVD.
- Error: `AVD '<name>' not found.`
  - Fix: run `scripts/smoke-debug-emulator.sh --list-avds` to pick a valid AVD, or use `--serial <emulator-id>` to target an already-running emulator.
- Error: `No AVDs found under ...`
  - Fix: create an AVD in Android Studio Device Manager and rerun `scripts/smoke-debug-emulator.sh --list-avds`.
- Error: `Failed to list AVDs via emulator -list-avds.`
  - Fix: inspect the surfaced emulator stderr details (first 40 lines), verify emulator installation/SDK setup, then rerun `--list-avds`.
- Error: `Timed out waiting for sys.boot_completed on ...`
  - Fix: check boot state with `adb -s <serial> shell getprop sys.boot_completed` and increase `--boot-timeout` if the device is still initializing.
  - Note: if auto-launch was used, inspect the printed per-run emulator launch log path (`/tmp/harmonixia-emulator-<avd>-*.log`).
- Error: `Smoke test failed: app process not running for ...`
  - Fix: inspect the printed per-run monkey log path (for example `/tmp/harmonixia-smoke-monkey-*.log`) and increase `--launch-wait` when startup is slow.
  - Note: when auto-launch is used, the failure output also prints a per-run emulator launch log path.
  - Note: failed runs retain the monkey log for diagnosis; transient uninstall logs are cleaned automatically.
- Debugging tip: use `--keep-logs` to retain per-run uninstall/monkey/emulator logs even when the run succeeds.
- Error: `JDK 17 is required for smoke execution.`
  - Fix: install/use JDK 17 (`java -version` should report 17) or set `JAVA_HOME` to a JDK 17 path.
- Error: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path...`
  - Fix: ensure `local.properties` `sdk.dir` points to an existing SDK directory and export:
    - `ANDROID_HOME=<sdk-path>`
    - `ANDROID_SDK_ROOT=<sdk-path>`
  - Expected packages for this repo baseline (`compileSdk 36`):
    - `platform-tools`
    - `platforms;android-36`
    - `build-tools;36.0.0`
  - Quick bootstrap example (Linux):
    - install command-line tools under `$HOME/Android/Sdk/cmdline-tools/latest`
    - run `sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"`
    - re-run `./gradlew :app:kaptDebugUnitTestKotlin --warning-mode=all`
