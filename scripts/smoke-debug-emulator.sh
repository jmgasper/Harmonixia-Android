#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

avd_name="Medium_Phone"
app_id="com.harmonixia.android"
gradle_task=":app:installDebug"
connect_timeout_seconds=300
boot_timeout_seconds=420
launch_wait_seconds=5
target_serial=""
auto_launch="true"
list_avds_only="false"
avd_option_set="false"
serial_option_set="false"
no_launch_option_set="false"
app_id_option_set="false"
task_option_set="false"
connect_timeout_option_set="false"
boot_timeout_option_set="false"

usage() {
    cat <<USAGE
Usage: $(basename "$0") [options]

Run a local emulator smoke test for the Harmonixia debug app:
1. Ensure an emulator is online (launches one if needed)
2. Wait for Android boot completion
3. Uninstall existing app package (to avoid signature mismatch)
4. Install debug APK via Gradle
5. Launch app and verify it is running

Options:
  --avd <name>          AVD name to launch when no emulator is online (default: ${avd_name})
  --serial <id>         Target specific adb serial instead of auto-detecting the first emulator
  --no-launch           Do not auto-launch an AVD when no emulator is online
  --list-avds           Print available AVD names and exit
  --app-id <id>         Android application id (default: ${app_id})
  --task <gradle-task>  Gradle install task (default: ${gradle_task})
  --connect-timeout <s> Emulator connect timeout in seconds (default: ${connect_timeout_seconds})
  --boot-timeout <s>    Boot completion timeout in seconds (default: ${boot_timeout_seconds})
  --help                Show this help
USAGE
}

require_option_value() {
    local option="$1"
    local value="${2:-}"
    if [[ -z "$value" || "$value" == --* ]]; then
        echo "Missing value for ${option}" >&2
        usage >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

require_positive_integer() {
    local option="$1"
    local value="$2"
    if ! [[ "$value" =~ ^[1-9][0-9]*$ ]]; then
        echo "Invalid value for ${option}: ${value}" >&2
        echo "Expected a positive integer." >&2
        usage >&2
        exit 1
    fi
    printf '%s\n' "$value"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --avd)
            avd_name="$(require_option_value "$1" "${2:-}")"
            avd_option_set="true"
            shift 2
            ;;
        --serial)
            target_serial="$(require_option_value "$1" "${2:-}")"
            serial_option_set="true"
            shift 2
            ;;
        --no-launch)
            auto_launch="false"
            no_launch_option_set="true"
            shift
            ;;
        --list-avds)
            list_avds_only="true"
            shift
            ;;
        --app-id)
            app_id="$(require_option_value "$1" "${2:-}")"
            app_id_option_set="true"
            shift 2
            ;;
        --task)
            gradle_task="$(require_option_value "$1" "${2:-}")"
            task_option_set="true"
            shift 2
            ;;
        --connect-timeout)
            connect_timeout_seconds="$(require_positive_integer "$1" "$(require_option_value "$1" "${2:-}")")"
            connect_timeout_option_set="true"
            shift 2
            ;;
        --boot-timeout)
            boot_timeout_seconds="$(require_positive_integer "$1" "$(require_option_value "$1" "${2:-}")")"
            boot_timeout_option_set="true"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [[ "$list_avds_only" == "true" ]]; then
    if [[ "$avd_option_set" == "true" || "$serial_option_set" == "true" || "$no_launch_option_set" == "true" || "$app_id_option_set" == "true" || "$task_option_set" == "true" || "$connect_timeout_option_set" == "true" || "$boot_timeout_option_set" == "true" ]]; then
        echo "--list-avds cannot be combined with runtime smoke options." >&2
        echo "Remove --avd/--serial/--no-launch/--app-id/--task/--connect-timeout/--boot-timeout when listing AVDs." >&2
        usage >&2
        exit 1
    fi
fi

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
sdk_home="${ANDROID_SDK_HOME:-$HOME/.config/.android}"
avd_home="${ANDROID_AVD_HOME:-$sdk_home/avd}"

export ANDROID_SDK_ROOT="$sdk_root"
export ANDROID_SDK_HOME="$sdk_home"
export ANDROID_AVD_HOME="$avd_home"
export PATH="$sdk_root/platform-tools:$sdk_root/emulator:$PATH"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "$HOME/.jdks/jdk-17.0.17+10" ]]; then
        export JAVA_HOME="$HOME/.jdks/jdk-17.0.17+10"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

