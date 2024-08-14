@echo off

if "%1"=="" (
  echo Usage: %0 name-of-application
  exit /b 1
)

set p=%1

set "basePath=..\.."

"%basePath%\binaries\win\adb" root

"%basePath%\binaries\win\adb" install "%basePath%\decompiled\dist\%p%"