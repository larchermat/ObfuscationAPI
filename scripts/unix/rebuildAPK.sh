#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <name of APK>"
  exit 1
fi

p="$1"

basePath=../..

java -jar $basePath/apktool/apktool.jar b $basePath/decompiled --use-aapt2 --debug

java -jar $basePath/apksigner/uber-apk-signer-1.3.0.jar -a $basePath/decompiled/dist/"$p" --allowResign --overwrite