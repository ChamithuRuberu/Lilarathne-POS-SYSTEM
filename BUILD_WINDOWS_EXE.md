# Building Windows .exe File - Step by Step Guide

This guide will walk you through building a Windows executable (.exe) file for the Kumara Enterprises POS System.

## 📋 Prerequisites Checklist

Before building, ensure you have:

- [ ] **Java JDK 21 or higher** (JDK, not just JRE)
  - Download: https://www.oracle.com/java/technologies/downloads/
  - Or OpenJDK: https://adoptium.net/
  - Verify: `java -version` and `javac -version`
  
- [ ] **Maven 3.6+**
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`
  
- [ ] **WiX Toolset 3.11+** (Optional, but recommended for better installers)
  - Download: https://wixtoolset.org/releases/
  - Add to PATH after installation
  - Verify: `candle -?` (if added to PATH)

- [ ] **PostgreSQL Database** (for testing)
  - The application needs database connection to run

---

## 🚀 Method 1: Using the Automated Build Script (Recommended)

### Step 1: Open Command Prompt
1. Press `Win + R`
2. Type `cmd` and press Enter
3. Navigate to your project directory:
   ```batch
   cd C:\path\to\Lilarathne-POS-SYSTEM
   ```

### Step 2: Run the Build Script
Simply double-click `build-windows.bat` or run:
```batch
build-windows.bat
```

The script will:
1. ✅ Check if Maven and Java are installed
2. ✅ Clean previous builds
3. ✅ Build the JAR file
4. ✅ Create the Windows executable (if jpackage is available)

### Step 3: Find Your Executable
After successful build, your .exe file will be at:
```
target\windows-installer\Kumara Enterprises POS System.exe
```

---

## 🔧 Method 2: Manual Build (Step by Step)

If you prefer to build manually or the script doesn't work:

### Step 1: Build the JAR File
```batch
mvn clean package -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: XX.XXX s
[INFO] Finished at: YYYY-MM-DD HH:MM:SS
[INFO] ------------------------------------------------------------------------
```

**Verify JAR exists:**
```batch
dir target\pos-1.0.0.jar
```

### Step 2: Check jpackage Availability
```batch
jpackage --version
```

**If jpackage is available**, proceed to Step 3.
**If not**, you need to:
- Install JDK 14+ (jpackage comes with JDK, not JRE)
- Make sure you're using JDK, not just JRE

### Step 3: Create Windows Executable
```batch
jpackage --input target ^
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
```

**Note:** The `^` character is a line continuation in Windows batch files. If running in PowerShell, use backtick `` ` `` instead.

### Step 4: Verify the Executable
```batch
dir target\windows-installer
```

You should see:
- `Kumara Enterprises POS System.exe`

---

## 🎯 Method 3: Using Maven Profile

You can also use the Maven profile we've configured:

```batch
mvn clean package -Pwindows -DskipTests
```

This will automatically run jpackage after building the JAR.

---

## 📦 Building MSI Installer (Advanced)

For a professional Windows installer package:

### Step 1: Install WiX Toolset
1. Download from: https://wixtoolset.org/releases/
2. Install it
3. Add to PATH (usually: `C:\Program Files (x86)\WiX Toolset v3.11\bin`)

### Step 2: Build MSI
```batch
jpackage --input target ^
    --name "Kumara Enterprises POS System" ^
    --main-jar pos-1.0.0.jar ^
    --main-class com.devstack.pos.PosApplication ^
    --type msi ^
    --dest target\windows-installer ^
    --app-version 1.0.0 ^
    --vendor "DevStack" ^
    --description "Point of Sale System" ^
    --win-menu ^
    --win-shortcut ^
    --win-dir-chooser ^
    --win-upgrade-uuid "YOUR-UNIQUE-UUID-HERE"
```

**Generate UUID:**
- Online: https://www.uuidgenerator.net/
- PowerShell: `[guid]::NewGuid()`

---

## ⚙️ Configuration Options

### Memory Settings
If you need to adjust memory for the executable, you can add JVM options:

```batch
jpackage --input target ^
    --name "Kumara Enterprises POS System" ^
    --main-jar pos-1.0.0.jar ^
    --main-class com.devstack.pos.PosApplication ^
    --type exe ^
    --dest target\windows-installer ^
    --java-options "-Xmx2048m" ^
    --java-options "-Xms512m" ^
    --app-version 1.0.0 ^
    --vendor "DevStack" ^
    --description "Point of Sale System" ^
    --win-menu ^
    --win-shortcut ^
    --win-dir-chooser
```

### Adding Application Icon
1. Create or obtain an `.ico` file (256x256 recommended)
2. Place it in the project root as `app-icon.ico`
3. Add to jpackage command:
   ```batch
   --icon app-icon.ico ^
   ```

