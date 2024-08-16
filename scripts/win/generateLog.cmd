@echo off

if "%1"=="" (
  echo Missing package
  exit /b 1
)

if "%2"=="" (
  echo Missing /.main_activity
  exit /b 1
)

if "%3"=="" (
  echo Missing activity event script
  exit /b 1
)

if "%4"=="" (
  echo Missing path to local log.txt
  exit /b 1
)

set p=%1

set "adb=..\..\binaries\win\adb"

%adb% shell am start -n "%p%%2"

set tmr=0
:waitForApp
timeout /T 5 > NUL

set /a tmr+=5

set "pid="

for /f %%a in ('%adb% shell pidof %p%') do (
  set "pid=%%a"
)

if "%pid%"=="" (
  if %tmr% gtr 60 (
    echo Timeout, app took too long to start
    %adb% emu kill
    exit /b 1
  )
  goto :waitForApp
)

%adb% shell /data/local/tmp/strace -p "%pid%" -o /data/local/tmp/strace_output.txt &

%adb% shell "%3"

timeout /T 5 > NUL

%adb% shell am force-stop "%p%"

%adb% pull /data/local/tmp/strace_output.txt "%4"

%adb% emu kill