@echo off
REM Kumara Enterprises POS System - Windows Launcher
REM This script runs the POS application on Windows

echo ========================================
echo Kumara Enterprises POS System
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher from https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Check Java version (must be 21 or higher)
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    goto :check_version
)
:check_version
echo Checking Java version...
java -version 2>&1 | findstr /i "version"
echo.

REM Set JAR file path
set JAR_FILE=target\pos-1.0.0.jar

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found: %JAR_FILE%
    echo Please build the project first using: mvn clean package
    pause
    exit /b 1
)

REM Set memory options
set JAVA_OPTS=-Xmx2048m -Xms512m

REM Run the application
echo Starting Kumara Enterprises POS System...
echo.
java %JAVA_OPTS% -jar "%JAR_FILE%"

REM Check if application exited with error
if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)

