@echo off
setlocal

cd /d "%~dp0"

call mvn -q compile exec:java

if errorlevel 1 (
    echo.
    echo Spusteni aplikace se nezdarilo.
    pause
    exit /b 1
)

pause