if [[ "$auto_launch" == "true" || "$list_avds_only" == "true" ]] && ! command -v emulator >/dev/null 2>&1; then
    echo "emulator not found. Ensure Android emulator is installed under ${sdk_root}." >&2
    exit 1
fi

if [[ "$list_avds_only" == "true" ]]; then
    emulator -list-avds
    exit 0
fi

if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found. Ensure Android platform-tools are installed under ${sdk_root}." >&2
    exit 1
fi

if [[ ! -x "$repo_root/gradlew" ]]; then
    echo "gradlew not found at $repo_root/gradlew" >&2
    exit 1
fi

echo "Starting adb server..."
adb start-server >/dev/null

emulator_serial="$target_serial"
if [[ -z "$emulator_serial" ]]; then
    emulator_serial="$(adb devices | awk '/^emulator-/{print $1; exit}')"
fi

if [[ -z "$emulator_serial" && "$auto_launch" == "true" ]]; then
    if ! emulator -list-avds | grep -Fx "$avd_name" >/dev/null 2>&1; then
        echo "AVD '$avd_name' not found. Available AVDs:" >&2
        emulator -list-avds >&2 || true
        exit 1
    fi

    emulator_log="/tmp/harmonixia-emulator-${avd_name}.log"
    echo "Launching AVD '$avd_name' (log: $emulator_log)..."
    nohup emulator -avd "$avd_name" -no-window -no-audio -no-boot-anim \
        -gpu swiftshader_indirect -netdelay none -netspeed full >"$emulator_log" 2>&1 &
elif [[ -z "$emulator_serial" ]]; then
    echo "No emulator is online and --no-launch was specified." >&2
    adb devices -l >&2 || true
    exit 1
fi

echo "Waiting for emulator connection..."
connect_deadline=$((SECONDS + connect_timeout_seconds))
while true; do
    if [[ -n "$target_serial" ]]; then
        emulator_serial="$target_serial"
        emulator_state="$(adb devices | awk -v serial="$target_serial" '$1 == serial {print $2; found=1} END {if (!found) print ""}')"
    else
        emulator_serial="$(adb devices | awk '/^emulator-/{print $1; exit}')"
        emulator_state="$(adb devices | awk '/^emulator-/{print $2; exit}')"
    fi
    if [[ -n "$emulator_serial" && "$emulator_state" == "device" ]]; then
        break
    fi
    if (( SECONDS >= connect_deadline )); then
        if [[ -n "$target_serial" ]]; then
            echo "Timed out waiting for adb serial '$target_serial' to become online." >&2
        else
            echo "Timed out waiting for emulator to connect." >&2
        fi
        adb devices -l >&2 || true
        exit 1
    fi
    sleep 2
done

adb -s "$emulator_serial" wait-for-device >/dev/null

echo "Waiting for Android boot completion on $emulator_serial..."
boot_deadline=$((SECONDS + boot_timeout_seconds))
while true; do
    boot_completed="$(adb -s "$emulator_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot_completed" == "1" ]]; then
        break
    fi
    if (( SECONDS >= boot_deadline )); then
        echo "Timed out waiting for sys.boot_completed on $emulator_serial." >&2
        exit 1
    fi
    sleep 3
done

echo "Uninstalling existing package (if present): $app_id"
adb -s "$emulator_serial" uninstall "$app_id" >/tmp/harmonixia-smoke-uninstall.log 2>&1 || true

echo "Installing debug app via Gradle task '$gradle_task'..."
(
    cd "$repo_root"
    ./gradlew --no-daemon "$gradle_task"
)

echo "Launching $app_id..."
adb -s "$emulator_serial" shell monkey -p "$app_id" -c android.intent.category.LAUNCHER 1 \
    >/tmp/harmonixia-smoke-monkey.log 2>&1
sleep "$launch_wait_seconds"

pid="$(adb -s "$emulator_serial" shell pidof "$app_id" 2>/dev/null | tr -d '\r' || true)"
top_activity="$(adb -s "$emulator_serial" shell dumpsys activity activities \
    | grep -m1 -E 'topResumedActivity|mResumedActivity' || true)"

if [[ -z "$pid" ]]; then
    echo "Smoke test failed: app process not running for $app_id." >&2
    echo "Top activity: ${top_activity:-<none>}" >&2
    echo "Monkey output:" >&2
    sed -n '1,80p' /tmp/harmonixia-smoke-monkey.log >&2 || true
    exit 1
fi

echo "Smoke test passed"
echo "  serial: $emulator_serial"
echo "  pid: $pid"
echo "  top activity: ${top_activity:-<unknown>}"
