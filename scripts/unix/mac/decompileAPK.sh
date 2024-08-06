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

dexFiles=`ls $basePath/decomp/ | grep classes`

i=1

for dexFile in $dexFiles
do
  $basePath/dexdump/mac/dexdump -d $basePath/decomp/"$dexFile" > $basePath/dump$i.txt
  i=$((i+1))
done

rm $basePath/apk.zip

rm -r $basePath/decomp