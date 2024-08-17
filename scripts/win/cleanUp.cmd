@echo off

cd ..\..\decompiled

if exist "smali\com\apireflectionmanager" (
  rmdir /s /q "smali\com\apireflectionmanager"
)

if exist "smali\com\123456789" (
  rmdir /s /q "smali\com\123456789"
)

git restore --staged --worktree .\