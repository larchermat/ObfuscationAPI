#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <directory>"
  exit 1
fi

cd ../../decompiled/"$1" || exit 1

if [ -e  smali/com/apireflectionmanager ]; then
  rm -r smali/com/apireflectionmanager
fi

if [ -e  smali/com/123456789 ]; then
  rm -r smali/com/123456789
fi

git restore --staged --worktree ./