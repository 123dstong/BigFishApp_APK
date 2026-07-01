@echo off
cd /d %~dp0
gradle assembleDebug
if exist %cd%\app\build\outputs\apk\debug\app-debug.apk (
  echo APK: %cd%\app\build\outputs\apk\debug\app-debug.apk
) else (
  echo Build finished, but APK was not found in the expected location.
)
pause
