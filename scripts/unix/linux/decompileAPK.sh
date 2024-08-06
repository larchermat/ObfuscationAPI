#!/bin/bash

echo "Path to APK"

read p

basePath=../../..

java -jar $basePath/apktool/apktool.jar d "$p" -o $basePath/decompiled

cp "$p" $basePath/apk.zip

unzip $basePath/apk.zip -d $basePath/decomp

$basePath/dexdump/linux/dexdump -d $basePath/decomp/classes.dex > $basePath/dump.txt

rm $basePath/apk.zip

rm -r $basePath/decomp