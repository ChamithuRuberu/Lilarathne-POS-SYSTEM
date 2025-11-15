# Windows Deployment Guide

This guide explains how to deploy the Kumara Enterprises POS System on Windows.

## Prerequisites

Before deploying the application, ensure you have the following installed:

1. **Java 21 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Or use OpenJDK: https://adoptium.net/
   - Verify installation: `java -version`

2. **Maven 3.6+** (for building)
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **PostgreSQL Database**
   - Download from: https://www.postgresql.org/download/windows/
   - The application requires PostgreSQL to be running
   - Default connection: `localhost:5432`
   - Database name: `robotikka`
   - Username: `postgres`
   - Password: `1234` (change in `application.properties` for production)

## Deployment Options

### Option 1: Run as JAR File (Simplest)

This is the easiest way to deploy the application.

#### Steps:

1. **Build the application:**
   ```batch
   build-windows.bat
   ```
   Or manually:
   ```batch
   mvn clean package
   ```

2. **Run the application:**
   ```batch
   run-windows.bat
   ```
   Or manually:
   ```batch
   java -jar target\pos-1.0.0.jar
   ```

#### Advantages:
- Simple and quick
- No additional tools required
- Easy to update (just replace the JAR file)

#### Disadvantages:
- Requires Java to be installed on target machines
- Users need to run from command line or batch file

---

### Option 2: Native Windows Executable (Recommended for End Users)

This creates a standalone `.exe` file that users can double-click to run.

#### Requirements:
- Java 14+ with `jpackage` tool (included in JDK)
- WiX Toolset 3.11+ (for creating MSI installer)
  - Download from: https://wixtoolset.org/
  - Add to PATH after installation

#### Steps:

1. **Build the JAR file:**
   ```batch
   mvn clean package
   ```

2. **Create Windows executable:**
   ```batch
   build-windows.bat
   ```
   
   Or manually using jpackage:
   ```batch
   jpackage --input target ^
        --name "Kumara Enterprises POS System" ^
       --main-jar pos-1.0.0.jar ^
       --main-class com.devstack.pos.PosApplication ^
       --type exe ^
       --dest target\windows-installer ^
       --app-version 1.0.0 ^
       --vendor "DevStack" ^
       --description "Point of Sale System" ^
       --win-menu ^
       --win-shortcut ^
       --win-dir-chooser
   ```

3. **Find the executable:**
   - Location: `target\windows-installer\Kumara Enterprises POS System.exe`
   - This file can be distributed to end users

#### Advantages:
- Native Windows application
- Users don't need Java installed (bundled with executable)
- Can create Start Menu shortcuts
- Professional appearance

#### Disadvantages:
- Larger file size (includes Java runtime)
- Requires WiX Toolset for MSI installer

---

### Option 3: Windows Installer (MSI)

Creates a professional Windows installer package.

#### Steps:

1. **Build the JAR file:**
   ```batch
   mvn clean package
   ```

2. **Create MSI installer:**
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
       --win-upgrade-uuid "YOUR-UUID-HERE"
   ```

3. **Distribute the MSI:**
   - Location: `target\windows-installer\Kumara Enterprises POS System-1.0.0.msi`
   - Users can double-click to install

---

## Database Configuration

Before running the application, ensure PostgreSQL is configured:

1. **Install and start PostgreSQL**

2. **Create the database:**
   ```sql
   CREATE DATABASE robotikka;
   ```

3. **Update connection settings** (if needed):
   - Edit: `src\resources\application.properties`
   - Update:
     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5432/robotikka
     spring.datasource.username=postgres
     spring.datasource.password=YOUR_PASSWORD
     ```

4. **Rebuild the application** after changing properties:
   ```batch
   mvn clean package
   ```

---

## Distribution Checklist

When distributing the application to end users:

- [ ] Build the application (`mvn clean package`)
- [ ] Test the JAR/executable on a clean Windows machine
- [ ] Ensure PostgreSQL is accessible (or provide installation instructions)
- [ ] Update database connection settings for production
- [ ] Create user documentation
- [ ] Package the executable/installer
- [ ] Include database setup instructions
- [ ] Provide support contact information

---

## Troubleshooting

### "Java is not recognized"
- Install Java 21 or higher
- Add Java to system PATH
- Restart command prompt

### "Maven is not recognized"
- Install Maven
- Add Maven to system PATH
- Restart command prompt

### "Database connection failed"
- Ensure PostgreSQL is running
- Check database credentials in `application.properties`
- Verify database exists: `CREATE DATABASE robotikka;`

### "jpackage command not found"
- Ensure you're using JDK 14+ (not just JRE)
- jpackage is included in JDK, not JRE
- Verify: `jpackage --version`

### Application won't start
- Check Java version: `java -version` (must be 21+)
- Check if JAR file exists: `target\pos-1.0.0.jar`
- Check database connection
- Review error messages in console

---

## Production Deployment Recommendations

1. **Security:**
   - Change default database password
   - Use environment variables for sensitive data
   - Enable SSL for database connections

2. **Performance:**
   - Adjust JVM memory settings in batch file
   - Configure database connection pool
   - Enable database query caching

3. **Monitoring:**
   - Set up application logging
   - Monitor database performance
   - Track application errors

4. **Backup:**
   - Regular database backups
   - Application configuration backups
   - User data backups

---

## Support

For issues or questions:
- Check the main README.md
- Review application logs
- Contact the development team

