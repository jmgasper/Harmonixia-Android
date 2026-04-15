# Local Validation Workflow

This project includes two local helper scripts:

- `scripts/validate-local.sh`: primary entrypoint for compile/test/lint and optional smoke runs.
- `scripts/smoke-debug-emulator.sh`: direct emulator smoke runner.

## Prerequisites

- JDK 17 for normal validation gates and non-list smoke execution.
- `scripts/smoke-debug-emulator.sh --list-avds` does not require Java preflight.
- Android SDK with `adb` and emulator tools.
- A configured AVD if you plan to auto-launch an emulator.

## Typical Validate-Local Commands

- Run standard local gates:
  - `scripts/validate-local.sh`
- Run smoke only and list AVDs:
  - `scripts/validate-local.sh --list-avds`
- Run smoke against an existing emulator serial:
  - `scripts/validate-local.sh --with-smoke --no-launch --serial emulator-5554 --skip-compile --skip-test --skip-lint`
- Tune smoke timeouts and launch wait:
  - `scripts/validate-local.sh --with-smoke --serial emulator-5554 --connect-timeout 60 --boot-timeout 120 --launch-wait 2 --skip-compile --skip-test --skip-lint`

## Typical Smoke-Script Commands

- List available AVDs:
  - `scripts/smoke-debug-emulator.sh --list-avds`
- Smoke run against an existing emulator:
  - `scripts/smoke-debug-emulator.sh --no-launch --serial emulator-5554 --connect-timeout 60 --boot-timeout 120 --launch-wait 2`
- Smoke run that auto-launches an AVD:
  - `scripts/smoke-debug-emulator.sh --avd Medium_Phone`

## Option-Conflict Rules

- `--serial` and `--avd` are mutually exclusive for non-list runs.
- `--no-launch --avd` requires also providing `--serial`; otherwise it is rejected.
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
- In `validate-local.sh`, `--list-avds` implies smoke-only mode and skips compile/test/lint gates.

## Troubleshooting

- Error: `No validation gates selected. Enable at least one gate or use --with-smoke/--smoke-only.`
  - Fix: enable at least one compile/test/lint gate, or run smoke mode via `--with-smoke` or `--smoke-only`.
- Error: `--list-avds cannot be combined with runtime smoke options.`
  - Fix: run `--list-avds` alone (or with non-runtime flags only), without serial/AVD/launch/task/timeout overrides.
- Error: `--no-launch cannot be combined with --avd unless --serial is also provided.`
  - Fix: provide `--serial <id>` when using `--no-launch`, or remove `--no-launch` if you want AVD auto-launch.
- Error: `Cannot combine --avd with --serial. Choose one target selector.`
  - Fix: choose exactly one target mode: AVD name (`--avd`) or adb serial (`--serial`).
- Error: `JDK 17 is required for smoke execution.`
  - Fix: install/use JDK 17 (`java -version` should report 17) or set `JAVA_HOME` to a JDK 17 path.
