#!/bin/bash

# One can optionally pass the permissions to grant as a single string separated by spaces "perm1 perm2" plus the name of
# the package

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <name of APK> <name of AVD>"
  exit 1
fi

a="$1"

d="$2"

basePath=../../..

adb=$basePath/binaries/mac/adb

nohup ~/Library/Android/sdk/emulator/emulator @"$d" -no-snapshot-save > /dev/null 2>&1 &

pattern="^List of devices attached[[:space:]].*device$"

tmr=0

while true; do
    sleep 5

    result=$($adb devices)

    if [[ "$result" =~ $pattern ]]; then
        echo "Successfully started emu"
        break
    fi

    tmr=$((tmr + 5))

    if [ $tmr -gt 60 ]; then
      echo "Timeout, device took too long to boot"
      $adb emu kill
      exit 1
    fi

done

$adb root

$adb install $basePath/decompiled/dist/"$a"

if [ -n "$3" ] && [ -n "$4" ]; then
  permissions="$4"
  p="$3"
  for perm in $permissions; do
    $adb shell pm grant "$p" android.permission."$perm"
  done
fi