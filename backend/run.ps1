Write-Host "Starting Helpdesk AI Backend..." -ForegroundColor Cyan
Write-Host ""

# Load environment variables from .env file
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Yellow
    Get-Content $envFile | ForEach-Object {
        if ($_ -notmatch '^\s*#' -and $_ -match '^\s*(.+?)\s*=\s*(.+)\s*$') {
            $name = $matches[1]
            $value = $matches[2]
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  Loaded: $name" -ForegroundColor Green
        }
    }
} else {
    Write-Host "ERROR: .env file not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Checking OPENAI_API_KEY..." -ForegroundColor Yellow
$apiKey = [Environment]::GetEnvironmentVariable("OPENAI_API_KEY", "Process")
if ([string]::IsNullOrEmpty($apiKey)) {
    Write-Host "ERROR: OPENAI_API_KEY not loaded from .env file" -ForegroundColor Red
    Write-Host "Please check that .env file contains OPENAI_API_KEY" -ForegroundColor Red
    exit 1
} else {
    $keyPreview = $apiKey.Substring(0, [Math]::Min(20, $apiKey.Length))
    Write-Host "  OK: OPENAI_API_KEY configured (starting with: $keyPreview...)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Spring Boot..." -ForegroundColor Cyan
& mvn -t ../.mvn/toolchains.xml spring-boot:run "-Dmaven.test.skip=true"
