@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0squares-launcher.ps1"

if errorlevel 1 (
    echo.
    echo Start hry selhal.
    pause
)

