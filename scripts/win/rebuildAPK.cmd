@echo off

if "%1"=="" (
  echo Usage: %0 name-of-application
  exit /b 1
)

set p=%1

set "basePath=..\.."

java -jar "%basePath%\apktool\apktool.jar" b "%basePath%\decompiled" --use-aapt2 --debug

java -jar "%basePath%\apksigner\uber-apk-signer-1.3.0.jar" -a "%basePath%\decompiled\dist\%p%" --allowResign --overwrite