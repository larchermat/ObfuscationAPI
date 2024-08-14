#!/bin/bash

basePath=../../..

adb=$basePath/binaries/mac/adb

$adb push $basePath/binaries/android/strace /data/local/tmp/

$adb shell chmod +x /data/local/tmp/strace