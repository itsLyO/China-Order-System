@echo off
setlocal

echo ===================================================
echo [START] Khoi dong Server Control Center Dashboard
echo He thong Order & Logistics Trung Quoc (Admin Hieu)
echo Cổng kết nối TCP: 8888
echo Giao dien FlatLaf Dark Theme
echo ===================================================

cd /d "%~dp0\.."

set "CP=bin;lib\*"
start "China Order Server" javaw -cp "%CP%" server.ui.ServerMainFrame
echo Server GUI da duoc khoi dong tren mot cua so rieng!
exit /b 0
