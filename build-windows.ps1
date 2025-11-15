# PowerShell Build Script for Windows .exe
# Kumara Enterprises POS System

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building Kumara Enterprises POS System for Windows" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Maven is installed
Write-Host "Checking Maven..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn -version 2>&1
    Write-Host "✓ Maven found" -ForegroundColor Green
} catch {
    Write-Host "✗ ERROR: Maven is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Maven from https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if Java is installed
Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java found" -ForegroundColor Green
    Write-Host $javaVersion[0] -ForegroundColor Gray
} catch {
    Write-Host "✗ ERROR: Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 21 or higher from https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Step 1: Cleaning previous build..." -ForegroundColor Yellow
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ ERROR: Clean failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Step 2: Building JAR file..." -ForegroundColor Yellow
mvn package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ ERROR: Build failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Step 3: Checking if jpackage is available..." -ForegroundColor Yellow
try {
    $jpackageVersion = jpackage --version 2>&1
    Write-Host "✓ jpackage found" -ForegroundColor Green
    Write-Host ""
    Write-Host "Creating Windows executable..." -ForegroundColor Yellow
    Write-Host ""
    
    # Create Windows executable using jpackage
    jpackage `
        --input target `
        --name "Kumara Enterprises POS System" `
        --main-jar pos-1.0.0.jar `
        --main-class com.devstack.pos.PosApplication `
        --type exe `
        --dest target\windows-installer `
        --app-version 1.0.0 `
        --vendor "DevStack" `
        --description "Point of Sale System built with Spring Boot and JavaFX" `
        --win-menu `
        --win-shortcut `
        --win-dir-chooser
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Build successful!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Windows executable created in: target\windows-installer\" -ForegroundColor Cyan
        Write-Host "File: Kumara Enterprises POS System.exe" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "You can distribute this .exe file to Windows users." -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "⚠ WARNING: jpackage failed. You can still use the JAR file." -ForegroundColor Yellow
        Write-Host "JAR file location: target\pos-1.0.0.jar" -ForegroundColor Cyan
        Write-Host "Use run-windows.bat to run the application." -ForegroundColor Cyan
        Write-Host ""
    }
} catch {
    Write-Host "✗ jpackage is not available (requires Java 14+ JDK)" -ForegroundColor Yellow
    Write-Host "Building JAR file only..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAR file created: target\pos-1.0.0.jar" -ForegroundColor Cyan
    Write-Host "Use run-windows.bat to run the application." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To create a Windows executable, you need:" -ForegroundColor Yellow
    Write-Host "1. Java 14 or higher JDK (not just JRE)" -ForegroundColor Yellow
    Write-Host "2. WiX Toolset (optional, for MSI installer)" -ForegroundColor Yellow
    Write-Host "   Download from: https://wixtoolset.org/" -ForegroundColor Yellow
    Write-Host ""
}

Read-Host "Press Enter to exit"