### Including Additional Files
If you need to include configuration files or resources:
```batch
--resource-dir src\resources ^
```

---

## 🐛 Troubleshooting

### Issue: "jpackage is not recognized"
**Solution:**
- Ensure you have JDK (not just JRE) installed
- JDK 14+ includes jpackage
- Check: `java -version` should show JDK
- Add JDK `bin` folder to PATH

### Issue: "WiX Toolset not found" (for MSI)
**Solution:**
- Install WiX Toolset from https://wixtoolset.org/
- Add WiX bin directory to PATH
- Restart command prompt

### Issue: "Main class not found"
**Solution:**
- Verify the JAR was built correctly: `jar tf target\pos-1.0.0.jar | findstr PosApplication`
- Check main class name matches: `com.devstack.pos.PosApplication`

### Issue: "JavaFX runtime not found"
**Solution:**
- JavaFX dependencies should be bundled in the JAR
- If not, you may need to use `--module-path` and `--add-modules`
- Check pom.xml includes JavaFX dependencies

### Issue: Build fails with "Out of memory"
**Solution:**
- Increase Maven memory:
  ```batch
  set MAVEN_OPTS=-Xmx2048m
  mvn clean package
  ```

### Issue: Executable runs but shows database error
**Solution:**
- The .exe still needs PostgreSQL connection
- Ensure database is accessible
- Update `application.properties` before building, or
- Provide database configuration at runtime

### Issue: "The system cannot find the path specified"
**Solution:**
- Ensure you're in the correct directory
- Check that `target` folder exists after `mvn package`
- Verify JAR file exists: `dir target\*.jar`

---

## ✅ Verification Checklist

After building, verify:

- [ ] JAR file exists: `target\pos-1.0.0.jar`
- [ ] Executable exists: `target\windows-installer\Kumara Enterprises POS System.exe`
- [ ] File size is reasonable (should be 50-200 MB, includes Java runtime)
- [ ] Can double-click and run (may need database connection)
- [ ] Start Menu shortcut created (if `--win-menu` was used)

---

## 📤 Distribution

### What to Distribute:

1. **Just the .exe file:**
   - `Kumara Enterprises POS System.exe`
   - Users can double-click to run
   - No installation needed (portable)

2. **MSI Installer (if built):**
   - `Kumara Enterprises POS System-1.0.0.msi`
   - Professional installation experience
   - Creates Start Menu shortcuts
   - Can be uninstalled via Control Panel

### Distribution Checklist:

- [ ] Test .exe on a clean Windows machine (without Java installed)
- [ ] Verify database connection works
- [ ] Include database setup instructions
- [ ] Provide user documentation
- [ ] Include `application.properties` template (if users need to configure)

---

## 🔄 Updating the Application

When you need to update:

1. Make your code changes
2. Update version in `pom.xml`:
   ```xml
   <version>1.0.1</version>
   ```
3. Rebuild:
   ```batch
   mvn clean package -DskipTests
   jpackage ... (same command as before)
   ```
4. Distribute new .exe file

---

## 📝 Quick Reference Commands

```batch
REM Build JAR only
mvn clean package -DskipTests

REM Build with Windows profile
mvn clean package -Pwindows -DskipTests

REM Create .exe manually
jpackage --input target --name "Kumara Enterprises POS System" --main-jar pos-1.0.0.jar --main-class com.devstack.pos.PosApplication --type exe --dest target\windows-installer --app-version 1.0.0 --vendor "DevStack" --description "Point of Sale System" --win-menu --win-shortcut --win-dir-chooser

REM Check Java version
java -version

REM Check jpackage
jpackage --version

REM Check Maven
mvn -version
```

---

## 💡 Tips

1. **Test First:** Always test the JAR file before creating .exe:
   ```batch
   java -jar target\pos-1.0.0.jar
   ```

2. **Clean Build:** If you encounter issues, do a clean build:
   ```batch
   mvn clean
   mvn package -DskipTests
   ```

3. **File Size:** The .exe will be large (50-200 MB) because it includes Java runtime. This is normal.

4. **Database:** Remember, the .exe still needs PostgreSQL. Consider:
   - Bundling PostgreSQL installer
   - Providing setup instructions
   - Using embedded database for simpler deployment

5. **Signing:** For production, consider code signing the .exe to avoid Windows security warnings.

---

## 🆘 Need Help?

If you encounter issues:
1. Check the error message carefully
2. Verify all prerequisites are installed
3. Try building JAR first: `mvn clean package`
4. Test JAR: `java -jar target\pos-1.0.0.jar`
5. Check JavaFX dependencies in pom.xml
6. Review application logs for runtime errors

---

**Good luck with your build! 🚀**

