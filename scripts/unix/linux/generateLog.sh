#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
  echo "Usage: $0 <package> </.main_activity> <activity event script> <path to local log.txt>"
  exit 1
fi

p="$1"

adb=../../../binaries/linux/adb

$adb shell am start -n "$p$2"

tmr=0

while true; do
  sleep 5

  tmr=$((tmr + 5))

  pid=$($adb shell pidof "$p")

  if [ -n "$pid" ]; then
    break
  fi

  if [ $tmr -gt 60 ]; then
          echo "Timeout, app took too long to start"
          $adb emu kill
          exit 1
      fi
done

$adb shell /data/local/tmp/strace -p "$pid" -o /data/local/tmp/strace_output.txt

$adb shell "$3"

sleep 5

$adb shell am force-stop "$p"

$adb pull /data/local/tmp/strace_output.txt "$4"

$adb emu kill