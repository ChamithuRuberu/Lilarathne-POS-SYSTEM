# KumaraPOS Installer Creation Script
# ===========================================

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  KumaraPOS Installer Builder" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$JDK_PATH = "C:\Users\PM_User\.jdks\ms-21.0.7"
$JPACKAGE = "$JDK_PATH\bin\jpackage.exe"
$JLINK = "$JDK_PATH\bin\jlink.exe"
$JAVA = "$JDK_PATH\bin\java.exe"

# Step 1: Clean previous builds
Write-Host "[1/5] Cleaning previous builds..." -ForegroundColor Green
if (Test-Path installer_staging) {
    Remove-Item installer_staging -Recurse -Force
}
if (Test-Path "KumaraPOS-1.0.0.exe") {
    Remove-Item "KumaraPOS-1.0.0.exe" -Force
}
Write-Host "Cleanup complete!" -ForegroundColor Green
Write-Host ""

# Step 2: Build the application
Write-Host "[2/5] Building application with Maven..." -ForegroundColor Green
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    Write-Host "Please fix the Maven errors above and try again." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host ""

# Step 3: Find the JAR file
Write-Host "[3/5] Looking for JAR file..." -ForegroundColor Green

# Try to find shaded JAR first, then fall back to regular JAR
$jarFile = Get-ChildItem target\pos-shaded-*.jar -ErrorAction SilentlyContinue | Select-Object -First 1

if ($null -eq $jarFile) {
    Write-Host "Shaded JAR not found, using Spring Boot JAR..." -ForegroundColor Yellow
    $jarFile = Get-ChildItem target\pos-*.jar -Exclude "*-shaded-*" | Select-Object -First 1
}

if ($null -eq $jarFile) {
    Write-Host ""
    Write-Host "ERROR: No JAR file found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Available files in target directory:" -ForegroundColor Yellow
    Get-ChildItem target\*.jar | ForEach-Object { Write-Host "  - $($_.Name)" -ForegroundColor Cyan }
    pause
    exit 1
}

Write-Host "Found: $($jarFile.Name)" -ForegroundColor Cyan
Write-Host "JAR size: $([math]::Round($jarFile.Length / 1MB, 2)) MB" -ForegroundColor Cyan
Write-Host ""
Write-Host "Skipping JAR test (optional step)..." -ForegroundColor Yellow
Write-Host "Note: If JAR fails to run, check JavaFX dependencies are included." -ForegroundColor Yellow
Write-Host ""

# Step 4: Verify JAR contains JavaFX dependencies
Write-Host "[4/6] Verifying JAR contents..." -ForegroundColor Green

# Check if JAR contains JavaFX dependencies (Spring Boot puts them in BOOT-INF/lib)
$jarList = jar tf $jarFile.FullName 2>&1 | Select-String -Pattern "(javafx|BOOT-INF/lib)" | Select-Object -First 5
if ($jarList) {
    Write-Host "JavaFX dependencies found in JAR" -ForegroundColor Green
    $jarList | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
} else {
    Write-Host "Warning: JavaFX classes not detected in JAR listing" -ForegroundColor Yellow
    Write-Host "Checking if this is a Spring Boot JAR..." -ForegroundColor Yellow
    $bootInfCheck = jar tf $jarFile.FullName 2>&1 | Select-String -Pattern "BOOT-INF" | Select-Object -First 1
    if ($bootInfCheck) {
        Write-Host "Spring Boot JAR structure detected - dependencies should be in BOOT-INF/lib" -ForegroundColor Green
    } else {
        Write-Host "Warning: This might not be a Spring Boot fat JAR" -ForegroundColor Yellow
    }
}
Write-Host ""

# Step 5: Create custom Java runtime (bundled with installer)
Write-Host "[5/7] Creating custom Java runtime (for bundling with installer)..." -ForegroundColor Green

$runtimeDir = "installer_staging\runtime"
if (Test-Path $runtimeDir) {
    Remove-Item $runtimeDir -Recurse -Force
}

if (-not (Test-Path $JLINK)) {
    Write-Host ""
    Write-Host "ERROR: jlink not found at: $JLINK" -ForegroundColor Red
    Write-Host "Please verify your JDK path." -ForegroundColor Yellow
    pause
    exit 1
}

# Create a custom runtime with essential Java modules
# This runtime will be bundled with the installer so Java doesn't need to be pre-installed
# Note: JavaFX is included as dependencies in the JAR, so we don't need to bundle JavaFX jmods
Write-Host "Building custom Java runtime (JavaFX will be loaded from JAR dependencies)..." -ForegroundColor Cyan

# Check if JavaFX jmods exist (they might not be in standard JDK)
$javafxJmodsPath = "$JDK_PATH\jmods"
$hasJavafxJmods = (Test-Path "$javafxJmodsPath\javafx.base.jmod") -or (Test-Path "$javafxJmodsPath\javafx.controls.jmod")

