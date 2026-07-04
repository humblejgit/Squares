@echo off
setlocal

cd /d "%~dp0"

if not exist out mkdir out

dir /s /b src\main\java\*.java > sources.txt
javac -source 8 -target 8 -encoding UTF-8 -d out @sources.txt

if errorlevel 1 (
    del sources.txt
    echo.
    echo Kompilace se nezdarila.
    pause
    exit /b 1
)

del sources.txt
java -cp out cz.codex.squares.SquaresApp

pause
