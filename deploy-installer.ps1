# KumaraPOS Installer Creation Script
# ===========================================

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  KumaraPOS Installer Builder" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Configuration - Auto-detect JDK path
Write-Host "[0/7] Detecting JDK installation..." -ForegroundColor Green

function Find-JDK {
    # Try multiple methods to find JDK 21
    $candidates = @()
    
    # Method 1: Check JAVA_HOME environment variable
    $javaHome = $env:JAVA_HOME
    if ($javaHome -and (Test-Path "$javaHome\bin\jpackage.exe")) {
        $candidates += $javaHome
        Write-Host "  Found JDK via JAVA_HOME: $javaHome" -ForegroundColor Gray
    }
    
    # Method 2: Check if jpackage is in PATH
    $jpackageInPath = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($jpackageInPath) {
        $jpackageDir = Split-Path (Split-Path $jpackageInPath.Source -Parent) -Parent
        if (Test-Path "$jpackageDir\bin\jpackage.exe") {
            $candidates += $jpackageDir
            Write-Host "  Found JDK via PATH: $jpackageDir" -ForegroundColor Gray
        }
    }
    
    # Method 3: Check common .jdks directory (IntelliJ IDEA, VS Code, etc.)
    $userProfile = $env:USERPROFILE
    if ($userProfile) {
        $jdksDir = "$userProfile\.jdks"
        if (Test-Path $jdksDir) {
            $jdkDirs = Get-ChildItem $jdksDir -Directory -ErrorAction SilentlyContinue | 
                       Where-Object { Test-Path "$($_.FullName)\bin\jpackage.exe" } |
                       Sort-Object Name -Descending
            foreach ($jdkDir in $jdkDirs) {
                $candidates += $jdkDir.FullName
                Write-Host "  Found JDK in .jdks: $($jdkDir.FullName)" -ForegroundColor Gray
            }
        }
    }
    
    # Method 4: Check Program Files
    $programFilesPaths = @(
        "${env:ProgramFiles}\Java",
        "${env:ProgramFiles(x86)}\Java",
        "${env:ProgramFiles}\Eclipse Adoptium",
        "${env:ProgramFiles}\Microsoft",
        "${env:ProgramFiles}\Amazon Corretto"
    )
    foreach ($basePath in $programFilesPaths) {
        if (Test-Path $basePath) {
            $jdkDirs = Get-ChildItem $basePath -Directory -ErrorAction SilentlyContinue |
                       Where-Object { Test-Path "$($_.FullName)\bin\jpackage.exe" } |
                       Sort-Object Name -Descending
            foreach ($jdkDir in $jdkDirs) {
                $candidates += $jdkDir.FullName
                Write-Host "  Found JDK in Program Files: $($jdkDir.FullName)" -ForegroundColor Gray
            }
        }
    }
    
    # Method 5: Check for JDK 21 specifically (preferred version)
    $jdk21 = $candidates | Where-Object { 
        $javaExe = "$_\bin\java.exe"
        if (Test-Path $javaExe) {
            $version = & $javaExe -version 2>&1 | Select-String -Pattern "version `"(\d+)" | ForEach-Object { $_.Matches[0].Groups[1].Value }
            return $version -eq "21"
        }
        return $false
    } | Select-Object -First 1
    
    if ($jdk21) {
        Write-Host "  Selected JDK 21: $jdk21" -ForegroundColor Green
        return $jdk21
    }
    
    # Fallback: Use first candidate found
    if ($candidates.Count -gt 0) {
        $selected = $candidates[0]
        Write-Host "  Selected JDK: $selected" -ForegroundColor Yellow
        Write-Host "  Warning: Not JDK 21, but will attempt to use it" -ForegroundColor Yellow
        return $selected
    }
    
    return $null
}

# Allow override via environment variable or parameter
$JDK_PATH = $null
if ($env:JDK_PATH -and (Test-Path "$env:JDK_PATH\bin\jpackage.exe")) {
    $JDK_PATH = $env:JDK_PATH
    Write-Host "  Using JDK from JDK_PATH environment variable: $JDK_PATH" -ForegroundColor Cyan
} elseif ($args.Count -gt 0 -and (Test-Path "$($args[0])\bin\jpackage.exe")) {
    $JDK_PATH = $args[0]
    Write-Host "  Using JDK from command line argument: $JDK_PATH" -ForegroundColor Cyan
} else {
    $JDK_PATH = Find-JDK
}

if (-not $JDK_PATH) {
    Write-Host ""
    Write-Host "ERROR: Could not find JDK installation!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please do one of the following:" -ForegroundColor Yellow
    Write-Host "  1. Set JAVA_HOME environment variable to your JDK path" -ForegroundColor White
    Write-Host "  2. Add JDK bin directory to PATH" -ForegroundColor White
    Write-Host "  3. Set JDK_PATH environment variable: `$env:JDK_PATH = 'C:\path\to\jdk'" -ForegroundColor White
    Write-Host "  4. Pass JDK path as argument: .\deploy-installer.ps1 'C:\path\to\jdk'" -ForegroundColor White
    Write-Host ""
    Write-Host "Example:" -ForegroundColor Cyan
    Write-Host "  `$env:JDK_PATH = 'C:\Users\YourName\.jdks\ms-21.0.7'" -ForegroundColor Gray
    Write-Host "  .\deploy-installer.ps1" -ForegroundColor Gray
    Write-Host ""
    pause
    exit 1
}

