#!/bin/bash

# This script starts the emulator wiping the device data and closes it to create a first snapshot that will be loaded
# everytime we execute an APK

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <name of AVD> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$2"

d="$1"

adb=~/Android/Sdk/platform-tools/adb

if [ ! -e ~/Android/Sdk/emulator/emulator ]; then
  echo "No android emulator installed"
  exit 1
fi

echo "The device's data will be wiped"

~/Android/Sdk/emulator/emulator @"$d" -wipe-data -no-snapshot-load -port "$2" -no-window > /dev/null 2>&1 &

pattern="^1"

tmr=0

while true; do
    sleep 1

    result=$("$adb" shell getprop sys.boot_completed)

    if [[ "$result" =~ $pattern ]]; then
        echo "Successfully started emu"
        break
    fi

    tmr=$((tmr + 1))

    if [ $tmr -gt 120 ]; then
        echo "Timeout, device took too long to boot" >&2
        "$adb" emu kill
        sleep 5
        exit 1
    fi
done

sleep 20

"$adb" emu kill

sleep 20