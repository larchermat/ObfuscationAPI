@echo off

if "%1"=="" (
  echo Missing name of AVD
  exit /b 1
)

set "emulator=%USERPROFILE%\AppData\Local\Android\Sdk\emulator\emulator.exe"

if not exist "%emulator%" (
  echo Android emulator could not be found under "%emulator%"
  exit /b 1
)

set d=%2

set "adb=..\..\binaries\win\adb"

echo The device data will be wiped

setlocal enabledelayedexpansion

start "" /b "%emulator%" -avd %d% -wipe-data -no-snapshot-load > NUL 2>&1

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
  %adb% emu kill
  exit /b 1
)

goto :waitForDevice

:deviceStarted

%adb% root

cmd.exe /c installStrace.cmd

%adb% emu kill

endlocal