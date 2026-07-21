@echo off
setlocal

cd /d "%~dp0"

call mvn -q -pl squares-desktop -am package

if errorlevel 1 (
    echo.
    echo Sestaveni aplikace se nezdarilo.
    pause
    exit /b 1
)

java -jar target\squares.jar

if errorlevel 1 (
    echo.
    echo Spusteni aplikace se nezdarilo.
    pause
    exit /b 1
)

pause
