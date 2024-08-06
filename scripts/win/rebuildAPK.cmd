@echo off

echo APK name
set /p p=

java -jar ..\..\apktool\apktool.jar b ..\..\decompiled --use-aapt2 --debug

java -jar ..\..\apksigner\uber-apk-signer-1.3.0.jar -a ..\..\decompiled\dist\%p% --allowResign --overwrite