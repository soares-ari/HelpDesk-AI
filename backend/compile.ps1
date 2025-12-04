# Script para compilar o projeto com Java 25
# Executa: .\compile.ps1

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Helpdesk AI - Compilação" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Configurar JAVA_HOME temporariamente
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "JAVA_HOME configurado para: $env:JAVA_HOME" -ForegroundColor Green
Write-Host ""

# Verificar versão do Java
Write-Host "Versão do Java:" -ForegroundColor Yellow
& java -version
Write-Host ""

# Verificar versão do Maven
Write-Host "Versão do Maven:" -ForegroundColor Yellow
& mvn -version
Write-Host ""

# Compilar o projeto
Write-Host "Iniciando compilação..." -ForegroundColor Yellow
Write-Host ""

& mvn clean compile

Write-Host ""
if ($LASTEXITCODE -eq 0) {
    Write-Host "================================" -ForegroundColor Green
    Write-Host "COMPILAÇÃO BEM-SUCEDIDA!" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
} else {
    Write-Host "================================" -ForegroundColor Red
    Write-Host "ERRO NA COMPILAÇÃO" -ForegroundColor Red
    Write-Host "================================" -ForegroundColor Red
}
