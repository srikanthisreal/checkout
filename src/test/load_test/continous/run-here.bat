@echo off
chcp 65001 >nul
echo Running k6 tests from current directory...
echo Current dir: %CD%
echo.

echo Available test scripts:
dir *.js

echo.
echo 1. Running continuous-load.js...
k6 run continuous-load.js

pause