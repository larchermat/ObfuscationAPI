#!/bin/bash

cd ../../decompiled || exit 1

if [ -e  smali/com/apireflectionmanager ]; then
  rm -r smali/com/apireflectionmanager
fi

if [ -e  smali/com/123456789 ]; then
  rm -r smali/com/123456789
fi

git restore --staged --worktree ./