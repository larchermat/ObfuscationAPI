#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "Usage: $0 <path_to_apk> <path to local log.txt> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$3"

adb=~/Library/Android/sdk/platform-tools/adb

aapt=~/Library/Android/sdk/build-tools/34.0.0/aapt

p=$("$aapt" dump badging "../../../decompiled/$1.apk" | grep "package: name=" | sed 's/package: name=//g' | sed 's/ versionCode.*//g' | sed "s/\'//g")

"$adb" shell monkey -p "$p" -c android.intent.category.LAUNCHER 1

tmr=0

while true; do
  sleep 1

  tmr=$((tmr + 1))

  pid=$("$adb" shell pidof "$p")

  if [ -n "$pid" ]; then
    break
  fi

  if [ $tmr -gt 30 ]; then
          echo "Timeout, app took too long to start" >&2
          "$adb" emu kill
          sleep 5
          exit 1
      fi
done

"$adb" shell strace -r -T -x -p "$pid" -o /data/local/tmp/strace_output.txt &

if [ -n "$4" ]; then
  "$adb" shell "$4"
fi

sleep 5

"$adb" shell am force-stop "$p"

"$adb" pull /data/local/tmp/strace_output.txt "$2" >&2

"$adb" emu kill

sleep 10