# Verify JDK has required tools
$JPACKAGE = "$JDK_PATH\bin\jpackage.exe"
$JLINK = "$JDK_PATH\bin\jlink.exe"
$JAVA = "$JDK_PATH\bin\java.exe"

if (-not (Test-Path $JPACKAGE)) {
    Write-Host ""
    Write-Host "ERROR: jpackage.exe not found at: $JPACKAGE" -ForegroundColor Red
    Write-Host "This JDK may not have jpackage tool. JDK 14+ is required." -ForegroundColor Yellow
    pause
    exit 1
}

# Display JDK version
if (Test-Path $JAVA) {
    $javaVersion = & $JAVA -version 2>&1 | Select-String -Pattern "version `"([^`"]+)" | ForEach-Object { $_.Matches[0].Groups[1].Value }
    Write-Host "  JDK Version: $javaVersion" -ForegroundColor Cyan
}

Write-Host "  JDK Path: $JDK_PATH" -ForegroundColor Green
Write-Host ""

# Step 1: Clean previous builds
Write-Host "[1/7] Cleaning previous builds..." -ForegroundColor Green
if (Test-Path installer_staging) {
    Remove-Item installer_staging -Recurse -Force
}
if (Test-Path "KumaraPOS-1.0.0.exe") {
    Remove-Item "KumaraPOS-1.0.0.exe" -Force
}
if (Test-Path "KumaraPOS") {
    Remove-Item "KumaraPOS" -Recurse -Force
}
Write-Host "Cleanup complete!" -ForegroundColor Green
Write-Host ""

# Step 2: Build the application
Write-Host "[2/7] Building application with Maven..." -ForegroundColor Green
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
Write-Host "[3/7] Looking for JAR file..." -ForegroundColor Green

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
Write-Host "[4/7] Verifying JAR contents..." -ForegroundColor Green

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

# Create a comprehensive runtime with ALL modules required for Spring Boot 3.x + JavaFX
# This runtime will be bundled with the installer so Java doesn't need to be pre-installed
Write-Host "Building comprehensive Java runtime for maximum compatibility..." -ForegroundColor Cyan
Write-Host "  Including all modules required for Spring Boot 3.x, JavaFX, and database connectivity..." -ForegroundColor Gray

# Check if JavaFX jmods exist (they might not be in standard JDK)
$javafxJmodsPath = "$JDK_PATH\jmods"
$hasJavafxJmods = (Test-Path "$javafxJmodsPath\javafx.base.jmod") -or (Test-Path "$javafxJmodsPath\javafx.controls.jmod")

# Function to check if a module exists in the JDK
function Test-ModuleExists {
    param([string]$moduleName)
    $jmodFile = "$JDK_PATH\jmods\$moduleName.jmod"
    return (Test-Path $jmodFile)
}

# Comprehensive module list for Spring Boot 3.x + JavaFX + Database + Security
# This ensures maximum compatibility across all Windows versions
# Note: java.lang.invoke is part of java.base, not a separate module
$moduleList = @(
    # Core Java modules
    "java.base",
    "java.desktop",
    "java.logging",
    "java.sql",
    "java.xml",
    "java.naming",
    "java.security.jgss",
    "java.security.sasl",
    "java.prefs",
    "java.management",
    "java.instrument",
    "java.compiler",
    "java.scripting",  # Required for javax.script (JavaScript engine, etc.)
    "jdk.unsupported",
    "jdk.crypto.ec",
    # Networking modules (required for Spring Boot)
    "java.net.http",
    "jdk.httpserver",
    # Note: java.lang.invoke is part of java.base, not a separate module
    # Serialization (required for Spring Boot)
    "java.serialization",
    # Charsets and encoding
    "jdk.charsets",
    # File system access
    "jdk.zipfs"
    # Note: jdk.incubator.vector is not a separate module in Java 21
    # Vector API functionality is part of java.base
)

# Add JavaFX modules if available in JDK (optional - JavaFX is in JAR dependencies)
if ($hasJavafxJmods) {
    Write-Host "  Including JavaFX modules from JDK (optional - also in JAR)..." -ForegroundColor Gray
    $moduleList += @("javafx.base", "javafx.controls", "javafx.fxml", "javafx.swing", "javafx.graphics", "javafx.media")
}

# Filter out modules that don't exist in the JDK
Write-Host "  Validating modules..." -ForegroundColor Gray
$modules = @()
foreach ($module in $moduleList) {
    if (Test-ModuleExists $module) {
        $modules += $module
        if ($module -eq "java.scripting") {
            Write-Host "    [OK] java.scripting module found (required for JavaFX FXML)" -ForegroundColor Green
        }
    } else {
        Write-Host "    Warning: Module $module not found, skipping..." -ForegroundColor Yellow
        if ($module -eq "java.scripting") {
            Write-Host "    ERROR: java.scripting is required but not found!" -ForegroundColor Red
            Write-Host "    This will cause javax.script.Bindings errors." -ForegroundColor Red
        }
    }
}

# Verify java.scripting is included
if ($modules -notcontains "java.scripting") {
    Write-Host ""
    Write-Host "ERROR: java.scripting module is required but not available!" -ForegroundColor Red
    Write-Host "This module is needed for JavaFX FXML loading (javax.script.Bindings)." -ForegroundColor Yellow
    Write-Host "Please use a JDK that includes the java.scripting module." -ForegroundColor Yellow
    pause
    exit 1
}

# Join modules into comma-separated string
$modulesString = $modules -join ","

$jlinkArgs = @(
    "--output", $runtimeDir,
    "--module-path", "$JDK_PATH\jmods",
    "--add-modules", $modulesString,
    "--strip-debug",
    "--compress", "2",
    "--no-header-files",
    "--no-man-pages"
)

Write-Host "  Creating runtime with $($modules.Count) modules..." -ForegroundColor Gray
& $JLINK $jlinkArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Failed to create custom runtime!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Trying with minimal module set..." -ForegroundColor Yellow
    
    # Fallback to minimal set if comprehensive set fails
    # Note: java.scripting is required for JavaFX FXML loading (javax.script.Bindings)
    $minimalModules = "java.base,java.desktop,java.logging,java.sql,java.xml,java.naming,java.security.jgss,java.security.sasl,java.scripting,jdk.unsupported,java.net.http,java.prefs"
    $jlinkArgs = @(
        "--output", $runtimeDir,
        "--module-path", "$JDK_PATH\jmods",
        "--add-modules", $minimalModules,
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    )
    
    & $JLINK $jlinkArgs
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to create runtime even with minimal modules!" -ForegroundColor Red
        pause
        exit 1
    } else {
        Write-Host "Runtime created with minimal module set (may have limited compatibility)" -ForegroundColor Yellow
    }
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

# Create logs directory for application logs
if (-not (Test-Path "logs")) {
    mkdir logs | Out-Null
}

Write-Host "Staging prepared!" -ForegroundColor Green
Write-Host "  JAR copied to: installer_staging\app.jar" -ForegroundColor Gray
Write-Host "  Logs directory created: logs\" -ForegroundColor Gray
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

# Check if user wants to enable console launcher (console disabled by default)
$createConsoleLauncher = $false
if ($args -contains "--console" -or $args -contains "-c") {
    $createConsoleLauncher = $true
    Write-Host "Console launcher enabled (errors will be visible)..." -ForegroundColor Green
} else {
    Write-Host "Console launcher disabled by default (no console window)..." -ForegroundColor Yellow
}

# Check for WiX tools (required for .exe installer)
# WiX is needed for creating Windows installers, but not for app-image (portable)
function Test-WiXAvailable {
    param([ref]$wixBinPath)

    # Check if WiX tools are in PATH
    $lightExe = Get-Command light.exe -ErrorAction SilentlyContinue
    $candleExe = Get-Command candle.exe -ErrorAction SilentlyContinue

    if ($lightExe -and $candleExe) {
        $wixBinPath.Value = Split-Path $lightExe.Source -Parent
        return $true
    }

    # Check WIX environment variable
    if ($env:WIX) {
        $wixBinPathCandidate = Join-Path $env:WIX "bin"
        if ((Test-Path "$wixBinPathCandidate\light.exe") -and (Test-Path "$wixBinPathCandidate\candle.exe")) {
            $wixBinPath.Value = $wixBinPathCandidate
            return $true
        }
    }

    # Check common WiX installation locations (including newer versions)
    $wixPaths = @(
        "${env:ProgramFiles}\WiX Toolset v3.14\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.14\bin",
        "${env:ProgramFiles}\WiX Toolset v3.13\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.13\bin",
        "${env:ProgramFiles}\WiX Toolset v3.12\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.12\bin",
        "${env:ProgramFiles}\WiX Toolset v3.11\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.11\bin",
        "${env:ProgramFiles}\WiX Toolset v3.10\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.10\bin",
        "${env:ProgramFiles}\WiX Toolset v3.9\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.9\bin"
    )

    # Also check for WiX in Program Files without version number
    $programFilesDirs = @("${env:ProgramFiles}", "${env:ProgramFiles(x86)}")
    foreach ($pfDir in $programFilesDirs) {
        if (Test-Path $pfDir) {
            $wixDirs = Get-ChildItem $pfDir -Directory -ErrorAction SilentlyContinue |
                       Where-Object { $_.Name -like "WiX*" -or $_.Name -like "*WiX*" }
            foreach ($wixDir in $wixDirs) {
                $binPath = Join-Path $wixDir.FullName "bin"
                if ((Test-Path "$binPath\light.exe") -and (Test-Path "$binPath\candle.exe")) {
                    $wixPaths += $binPath
                }
            }
        }
    }

    foreach ($wixPath in $wixPaths) {
        if ((Test-Path "$wixPath\light.exe") -and (Test-Path "$wixPath\candle.exe")) {
            $wixBinPath.Value = $wixPath
            return $true
        }
    }

    # Check registry for WiX installation
    $regPaths = @(
        "HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*"
    )
    foreach ($regPath in $regPaths) {
        $wixInstall = Get-ItemProperty $regPath -ErrorAction SilentlyContinue |
                      Where-Object { $_.DisplayName -like "*WiX*" -or $_.DisplayName -like "*Windows Installer XML*" } |
                      Select-Object -First 1
        if ($wixInstall -and $wixInstall.InstallLocation) {
            $binPath = Join-Path $wixInstall.InstallLocation "bin"
            if ((Test-Path "$binPath\light.exe") -and (Test-Path "$binPath\candle.exe")) {
                $wixBinPath.Value = $binPath
                return $true
            }
        }
    }

    return $false
}

# Function to install WiX using winget or chocolatey
function Install-WiX {
    Write-Host ""
    Write-Host "Attempting to install WiX Toolset automatically..." -ForegroundColor Cyan
    Write-Host ""

    # Try winget first (Windows 10/11 built-in)
    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if ($winget) {
        Write-Host "Found winget - attempting to install WiX..." -ForegroundColor Green
        try {
            $wingetOutput = & winget install --id WiXToolset.WiXToolset --accept-package-agreements --accept-source-agreements 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "WiX installed successfully via winget!" -ForegroundColor Green
                # Refresh PATH
                $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
                return $true
            } else {
                Write-Host "winget installation failed. Trying chocolatey..." -ForegroundColor Yellow
            }
        } catch {
            Write-Host "winget installation failed. Trying chocolatey..." -ForegroundColor Yellow
        }
    }

    # Try chocolatey
    $choco = Get-Command choco.exe -ErrorAction SilentlyContinue
    if ($choco) {
        Write-Host "Found chocolatey - attempting to install WiX..." -ForegroundColor Green
        try {
            # Run chocolatey install (requires admin)
            $chocoOutput = & choco install wix --yes --accept-license 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "WiX installed successfully via chocolatey!" -ForegroundColor Green
                # Refresh PATH
                $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
                return $true
            }
        } catch {
            Write-Host "chocolatey installation failed." -ForegroundColor Yellow
        }
    }

    Write-Host ""
    Write-Host "Automatic installation failed or package managers not available." -ForegroundColor Yellow
    Write-Host "Please install WiX manually:" -ForegroundColor Cyan
    Write-Host "  1. Download from: https://wixtoolset.org/docs/wix3/" -ForegroundColor White
    Write-Host "  2. Or use winget: winget install WiXToolset.WiXToolset" -ForegroundColor White
    Write-Host "  3. Or use chocolatey: choco install wix" -ForegroundColor White
    Write-Host "  4. After installation, add WiX bin directory to PATH" -ForegroundColor White
    Write-Host ""
    return $false
}

# Determine installer type - prioritize creating installer (.exe) over portable app
$installerType = "exe"
$useAppImage = $false
$wixBinPath = ""
$wixAvailable = Test-WiXAvailable -wixBinPath ([ref]$wixBinPath)

# Check if user explicitly wants app-image
if ($args -contains "--app-image" -or $args -contains "-p" -or $args -contains "--portable") {
    $installerType = "app-image"
    $useAppImage = $true
    Write-Host "Creating portable app-image (no installation required)..." -ForegroundColor Cyan
} elseif (-not $wixAvailable) {
    Write-Host ""
    Write-Host "WiX tools not found - attempting to install automatically..." -ForegroundColor Yellow
    Write-Host "  WiX (Windows Installer XML) is required to create .exe installers." -ForegroundColor Yellow
    Write-Host ""

    # Automatically try to install WiX (unless user explicitly wants app-image)
    $tryAutoInstall = $true
    if ($args -contains "--no-auto-install-wix") {
        $tryAutoInstall = $false
        Write-Host "Skipping automatic WiX installation (--no-auto-install-wix flag detected)" -ForegroundColor Yellow
    }

    if ($tryAutoInstall) {
        Write-Host "Attempting automatic WiX installation..." -ForegroundColor Cyan
        $installed = Install-WiX
        if ($installed) {
            # Re-check for WiX after installation
            $wixAvailable = Test-WiXAvailable -wixBinPath ([ref]$wixBinPath)
            if ($wixAvailable) {
                Write-Host ""
                Write-Host "WiX tools found - creating .exe installer..." -ForegroundColor Green
                if ($wixBinPath -and $wixBinPath -notin $env:Path) {
                    Write-Host "Adding WiX to PATH for this session..." -ForegroundColor Yellow
                    $env:Path = "$wixBinPath;$env:Path"
                }
            } else {
                Write-Host ""
                Write-Host "WiX installation completed but tools not found in PATH." -ForegroundColor Yellow
                Write-Host "Please restart PowerShell or add WiX bin directory to PATH manually." -ForegroundColor Yellow
                Write-Host ""
                Write-Host "To create an installer, please:" -ForegroundColor Cyan
                Write-Host "  1. Restart PowerShell and run this script again" -ForegroundColor White
                Write-Host "  2. Or manually add WiX to PATH" -ForegroundColor White
                Write-Host "  3. Or install WiX manually from: https://wixtoolset.org/" -ForegroundColor White
                Write-Host ""
                Write-Host "Alternatively, create a portable version with: .\deploy-installer.ps1 --app-image" -ForegroundColor Cyan
                pause
                exit 1
            }
        } else {
            Write-Host ""
            Write-Host "Automatic WiX installation failed." -ForegroundColor Red
            Write-Host ""
            Write-Host "To create an installer, please install WiX manually:" -ForegroundColor Cyan
            Write-Host "  1. Download from: https://wixtoolset.org/docs/wix3/" -ForegroundColor White
            Write-Host "  2. Or use winget: winget install WiXToolset.WiXToolset" -ForegroundColor White
            Write-Host "  3. Or use chocolatey: choco install wix" -ForegroundColor White
            Write-Host "  4. After installation, run this script again" -ForegroundColor White
            Write-Host ""
            Write-Host "Alternatively, create a portable version with: .\deploy-installer.ps1 --app-image" -ForegroundColor Cyan
            Write-Host ""
            pause
            exit 1
        }
    } else {
        Write-Host ""
        Write-Host "WiX is required to create an installer." -ForegroundColor Red
        Write-Host "Please install WiX manually or run without --no-auto-install-wix flag." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To create an installer, please install WiX:" -ForegroundColor Cyan
        Write-Host "  1. Download from: https://wixtoolset.org/docs/wix3/" -ForegroundColor White
        Write-Host "  2. Or use winget: winget install WiXToolset.WiXToolset" -ForegroundColor White
        Write-Host "  3. Or use chocolatey: choco install wix" -ForegroundColor White
        Write-Host "  4. After installation, run this script again" -ForegroundColor White
        Write-Host ""
        pause
        exit 1
    }
} else {
    Write-Host "WiX tools found - creating .exe installer..." -ForegroundColor Green
    if ($wixBinPath -and $wixBinPath -notin $env:Path) {
        Write-Host "Adding WiX to PATH for this session: $wixBinPath" -ForegroundColor Yellow
        $env:Path = "$wixBinPath;$env:Path"
    }
}

# Build jpackage command with bundled Java runtime
# This creates a self-contained installer that works on any Windows PC without Java pre-installed
# Enhanced with maximum compatibility options for all Windows versions
$jpackageArgs = @(
    "--name", "KumaraPOS",
    "--app-version", "1.0.0",
    "--vendor", "Kumara Enterprises",
    "--description", "Point of Sale System - Self Contained Installer",
    "--type", $installerType,
    "--input", "installer_staging",
    "--main-jar", "app.jar",
    "--main-class", "org.springframework.boot.loader.launch.JarLauncher",
    "--runtime-image", $runtimeDir
)

# Add Windows-specific options only for .exe installer
if (-not $useAppImage) {
    $jpackageArgs += @(
        "--win-shortcut",
        "--win-menu",
        "--win-dir-chooser",
        "--win-per-user-install"
    )
} else {
    # For app-image, we can still add shortcuts but they're created differently
    Write-Host "  Note: app-image creates a portable folder (no installer needed)" -ForegroundColor Gray
}

# Add console launcher (enabled by default to show errors)
if ($createConsoleLauncher) {
    $jpackageArgs += "--win-console"
    Write-Host "  Console launcher: ENABLED (errors will be visible)" -ForegroundColor Green
} else {
    Write-Host "  Console launcher: DISABLED (errors will not be visible)" -ForegroundColor Yellow
}

# Add Java options (including JavaFX-specific options)
$jpackageArgs += @(
    "--java-options", "-Xmx1024m",
    "--java-options", "-Xms256m",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Djava.awt.headless=false",
    "--java-options", "-Djava.net.useSystemProxies=true",
    "--java-options", "-Duser.timezone=UTC",
    # Core module opens
    "--java-options", "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    # Desktop/AWT module opens (required for JavaFX)
    "--java-options", "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
    "--java-options", "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
    "--java-options", "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--java-options", "--add-opens=java.desktop/sun.java2d=ALL-UNNAMED",
    "--java-options", "--add-opens=java.desktop/sun.font=ALL-UNNAMED",
    "--java-options", "--add-opens=java.desktop/sun.swing=ALL-UNNAMED",
    # Preferences module opens
    "--java-options", "--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED",
    # Exports for JavaFX
    "--java-options", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
    "--java-options", "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED",
    "--java-options", "--add-exports=java.desktop/sun.font=ALL-UNNAMED",
    # JavaFX module opens (if JavaFX is in classpath)
    "--java-options", "--add-opens=javafx.base/com.sun.javafx=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED",
    # GC options
    "--java-options", "-XX:+UseG1GC",
    "--java-options", "-XX:MaxGCPauseMillis=200"
)

# Check Windows version for compatibility
$osVersion = [System.Environment]::OSVersion.Version
$isWindows10OrLater = ($osVersion.Major -ge 10)
Write-Host "Detected Windows version: $($osVersion.Major).$($osVersion.Minor)" -ForegroundColor Cyan

if (-not $isWindows10OrLater) {
    Write-Host "WARNING: Windows 10 or later is recommended for best compatibility" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Running jpackage with the following configuration:" -ForegroundColor Cyan
Write-Host "  Type: $installerType" -ForegroundColor $(if ($useAppImage) { "Yellow" } else { "Green" })
Write-Host "  Main JAR: app.jar" -ForegroundColor White
Write-Host "  Main Class: org.springframework.boot.loader.launch.JarLauncher (Spring Boot)" -ForegroundColor White
Write-Host "  Start Class: com.devstack.pos.PosApplication" -ForegroundColor White
Write-Host "  Runtime: Custom Java runtime (bundled - no Java installation required)" -ForegroundColor Green
Write-Host '  Compatibility: Enhanced for all Windows 10/11 versions (64-bit)' -ForegroundColor Green
Write-Host "  Java Options: Optimized for Spring Boot 3.x + JavaFX" -ForegroundColor Green
Write-Host ""

# Verify WiX is accessible if creating .exe installer
if (-not $useAppImage) {
    $lightCheck = Get-Command light.exe -ErrorAction SilentlyContinue
    $candleCheck = Get-Command candle.exe -ErrorAction SilentlyContinue
    if (-not ($lightCheck -and $candleCheck)) {
        Write-Host ""
        Write-Host "ERROR: WiX tools (light.exe, candle.exe) are not accessible in PATH!" -ForegroundColor Red
        Write-Host "  Even though WiX was detected, the tools cannot be found when running jpackage." -ForegroundColor Yellow
        if ($wixBinPath) {
            Write-Host "  WiX bin path found: $wixBinPath" -ForegroundColor Cyan
            Write-Host "  Attempting to add to PATH for this session..." -ForegroundColor Yellow
            $env:Path = "$wixBinPath;$env:Path"
            # Re-check
            $lightCheck = Get-Command light.exe -ErrorAction SilentlyContinue
            $candleCheck = Get-Command candle.exe -ErrorAction SilentlyContinue
            if (-not ($lightCheck -and $candleCheck)) {
                Write-Host "  Failed to make WiX accessible. Please add WiX bin directory to system PATH." -ForegroundColor Red
                Write-Host "  WiX bin directory: $wixBinPath" -ForegroundColor Yellow
                Write-Host ""
                Write-Host "  Alternatively, use app-image instead: .\deploy-installer.ps1 --app-image" -ForegroundColor Cyan
                pause
                exit 1
            } else {
                Write-Host "  WiX tools are now accessible!" -ForegroundColor Green
            }
        } else {
            Write-Host "  Please install WiX and add it to your PATH, or use app-image instead." -ForegroundColor Yellow
            Write-Host "  To use app-image: .\deploy-installer.ps1 --app-image" -ForegroundColor Cyan
            pause
            exit 1
        }
    } else {
        Write-Host "  WiX tools verified and accessible" -ForegroundColor Green
    }
    Write-Host ""
}

# Run jpackage with error handling
if ($useAppImage) {
    Write-Host "Creating portable application (this may take several minutes)..." -ForegroundColor Yellow
} else {
    Write-Host "Creating installer (this may take several minutes)..." -ForegroundColor Yellow
}
& $JPACKAGE $jpackageArgs 2>&1 | Tee-Object -Variable jpackageOutput

# Check if there were warnings but it still succeeded
$hasWarnings = $jpackageOutput | Select-String -Pattern "warning|WARNING" -Quiet

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "     SUCCESS!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""

    if ($useAppImage) {
        # App-image creates a folder, not an installer
        $appImageFolder = "KumaraPOS"
        if (Test-Path $appImageFolder) {
            Write-Host "Portable application created: " -NoNewline
            Write-Host $appImageFolder -ForegroundColor Cyan
            Write-Host "Location: " -NoNewline
            Write-Host "$(Get-Location)\$appImageFolder" -ForegroundColor Yellow
            Write-Host ""

            $folderSize = (Get-ChildItem $appImageFolder -Recurse -ErrorAction SilentlyContinue |
                           Measure-Object -Property Length -Sum).Sum / 1MB
            Write-Host "Folder size: " -NoNewline
            Write-Host "$([math]::Round($folderSize, 2)) MB" -ForegroundColor Cyan
            Write-Host ""

            $exePath = Join-Path $appImageFolder "KumaraPOS.exe"
            if (Test-Path $exePath) {
                Write-Host "Application executable: " -NoNewline
                Write-Host $exePath -ForegroundColor Green
                Write-Host ""
            }

            Write-Host "You can now:" -ForegroundColor Green
            Write-Host "  1. Run the application directly: .\$appImageFolder\KumaraPOS.exe" -ForegroundColor White
            Write-Host "  2. Copy the entire '$appImageFolder' folder to any Windows computer" -ForegroundColor White
            Write-Host "  3. No installation required - just run KumaraPOS.exe" -ForegroundColor White
            Write-Host ""
            Write-Host "IMPORTANT: This is a PORTABLE application!" -ForegroundColor Green
            Write-Host "  - No Java installation required on target computers" -ForegroundColor White
            Write-Host '  - Works on Windows 10/11 (64-bit) - all versions' -ForegroundColor White
            Write-Host "  - Java runtime is bundled in the folder" -ForegroundColor White
            Write-Host "  - Just copy the folder and run - no installer needed!" -ForegroundColor White
            Write-Host "  - Enhanced compatibility with all required Java modules" -ForegroundColor White
            Write-Host "  - Optimized Java options for Spring Boot 3.x + JavaFX" -ForegroundColor White
            Write-Host ""
        } else {
            Write-Host "WARNING: App-image folder not found at expected location!" -ForegroundColor Yellow
        }
    } else {
        # .exe installer
        $installerFile = "KumaraPOS-1.0.0.exe"
        if (Test-Path $installerFile) {
            Write-Host "Installer created: " -NoNewline
            Write-Host $installerFile -ForegroundColor Cyan
            Write-Host "Location: " -NoNewline
            Write-Host "$(Get-Location)" -ForegroundColor Yellow
            Write-Host ""

            $fileSize = (Get-Item $installerFile).Length / 1MB
            Write-Host "File size: " -NoNewline
            Write-Host "$([math]::Round($fileSize, 2)) MB" -ForegroundColor Cyan
            Write-Host ""

            # Verify installer integrity
            Write-Host "Verifying installer..." -ForegroundColor Cyan
            $fileInfo = Get-Item $installerFile
            if ($fileInfo.Length -gt 50MB) {
                Write-Host "  Installer size looks good (contains bundled Java runtime)" -ForegroundColor Green
            } else {
                Write-Host "  WARNING: Installer seems small - may not contain runtime" -ForegroundColor Yellow
            }
            Write-Host ""

            Write-Host "You can now:" -ForegroundColor Green
            $runCommand = ".\" + $installerFile
            Write-Host "  1. Test the installer by running: $runCommand" -ForegroundColor White
            Write-Host "  2. Distribute this file to other Windows computers" -ForegroundColor White
            Write-Host ""
            Write-Host "IMPORTANT: This is a SELF-CONTAINED installer!" -ForegroundColor Green
            Write-Host "  - No Java installation required on target computers" -ForegroundColor White
            Write-Host '  - Works on Windows 10/11 (64-bit) - all versions' -ForegroundColor White
            Write-Host "  - Java runtime is bundled with the installer" -ForegroundColor White
            Write-Host "  - Enhanced compatibility with all required Java modules" -ForegroundColor White
            Write-Host "  - Optimized Java options for Spring Boot 3.x + JavaFX" -ForegroundColor White
            Write-Host ""
        } else {
            if ($useAppImage) {
                Write-Host "WARNING: App-image folder not found at expected location!" -ForegroundColor Yellow
            } else {
                Write-Host "WARNING: Installer file not found at expected location!" -ForegroundColor Yellow
            }
            Write-Host "Check the jpackage output above for details." -ForegroundColor Yellow
            Write-Host ""
        }
    }

    # Show warnings and troubleshooting info (outside the file/folder check)
    if ($hasWarnings) {
        Write-Host "NOTE: Some warnings were detected but application was created successfully." -ForegroundColor Yellow
        Write-Host "      The application should still work correctly." -ForegroundColor Yellow
        Write-Host ""
    }

    Write-Host "TROUBLESHOOTING:" -ForegroundColor Cyan
    Write-Host '  If the application does not run on some Windows versions:' -ForegroundColor White
    Write-Host '  1. Ensure target PC is Windows 10/11 (64-bit)' -ForegroundColor Gray
    Write-Host "  2. Run as Administrator if needed" -ForegroundColor Gray
    Write-Host "  3. Check Windows Event Viewer for detailed errors" -ForegroundColor Gray
    Write-Host '  4. Ensure antivirus is not blocking the application' -ForegroundColor Gray
    Write-Host "  5. Try creating a portable version (app-image) instead" -ForegroundColor Gray
    Write-Host ""
    if (-not $useAppImage) {
        Write-Host "OPTIONAL: Create portable version (app-image)?" -ForegroundColor Cyan
        Write-Host '  A portable version does not require installation and may work better on some systems.' -ForegroundColor Gray
        Write-Host "  To create it, run this script again with: .\deploy-installer.ps1 --app-image" -ForegroundColor Gray
        Write-Host ""
    }
    Write-Host "DEBUGGING TOOLS:" -ForegroundColor Cyan
    Write-Host '  If the application does not work or nothing happens when you click it:' -ForegroundColor White
    Write-Host ""
    Write-Host "  1. Recreate with console launcher (RECOMMENDED):" -ForegroundColor Yellow
    Write-Host "     .\deploy-installer.ps1 --console" -ForegroundColor Gray
    Write-Host "     (This creates an application that shows errors in a console window)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  2. Test JAR before packaging:" -ForegroundColor Yellow
    Write-Host "     java -jar target\pos-1.0.0.jar" -ForegroundColor Gray
    Write-Host "     (This runs the JAR with visible console to see errors)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  3. View application logs:" -ForegroundColor Yellow
    Write-Host "     Get-Content logs\kumarapos.log" -ForegroundColor Gray
    Write-Host "     (Shows log files from the application)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  4. Check Windows Event Viewer:" -ForegroundColor Yellow
    Write-Host "     eventvwr.msc" -ForegroundColor Gray
    Write-Host "     (Checks Windows Event Viewer for errors)" -ForegroundColor DarkGray
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "     FAILED!" -ForegroundColor Red
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host ""
    if ($useAppImage) {
        Write-Host "Portable application creation failed!" -ForegroundColor Red
    } else {
        Write-Host "Installer creation failed!" -ForegroundColor Red
    }
    Write-Host ""

    # Check if the error is WiX-related
    $wixError = $jpackageOutput | Select-String -Pattern "WiX|wix|light\.exe|candle\.exe|Windows Installer XML" -Quiet
    if ($wixError -and -not $useAppImage) {
        Write-Host "ERROR DETECTED: WiX-related issue!" -ForegroundColor Red
        Write-Host ""
        Write-Host "The installer creation failed because WiX tools are not properly configured." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Solutions:" -ForegroundColor Cyan
        Write-Host "  1. Install WiX Toolset:" -ForegroundColor White
        Write-Host "     - Download from: https://wixtoolset.org/docs/wix3/" -ForegroundColor Gray
        Write-Host "     - Or use: winget install WiXToolset.WiXToolset" -ForegroundColor Gray
        Write-Host "     - Or use: choco install wix" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  2. Add WiX to PATH:" -ForegroundColor White
        if ($wixBinPath) {
            Write-Host "     WiX bin directory: $wixBinPath" -ForegroundColor Gray
            Write-Host "     Add this to your system PATH environment variable" -ForegroundColor Gray
        } else {
            Write-Host "     Find WiX installation (usually in Program Files\WiX Toolset v3.x\bin)" -ForegroundColor Gray
            Write-Host "     Add the 'bin' directory to your system PATH" -ForegroundColor Gray
        }
        Write-Host ""
        Write-Host "  3. Use portable app-image instead (no WiX needed):" -ForegroundColor White
        Write-Host "     .\deploy-installer.ps1 --app-image" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  4. Try automatic WiX installation:" -ForegroundColor White
        Write-Host "     .\deploy-installer.ps1 --install-wix" -ForegroundColor Gray
        Write-Host "     (Requires admin rights and winget/chocolatey)" -ForegroundColor DarkGray
        Write-Host ""
    } else {
        Write-Host "Common issues and solutions:" -ForegroundColor Yellow
        Write-Host "  1. Missing dependencies:" -ForegroundColor White
        Write-Host "     - Ensure JDK 21 is properly installed" -ForegroundColor Gray
        Write-Host "     - Verify jpackage.exe exists at: $JPACKAGE" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  2. Runtime creation issues:" -ForegroundColor White
        Write-Host "     - Check that jlink.exe exists at: $JLINK" -ForegroundColor Gray
        Write-Host "     - Verify JDK jmods directory exists" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  3. Windows-specific issues:" -ForegroundColor White
        Write-Host "     - Run PowerShell as Administrator" -ForegroundColor Gray
        Write-Host '     - Check antivirus is not blocking jpackage' -ForegroundColor Gray
        Write-Host "     - Ensure sufficient disk space (need ~500MB free)" -ForegroundColor Gray
        Write-Host ""
        if (-not $useAppImage) {
            Write-Host "  4. WiX tools missing (for .exe installer):" -ForegroundColor White
            Write-Host "     - Install WiX from: https://wixtoolset.org/" -ForegroundColor Gray
            Write-Host "     - Or use portable app-image instead: .\deploy-installer.ps1 --app-image" -ForegroundColor Gray
            Write-Host ""
        }
        Write-Host "  5. Check the error messages above for specific details." -ForegroundColor Yellow
        Write-Host ""
    }

    # Try to provide more diagnostic info
    if (Test-Path $runtimeDir) {
        Write-Host "Runtime directory exists: $runtimeDir" -ForegroundColor Cyan
        $runtimeSize = (Get-ChildItem $runtimeDir -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1MB
        Write-Host "  Runtime size: $([math]::Round($runtimeSize, 2)) MB" -ForegroundColor Gray
    } else {
        Write-Host "Runtime directory missing - runtime creation may have failed" -ForegroundColor Red
    }
    Write-Host ""
}

pause
pause