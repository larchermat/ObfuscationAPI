#!/bin/bash

# This script starts the emulator wiping the device data and closes it to create a first snapshot that will be loaded
# everytime we execute an APK

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <name of AVD> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$2"

d="$1"

adb=~/Library/Android/sdk/platform-tools/adb

if [ ! -e ~/Library/Android/sdk/emulator/emulator ]; then
  echo "No android emulator installed"
  exit 1
fi

echo "The device's data will be wiped"

nohup ~/Library/Android/sdk/emulator/emulator @"$d" -wipe-data -no-snapshot-load -port "$2" -no-boot-anim > /dev/null 2>&1 &

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

    if [ $tmr -gt 60 ]; then
        echo "Timeout, device took too long to boot"
        "$adb" emu kill
        sleep 5
        exit 1
    fi
done

"$adb" root

"$adb" emu kill

sleep 10