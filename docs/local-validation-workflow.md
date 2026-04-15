# Local Validation Workflow

This project includes two local helper scripts:

- `scripts/validate-local.sh`: primary entrypoint for compile/test/lint and optional smoke runs.
- `scripts/smoke-debug-emulator.sh`: direct emulator smoke runner.

## Prerequisites

- JDK 17 for normal validation gates and non-list smoke execution.
- Android SDK with `adb` and emulator tools.
- A configured AVD if you plan to auto-launch an emulator.

## Typical Validate-Local Commands

- Run standard local gates:
  - `scripts/validate-local.sh`
- Run smoke only and list AVDs:
  - `scripts/validate-local.sh --smoke-only --list-avds`
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
