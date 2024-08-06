@echo off

setlocal enabledelayedexpansion

if "%1"=="" (
  echo Usage: %0 path-to-APK
  exit /b 1
)

set p=%1

set "basePath=..\.."

java -jar "%basePath%\apktool\apktool.jar" d "%p%" -o "%basePath%\decompiled"

copy "%p%" %basePath%\apk.zip

powershell -command "Expand-Archive -Path %basePath%\apk.zip -DestinationPath %basePath%\decomp"

set "dexFiles="

for /f "delims=" %%f in ('dir /b %basePath%\decomp\*classes*') do (
  set "dexFiles=!dexFiles! %%f"
)

set i=1

for %%f in (!dexFiles!) do (
  "%basePath%\dexdump\win\dexdump" -d "%basePath%\decomp\%%f" > "%basePath%\dump!i!.txt"
  set /a i+=1
)

del "%basePath%\apk.zip"

rmdir /s /q "%basePath%\decomp"

endlocal