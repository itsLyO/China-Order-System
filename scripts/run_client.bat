@echo off
setlocal

echo ===================================================
echo [START] Khoi dong Client Node (Retailer / Dai ly)
echo ===================================================

cd /d "%~dp0\.."

set "CP=bin;lib\*"
java -cp "%CP%" client.core.OrderClient
pause
