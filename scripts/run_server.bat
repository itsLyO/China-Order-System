@echo off
setlocal

echo ===================================================
echo [START] Khoi dong Server Node (Order Specialist)
echo Port mac dinh: 8888
echo ===================================================

cd /d "%~dp0\.."

set "CP=bin;lib\*"
java -cp "%CP%" server.core.OrderServer
pause
