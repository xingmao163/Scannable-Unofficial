@echo off
REM Build for all supported Minecraft versions
REM Requires Python to parse versions.gradle

setlocal enabledelayedexpansion

echo Building for all supported versions...
echo.

call .\gradlew buildAll --no-daemon

echo.
echo Done! JARs are in neoforge\build\libs\
pause
