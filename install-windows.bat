@echo off
REM Installation script for Windows
REM This script helps set up the POS system on a Windows machine

echo ========================================
echo Kumara Enterprises POS System - Installation
echo ========================================
echo.

REM Check Java installation
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed!
    echo.
    echo Please install Java 21 or higher:
    echo 1. Download from: https://www.oracle.com/java/technologies/downloads/
    echo 2. Or use OpenJDK: https://adoptium.net/
    echo 3. Add Java to your system PATH
    echo.
    pause
    exit /b 1
)

echo [OK] Java is installed
java -version
echo.

REM Check PostgreSQL installation
echo Checking PostgreSQL installation...
psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] PostgreSQL command line tools not found in PATH
    echo Please ensure PostgreSQL is installed and running
    echo Download from: https://www.postgresql.org/download/windows/
    echo.
) else (
    echo [OK] PostgreSQL is installed
    psql --version
    echo.
)

REM Check if database exists
echo Checking database connection...
echo Please enter your PostgreSQL password when prompted:
psql -U postgres -h localhost -c "SELECT 1" >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Database connection successful
    echo.
    
    REM Check if database exists
    psql -U postgres -h localhost -lqt | findstr /C:"robotikka" >nul
    if %errorlevel% equ 0 (
        echo [OK] Database 'robotikka' exists
    ) else (
        echo [INFO] Creating database 'robotikka'...
        psql -U postgres -h localhost -c "CREATE DATABASE robotikka;"
        if %errorlevel% equ 0 (
            echo [OK] Database created successfully
        ) else (
            echo [ERROR] Failed to create database
        )
    )
) else (
    echo [WARNING] Could not connect to database
    echo Please ensure PostgreSQL is running and credentials are correct
    echo.
)

echo.
echo ========================================
echo Installation Check Complete
echo ========================================
echo.
echo Next steps:
echo 1. Ensure PostgreSQL is running
echo 2. Update database settings in: src\resources\application.properties
echo 3. Build the application: build-windows.bat
echo 4. Run the application: run-windows.bat
echo.
pause

