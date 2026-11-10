@echo off
cd /d "%~dp0"
echo Compiling and running LataBeats DSA Test Suite...
echo.
call mvn test-compile
if %errorlevel% neq 0 (
    echo.
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)

java -cp "target/classes;target/test-classes;%USERPROFILE%\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.10.0\junit-jupiter-api-5.10.0.jar;%USERPROFILE%\.m2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar" musicplayer.StandaloneTestRunner
echo.
pause
