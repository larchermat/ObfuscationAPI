#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <path to APK> <name of app>"
  exit 1
fi

p="$1"

a="$2"

basePath=../../..

rm -rf $basePath/decompiled/"$a"

java -jar $basePath/apktool/apktool.jar d "$p" -o $basePath/decompiled/"$a"

cp "$p" $basePath/decompiled/"$a"/"$a".zip

unzip $basePath/decompiled/"$a"/"$a".zip -d $basePath/decompiled/"$a"/decomp

dexFiles=$(ls $basePath/decompiled/"$a"/decomp/ | grep classes)

i=1

for dexFile in $dexFiles
do
  $basePath/binaries/mac/dexdump -d $basePath/decompiled/"$a"/decomp/"$dexFile" > $basePath/decompiled/"$a"/dump$i.txt
  i=$((i+1))
done

rm $basePath/decompiled/"$a"/"$a".zip

rm -r $basePath/decompiled/"$a"/decomp

cd $basePath/decompiled/"$a" || exit 1

printf "dist\nbuild" > .gitignore

git init > /dev/null 2>&1 || exit 1

git config core.safecrlf false > /dev/null 2>&1

git add -A > /dev/null 2>&1

git commit -q -m "Initial commit" > /dev/null 2>&1