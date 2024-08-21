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

set "adb=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb"

start "" /B "%USERPROFILE%\AppData\Local\Android\Sdk\emulator\emulator" -avd %d% -no-snapshot-save > NUL 2>&1

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

if %tmr% gtr 60 (
  echo Timeout, device took too long to boot
  "%adb%" emu kill
  exit /b 1
)

goto :waitForDevice

:deviceStarted

"%adb%" root

"%adb%" install "%basePath%\decompiled\dist\%a%"

if not "%3"=="" if not "%~4"=="" (
  for %%p in (%~4) do (
    "%adb%" shell pm grant "%3" android.permission.%%p
  )
)