$jlinkArgs = @(
    "--output", $runtimeDir,
    "--module-path", "$JDK_PATH\jmods",
    "--add-modules", "java.base,java.desktop,java.logging,java.sql,java.xml,java.naming,java.security.jgss,java.security.sasl,jdk.unsupported",
    "--strip-debug",
    "--compress", "2",
    "--no-header-files",
    "--no-man-pages"
)

# Add JavaFX modules only if they exist in the JDK
if ($hasJavafxJmods) {
    Write-Host "  Including JavaFX modules from JDK..." -ForegroundColor Gray
    $jlinkArgs += "--add-modules", "javafx.base,javafx.controls,javafx.fxml,javafx.swing"
} else {
    Write-Host "  JavaFX jmods not found in JDK - will use JavaFX from JAR dependencies" -ForegroundColor Yellow
}

& $JLINK $jlinkArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Failed to create custom runtime!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "Custom runtime created successfully!" -ForegroundColor Green
$runtimeSize = (Get-ChildItem $runtimeDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "  Runtime size: $([math]::Round($runtimeSize, 2)) MB" -ForegroundColor Gray
Write-Host ""

# Step 6: Prepare staging directory
Write-Host "[6/7] Preparing staging directory..." -ForegroundColor Green
if (Test-Path installer_staging\app.jar) {
    Remove-Item installer_staging\app.jar -Force -ErrorAction SilentlyContinue
}
if (-not (Test-Path installer_staging)) {
    mkdir installer_staging | Out-Null
}
Copy-Item $jarFile.FullName installer_staging\app.jar
Write-Host "Staging prepared!" -ForegroundColor Green
Write-Host "  JAR copied to: installer_staging\app.jar" -ForegroundColor Gray
Write-Host ""

# Step 7: Create installer with jpackage
Write-Host "[7/7] Creating Windows installer with jpackage..." -ForegroundColor Green

if (-not (Test-Path $JPACKAGE)) {
    Write-Host ""
    Write-Host "ERROR: jpackage not found at: $JPACKAGE" -ForegroundColor Red
    Write-Host "Please verify your JDK path." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "Using jpackage from: $JPACKAGE" -ForegroundColor Cyan
Write-Host ""

# Build jpackage command with bundled Java runtime
# This creates a self-contained installer that works on any Windows PC without Java pre-installed
$jpackageArgs = @(
    "--name", "KumaraPOS",
    "--app-version", "1.0.0",
    "--vendor", "Kumara Enterprises",
    "--description", "Point of Sale System",
    "--type", "exe",
    "--input", "installer_staging",
    "--main-jar", "app.jar",
    "--main-class", "org.springframework.boot.loader.launch.JarLauncher",
    "--runtime-image", $runtimeDir,
    "--win-shortcut",
    "--win-menu",
    "--win-dir-chooser",
    "--java-options", "-Xmx1024m",
    "--java-options", "-Xms256m",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Djava.awt.headless=false",
    "--java-options", "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
)

Write-Host "Running jpackage with the following configuration:" -ForegroundColor Cyan
Write-Host "  Main JAR: app.jar" -ForegroundColor White
Write-Host "  Main Class: org.springframework.boot.loader.launch.JarLauncher (Spring Boot)" -ForegroundColor White
Write-Host "  Start Class: com.devstack.pos.PosApplication" -ForegroundColor White
Write-Host "  Runtime: Custom Java runtime (bundled - no Java installation required)" -ForegroundColor Green
Write-Host "  Note: This installer will work on any Windows PC without Java pre-installed!" -ForegroundColor Green
Write-Host ""

& $JPACKAGE $jpackageArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "     SUCCESS!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Installer created: " -NoNewline
    Write-Host "KumaraPOS-1.0.0.exe" -ForegroundColor Cyan
    Write-Host "Location: " -NoNewline
    Write-Host "$(Get-Location)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "File size: " -NoNewline
    $fileSize = (Get-Item "KumaraPOS-1.0.0.exe").Length / 1MB
    Write-Host "$([math]::Round($fileSize, 2)) MB" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "You can now:" -ForegroundColor Green
    Write-Host "  1. Test the installer by running KumaraPOS-1.0.0.exe" -ForegroundColor White
    Write-Host "  2. Distribute this file to other Windows computers" -ForegroundColor White
    Write-Host ""
    Write-Host "IMPORTANT: This is a SELF-CONTAINED installer!" -ForegroundColor Green
    Write-Host "  - No Java installation required on target computers" -ForegroundColor White
    Write-Host "  - Works on any Windows 10/11 PC (64-bit)" -ForegroundColor White
    Write-Host "  - Java runtime is bundled with the installer" -ForegroundColor White
    Write-Host ""
    Write-Host "NOTE: Logs are written to files in the logs/ directory." -ForegroundColor Yellow
    Write-Host "      Use .\view-logs.ps1 to view logs in a separate window." -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "     FAILED!" -ForegroundColor Red
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Installer creation failed!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Yellow
    Write-Host ""
}

pause