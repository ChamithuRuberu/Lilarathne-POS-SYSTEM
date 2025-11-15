# Quick Build Guide - Windows .exe

## 🚀 Fastest Way (3 Steps)

### Step 1: Install Prerequisites
- ✅ **Java JDK 21+** (not JRE) - https://adoptium.net/
- ✅ **Maven 3.6+** - https://maven.apache.org/download.cgi

### Step 2: Run Build Script
**Option A - Batch File:**
```batch
build-windows.bat
```

**Option B - PowerShell:**
```powershell
.\build-windows.ps1
```

### Step 3: Find Your .exe
```
target\windows-installer\Kumara Enterprises POS System.exe
```

---

## 📋 Prerequisites Check

Run these commands to verify:

```batch
java -version    # Should show JDK 21+
mvn -version     # Should show Maven 3.6+
jpackage --version  # Should show version (optional but recommended)
```

---

## ⚡ Quick Commands

### Build JAR Only
```batch
mvn clean package -DskipTests
```

### Build .exe (if jpackage available)
```batch
jpackage --input target --name "Kumara Enterprises POS System" --main-jar pos-1.0.0.jar --main-class com.devstack.pos.PosApplication --type exe --dest target\windows-installer --app-version 1.0.0 --vendor "DevStack" --description "Point of Sale System" --win-menu --win-shortcut --win-dir-chooser
```

### Test JAR Before Building .exe
```batch
java -jar target\pos-1.0.0.jar
```

---

## ❌ Common Issues

| Issue | Solution |
|-------|----------|
| "jpackage not found" | Install JDK (not JRE) - jpackage comes with JDK |
| "Maven not found" | Add Maven to PATH or install Maven |
| "Java not found" | Install Java and add to PATH |
| Build fails | Run `mvn clean` first, then rebuild |
| .exe won't run | Check database connection, ensure PostgreSQL is running |

---

## 📖 Full Documentation

For detailed instructions, see: **BUILD_WINDOWS_EXE.md**

---

## ✅ Success Checklist

After building:
- [ ] JAR file exists: `target\pos-1.0.0.jar`
- [ ] .exe file exists: `target\windows-installer\Kumara Enterprises POS System.exe`
- [ ] File size is 50-200 MB (normal, includes Java runtime)
- [ ] Can double-click to run (needs database connection)

---

**That's it! Your .exe is ready to distribute! 🎉**

