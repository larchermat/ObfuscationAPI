@echo off

set "arch="

for /f %%a in ('echo "%PROCESSOR_ARCHITECTURE%"') do (
  set "arch=%%a"
)

set "strace=..\..\binaries\android\x86\strace"

set cond=0

echo %arch%|findstr /r "AMD64" > NUL 2>&1
if errorlevel 1 (
  set cond=1
)

if %cond%==1 (
  set "strace=..\..\binaries\android\arm\strace"
)

set "adb=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb"

"%adb%" push %strace% /data/local/tmp/

"%adb%" shell chmod +x /data/local/tmp/strace