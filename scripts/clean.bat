@echo off
setlocal

echo [CLEAN] Dang xoa thu muc bin/ va cac file tam...

cd /d "%~dp0\.."

if exist "bin" rd /s /q "bin"
if exist "data\*.db-journal" del "data\*.db-journal"
if exist "scripts\sources.txt" del "scripts\sources.txt"

echo [HOAN TAT] Da don dep sach se.
pause
