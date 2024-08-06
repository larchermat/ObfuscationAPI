@echo off

if "%1"=="" (
  echo Usage: %0 path-to-APK
  exit /b 1
)

set p=%1

java -jar ..\..\apktool\apktool.jar d "%p%" -o ..\..\decompiled

copy "%p%" ..\..\apk.zip

powershell -command "Expand-Archive -Path ..\..\apk.zip -DestinationPath ..\..\decomp"

..\..\dexdump\win\dexdump -d ..\..\decomp\classes.dex > ..\..\dump.txt

del ..\..\apk.zip

rmdir /s /q ..\..\decomp
