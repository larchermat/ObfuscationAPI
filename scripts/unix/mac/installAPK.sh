#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "Usage: $0 <path to APK> <name of AVD> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$3"

a="$1.apk"

d="$2"

basePath=../../..

adb=~/Library/Android/sdk/platform-tools/adb

nohup ~/Library/Android/sdk/emulator/emulator @"$d" -no-snapshot-save -port "$3" -no-window > /dev/null 2>&1 &

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

"$adb" install -g $basePath/decompiled/"$a"