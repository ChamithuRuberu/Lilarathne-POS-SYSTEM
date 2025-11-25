@echo off
echo Downloading Noto Sans Sinhala font...
echo.

REM Create directory if it doesn't exist
if not exist "src\resources\fonts" mkdir "src\resources\fonts"

REM Try PowerShell download
powershell -ExecutionPolicy Bypass -Command "& {Invoke-WebRequest -Uri 'https://github.com/google/fonts/raw/main/ofl/notosanssinhala/NotoSansSinhala-Regular.ttf' -OutFile 'src\resources\fonts\NotoSansSinhala-Regular.ttf' -UseBasicParsing}"

if exist "src\resources\fonts\NotoSansSinhala-Regular.ttf" (
    echo.
    echo ✓ Successfully downloaded font!
    echo   File: src\resources\fonts\NotoSansSinhala-Regular.ttf
    echo.
    echo Next steps:
    echo   1. Rebuild the project: mvn clean package
    echo   2. Restart your application
) else (
    echo.
    echo ✗ Download failed!
    echo.
    echo Manual download:
    echo   1. Visit: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala
    echo   2. Click 'Download family'
    echo   3. Extract ZIP and copy NotoSansSinhala-Regular.ttf to: src\resources\fonts\
)

pause

