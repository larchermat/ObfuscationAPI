#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
  echo "Usage: $0 <path to APK> <name of AVD> <path to local log.txt> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$4"

basePath=../../..

a="$basePath/decompiled/$1.apk"

d="$2"

adb=~/Android/Sdk/platform-tools/adb

~/Android/Sdk/emulator/emulator @"$d" -no-snapshot-save -port "$4" > /dev/null 2>&1 &

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

    if [ $tmr -gt 30 ]; then
      echo "Timeout, device took too long to boot" >&2
      "$adb" emu kill
      sleep 5
      exit 1
    fi

done

"$adb" root

ping 127.0.0.1 -t 3

"$adb" install -g "$a"

ping 127.0.0.1 -t 3

aapt=~/Android/Sdk/build-tools/34.0.0/aapt

p=$("$aapt" dump badging "$a" | grep "package: name=" | sed 's/package: name=//g' | sed 's/ versionCode.*//g' | sed "s/\'//g")

"$adb" shell monkey --pct-syskeys 0 -p "$p" -c android.intent.category.LAUNCHER 1

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

if [ -n "$5" ]; then
  "$adb" shell "$5"
fi

ping 127.0.0.1 -t 5

"$adb" shell am force-stop "$p"

"$adb" pull /data/local/tmp/strace_output.txt "$3"

"$adb" emu kill

ping 127.0.0.1 -t 5