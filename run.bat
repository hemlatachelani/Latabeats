@echo off
cd /d "%~dp0"
echo Starting LataBeats Music Player...
call mvn javafx:run
if %errorlevel% neq 0 (
    echo.
    echo Application exited with code %errorlevel%.
)
pause
