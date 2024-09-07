#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <path to apk>"
  exit 1
fi

aapt=~/Android/Sdk/build-tools/34.0.0/aapt

PKG=$("$aapt" dump badging "$1" | grep "package: name=" | sed 's/package: name=//g' | sed 's/ versionCode.*//g' | sed "s/\'//g")

echo "$PKG"

# bash test.sh "com.talent.Fpqrc" "/.CTwvkad" "/home/mlarcher/Desktop/tesi/ObfuscationAPI/logs/Dowgin/IdentifierRenaming/POWER_BUTTON/log1.txt" 5580 "input keyevent 26"