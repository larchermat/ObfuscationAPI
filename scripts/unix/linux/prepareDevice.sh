#!/bin/bash

# This script starts the emulator wiping the device data and closes it to create a first snapshot that will be loaded
# everytime we execute an APK

if [ -z "$1" ]; then
  echo "Usage: $0 <name of AVD>"
  exit 1
fi

d="$1"

adb=~/Android/Sdk/platform-tools/adb

if [ ! -e ~/Android/Sdk/emulator/emulator ]; then
  echo "No android emulator installed"
  exit 1
fi

echo "The device's data will be wiped"

nohup ~/Android/Sdk/emulator/emulator @"$d" -wipe-data -no-snapshot-load > /dev/null 2>&1 &

timeout 60 "$adb" wait-for-device

if [ $? -ne 0 ]; then
  echo "Emulator failed to start within the timeout period."
  "$adb" emu kill
  exit 1
else
  echo "Emulator started successfully."
fi

"$adb" root

bash installStrace.sh

"$adb" emu kill

pattern="^List of devices attached$"

while true; do
  result=$("$adb" devices)

  if [[ "$result" =~ $pattern ]]; then
    break
  fi
done