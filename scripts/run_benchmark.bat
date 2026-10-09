@echo off
setlocal

echo ===================================================
echo [BENCHMARK] Do luong Hieu nang mang va Do tre RTT
echo ===================================================

cd /d "%~dp0\.."

set "CP=bin;lib\*"
java -cp "%CP%" benchmark.LatencyThroughputTester
pause
