#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <path to APK>"
  exit 1
fi

p="$1"

basePath=../../..

java -jar $basePath/apktool/apktool.jar d "$p" -o $basePath/decompiled

cp "$p" $basePath/apk.zip

unzip $basePath/apk.zip -d $basePath/decomp

$basePath/dexdump/mac/dexdump -d $basePath/decomp/classes.dex > $basePath/dump.txt

rm $basePath/apk.zip

rm -r $basePath/decomp