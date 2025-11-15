@echo off
REM Build script for Windows deployment
REM This script builds the application and creates a Windows executable

echo ========================================
echo Building Kumara Enterprises POS System for Windows
echo ========================================
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or higher from https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo Checking Java version...
java -version
echo.

echo Step 1: Cleaning previous build...
call mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo.
echo Step 2: Building JAR file...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

echo.
echo Step 3: Checking if jpackage is available...
jpackage --version >nul 2>&1
if %errorlevel% equ 0 (
    echo jpackage is available. Creating Windows executable...
    echo.
    
    REM Create Windows executable using jpackage
    jpackage ^
        --input target ^
        --name "Kumara Enterprises POS System" ^
        --main-jar pos-1.0.0.jar ^
        --main-class com.devstack.pos.PosApplication ^
        --type exe ^
        --dest target\windows-installer ^
        --app-version 1.0.0 ^
        --vendor "DevStack" ^
        --description "Point of Sale System built with Spring Boot and JavaFX" ^
        --win-menu ^
        --win-shortcut ^
        --win-dir-chooser
    
    if %errorlevel% equ 0 (
        echo.
        echo ========================================
        echo Build successful!
        echo ========================================
        echo.
        echo Windows executable created in: target\windows-installer\
        echo You can distribute the .exe file to Windows users.
        echo.
    ) else (
        echo.
        echo WARNING: jpackage failed. You can still use the JAR file.
        echo JAR file location: target\pos-1.0.0.jar
        echo Use run-windows.bat to run the application.
        echo.
    )
) else (
    echo jpackage is not available (requires Java 14+).
    echo Building JAR file only...
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo JAR file created: target\pos-1.0.0.jar
    echo Use run-windows.bat to run the application.
    echo.
    echo To create a Windows executable, you need:
    echo 1. Java 14 or higher with jpackage tool
    echo 2. WiX Toolset (for creating .msi installer)
    echo    Download from: https://wixtoolset.org/
    echo.
)

pause

