#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <name of APK> <name of AVD>"
  exit 1
fi

p="$1"

d="$2"

basePath=../../..

adb=~/Android/Sdk/platform-tools/adb

nohup ~/Android/sdk/emulator/emulator @"$d" -no-snapshot-save > /dev/null 2>&1 &

timeout 60 "$adb" wait-for-device

if [ $? -ne 0 ]; then
  echo "Emulator failed to start within the timeout period."
  "$adb" emu kill
  exit 1
else
  echo "Emulator started successfully."
fi

"$adb" root

"$adb" install -g $basePath/decompiled/dist/"$p"