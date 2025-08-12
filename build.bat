@echo off
echo Building MightyRTP Plugin...
echo.

REM Check if Maven is installed
mvn --version >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo Maven found, building plugin...
echo.

REM Clean and package
mvn clean package

if errorlevel 1 (
    echo.
    echo Build failed! Check the error messages above.
    pause
    exit /b 1
)

echo.
echo Build successful! Plugin JAR is located in the target/ folder
echo.
echo Files created:
dir target\*.jar /b
echo.
pause
