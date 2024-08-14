#!/bin/bash

basePath=../../..

adb=$basePath/binaries/linux/adb

$adb push $basePath/binaries/android/strace /data/local/tmp/

$adb shell chmod +x /data/local/tmp/strace