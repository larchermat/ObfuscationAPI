@echo off

set "basePath=..\.."

"%basePath%\binaries\win\adb" push "%basePath%\binaries\android\strace" /data/local/tmp/

"%basePath%\binaries\win\adb" shell chmod +x /data/local/tmp/strace