#!/bin/bash

# One can optionally pass the permissions to grant as a single string separated by spaces "perm1 perm2" plus the name of
# the package

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <name of APK> <name of AVD>"
  exit 1
fi

p="$1"

d="$2"

basePath=../../..

adb=$basePath/binaries/linux/adb

nohup ~/Android/sdk/emulator/emulator @"$d" -no-snapshot-save > /dev/null 2>&1 &

timeout 60 $adb wait-for-device

if [ $? -ne 0 ]; then
  echo "Emulator failed to start within the timeout period."
  $adb emu kill
  exit 1
else
  echo "Emulator started successfully."
fi

$adb root

$adb install $basePath/decompiled/dist/"$p"

if [ -n "$3" ] && [ -n "$4" ]; then
  permissions="$4"
  p="$3"
  for perm in $permissions; do
    $adb shell pm grant "$p" android.permission."$perm"
  done
fi