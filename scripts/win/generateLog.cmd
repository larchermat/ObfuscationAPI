@echo off

if "%~1"=="" (
  echo Missing package
  exit /b 1
)

if "%~2"=="" (
  echo Missing /.main_activity
  exit /b 1
)

if "%~3"=="" (
  echo Missing path to local log.txt
  exit /b 1
)

set "p=%1"

set "adb=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb"

"%adb%" shell am start -n "%p%%2"

set tmr=0
:waitForApp
ping -n 5 127.0.0.1 > NUL

set /a tmr+=5

set "pid="

for /f %%a in ('"%adb%" shell pidof %p%') do (
  set "pid=%%a"
)

if "%pid%"=="" (
  if %tmr% gtr 60 (
    echo Timeout, app took too long to start
    "%adb%" emu kill
    exit /b 1
  )
  goto :waitForApp
)

setlocal enabledelayedexpansion

set "output="

for /f "delims=" %%i in ('"%adb%" shell /data/local/tmp/strace') do (
  set "output=!output! %%i"
)

set cond=0

echo !output!|findstr /r /c:"No such file or directory" > NUL 2>&1
if errorlevel 1 (
  set cond=1
)

if %cond%==1 (
  start "" /b "%adb%" shell strace -p "%pid%" -o /data/local/tmp/strace_output.txt > NUL 2>&1
) else (
  start "" /b "%adb%" shell /data/local/tmp/strace -p "%pid%" -o /data/local/tmp/strace_output.txt > NUL 2>&1
)

if not "%~4"=="" (
  "%adb%" shell "%~4"
)

ping -n 5 127.0.0.1 > NUL

"%adb%" shell am force-stop "%p%"

"%adb%" pull /data/local/tmp/strace_output.txt "%~3"

"%adb%" emu kill

endlocal