#!/bin/bash

# This script starts the emulator wiping the device data and closes it to create a first snapshot that will be loaded
# everytime we execute an APK

if [ -z "$1" ]; then
  echo "Usage: $0 <name of AVD>"
  exit 1
fi

d="$1"

adb=../../../binaries/mac/adb

if [ ! -e ~/Library/Android/sdk/emulator/emulator ]; then
  echo "No android emulator installed"
  exit 1
fi

echo "The device's data will be wiped"

nohup ~/Library/Android/sdk/emulator/emulator @"$d" -wipe-data -no-snapshot-load > /dev/null 2>&1 &

pattern="^List of devices attached[[:space:]].*device$"

tmr=0

while true; do
    sleep 5

    result=$($adb devices)

    if [[ "$result" =~ $pattern ]]; then
        echo "Successfully started emu"
        $adb root
        bash installStrace.sh
        $adb emu kill
        break
    fi

    tmr=$((tmr + 5))

    if [ $tmr -gt 60 ]; then
        echo "Timeout, device took too long to boot"
        $adb emu kill
        exit 1
    fi
done
