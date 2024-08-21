#!/bin/bash

basePath=../../..

pattern="arm"

arch=$(uname -m)

strace=$basePath/binaries/android/x86/strace

if [[ "$arch" =~ $pattern ]]; then
  strace=$basePath/binaries/android/arm/strace
fi

adb=~/Android/Sdk/platform-tools/adb

"$adb" push $strace /data/local/tmp/

"$adb" shell chmod +x /data/local/tmp/strace