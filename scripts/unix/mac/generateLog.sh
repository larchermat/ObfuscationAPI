#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
  echo "Usage: $0 <package> </.main_activity> <path to local log.txt> <port>"
  exit 1
fi

export ANDROID_SERIAL="emulator-$4"

p="$1"

adb=~/Library/Android/sdk/platform-tools/adb

"$adb" shell am start -n "$p$2"

tmr=0

while true; do
  sleep 1

  tmr=$((tmr + 1))

  pid=$("$adb" shell pidof "$p")

  if [ -n "$pid" ]; then
    break
  fi

  if [ $tmr -gt 60 ]; then
          echo "Timeout, app took too long to start"
          "$adb" emu kill
          sleep 5
          exit 1
      fi
done

"$adb" shell /data/local/tmp/strace -p "$pid" -o /data/local/tmp/strace_output.txt &

if [ -n "$5" ]; then
  "$adb" shell "$5"
fi

sleep 5

"$adb" shell am force-stop "$p"

"$adb" pull /data/local/tmp/strace_output.txt "$3"

"$adb" emu kill

sleep 10