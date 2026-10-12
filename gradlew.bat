@echo off
setlocal
set "ROOT=%~dp0"
set "GRADLE_VERSION=9.7.1"
set "BOOTSTRAP=%ROOT%.gradle-bootstrap"
set "GRADLE_HOME=%BOOTSTRAP%\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"

if exist "%GRADLE_BIN%" goto runGradle
if not exist "%BOOTSTRAP%" mkdir "%BOOTSTRAP%"
echo Gradle %GRADLE_VERSION% nicht gefunden; lade Gradle herunter ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='%BOOTSTRAP%'; $zip=Join-Path $base 'gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath $base -Force; Remove-Item -LiteralPath $zip -Force"
if errorlevel 1 exit /b 1
if not exist "%GRADLE_BIN%" (
  echo Fehler: Gradle konnte nicht installiert werden.
  exit /b 1
)

:runGradle
call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
