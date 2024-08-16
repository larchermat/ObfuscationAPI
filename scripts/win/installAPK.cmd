@echo off

if "%1"=="" (
  echo Missing name of APK
  exit /b 1
)
if "%2"=="" (
  echo Missing name of AVD
  exit /b 1
)

set a=%1

set d=%2

set "basePath=..\.."

set "adb=%basePath%\binaries\win\adb"

setlocal enabledelayedexpansion

start "" /B "%USERPROFILE%\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd %d% -no-snapshot-save > NUL 2>&1

set tmr=0

:waitForDevice
timeout /T 5 > NUL

set "output="

for /f "tokens=*" %%i in ('%adb% devices') do (
  set "output=!output!%%i "
)

echo !output!|findstr /r /c:"^List of devices attached.*device" > NUL 2>&1
if not errorlevel 1 (
    echo Successfully started emu
    goto :deviceStarted
)

set /a tmr+=5

if %tmr% gtr 60 (
  echo Timeout, device took too long to boot
  "%adb%" emu kill
  exit /b 1
)

goto :waitForDevice

:deviceStarted

"%adb%" root

"%adb%" install "%basePath%\decompiled\dist\%a%"

if not "%3"=="" if not "%4"=="" (
  set "permissions=%4"
  set "p=%3"
  for %%perm in (%permissions%) do (
    "%adb%" shell pm grant "%p%" android.permission.%%perm
  )
)

endlocal