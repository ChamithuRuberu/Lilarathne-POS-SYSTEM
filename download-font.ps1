# PowerShell script to download Noto Sans Sinhala font
Write-Host "Downloading Noto Sans Sinhala font..." -ForegroundColor Cyan

$fontUrl = "https://github.com/google/fonts/raw/main/ofl/notosanssinhala/NotoSansSinhala-Regular.ttf"
$fontPath = "src/resources/fonts/NotoSansSinhala-Regular.ttf"
$fontDir = "src/resources/fonts"

if (-not (Test-Path $fontDir)) {
    New-Item -ItemType Directory -Path $fontDir -Force | Out-Null
    Write-Host "Created directory: $fontDir" -ForegroundColor Green
}

try {
    Write-Host "Downloading from: $fontUrl" -ForegroundColor Yellow
    Invoke-WebRequest -Uri $fontUrl -OutFile $fontPath -UseBasicParsing
    
    if (Test-Path $fontPath) {
        $fileSize = (Get-Item $fontPath).Length / 1KB
        Write-Host "Successfully downloaded font!" -ForegroundColor Green
        Write-Host "  File: $fontPath" -ForegroundColor Green
        Write-Host "  Size: $([math]::Round($fileSize, 2)) KB" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "  1. Rebuild the project: mvn clean package" -ForegroundColor Yellow
        Write-Host "  2. Restart your application" -ForegroundColor Yellow
    } else {
        Write-Host "Download failed - file not found" -ForegroundColor Red
    }
} catch {
    Write-Host "Download failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Manual download:" -ForegroundColor Yellow
    Write-Host "  1. Visit: https://fonts.google.com/noto/specimen/Noto+Sans+Sinhala" -ForegroundColor Yellow
    Write-Host "  2. Click Download family" -ForegroundColor Yellow
    Write-Host "  3. Extract ZIP and copy NotoSansSinhala-Regular.ttf to src/resources/fonts/" -ForegroundColor Yellow
}
