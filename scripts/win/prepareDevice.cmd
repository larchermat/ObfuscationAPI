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

set d=%1

set "adb=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb"

echo The device data will be wiped

start "" /b "%emulator%" -avd %d% -wipe-data -no-snapshot-load > NUL 2>&1

set tmr=0

:waitForDevice
ping -n 5 127.0.0.1 > NUL

for /f "tokens=*" %%i in ('"%adb%" shell getprop sys.boot_completed') do (
  echo %%i|findstr /r /c:"^1" > NUL 2>&1
  if not errorlevel 1 (
      echo Successfully started emu
      goto :deviceStarted
  )
)

set /a tmr+=5

if %tmr% gtr 120 (
  echo Timeout, device took too long to boot
  "%adb%" emu kill
  exit /b 1
)

goto :waitForDevice

:deviceStarted

"%adb%" root

"%adb%" emu kill