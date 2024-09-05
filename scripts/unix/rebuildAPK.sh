#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <name of APK> <obfuscation applied if any>"
  exit 1
fi

p="$1.apk"

basePath=../..

if [ -z "$2" ]; then
  java -jar $basePath/apktool/apktool.jar b $basePath/decompiled/"$1" -o $basePath/decompiled/"$1"/dist/unmodified/"$p" --use-aapt2 --debug
  java -jar $basePath/apksigner/uber-apk-signer-1.3.0.jar -a $basePath/decompiled/"$1"/dist/unmodified/"$p" --allowResign --overwrite
else
  java -jar $basePath/apktool/apktool.jar b $basePath/decompiled/"$1" -o $basePath/decompiled/"$1/dist/$2/$p" --use-aapt2 --debug
  java -jar $basePath/apksigner/uber-apk-signer-1.3.0.jar -a $basePath/decompiled/"$1/dist/$2/$p" --allowResign --overwrite
fi

bash cleanUp.sh "$1"