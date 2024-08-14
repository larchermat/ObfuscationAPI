#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
  echo "Usage: $0 <package> </.main_activity> <activity event script> <path to local log.txt>"
  exit 1
fi

p="$1"

m="$2"

s="$3"

basePath=../../..

adb=$basePath/binaries/mac/adb

$adb shell am start -n "$p$m"

while true; do
  pid=$($adb shell pidof "$p")

  if [ -n "$pid" ]; then
    break
  fi
done

$adb shell /data/local/tmp/strace -p "$pid" -o /data/local/tmp/strace_output.txt &

$adb shell "$s"

sleep 5

$adb shell am force-stop "$p"

$adb pull /data/local/tmp/strace_output.txt "$4"

$adb emu kill