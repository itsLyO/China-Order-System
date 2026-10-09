@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo [BUILD] Bien dich He thong China Order Client/Server
echo ===================================================

cd /d "%~dp0\.."

if not exist "bin" mkdir bin

set "CP=lib\*;source"

echo [1/2] Tim kiem danh sach cac file nguon Java...
dir /s /b source\*.java > scripts\sources.txt

echo [2/2] Dang bien dich ma nguon Java bang javac...
javac -encoding UTF-8 -cp "%CP%" -d bin @scripts\sources.txt

if %ERRORLEVEL% equ 0 (
    echo [THANH CONG] Bien dich hoan tat vao thu muc bin/
) else (
    echo [THAT BAI] Co loi trong qua trinh bien dich!
)

if exist "scripts\sources.txt" del "scripts\sources.txt"
pause
