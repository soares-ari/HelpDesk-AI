@echo off
echo Starting Helpdesk AI Backend with .env configuration...
echo.

REM Load environment variables from .env file
for /f "usebackq tokens=1,2 delims==" %%a in ("c:\Users\PICHAU\Documents\Dev\helpdesk-ai\backend\.env") do (
    set "line=%%a"
    REM Skip comments and empty lines
    if not "!line:~0,1!"=="#" if not "%%a"=="" (
        set "%%a=%%b"
        echo Loaded: %%a
    )
)

echo.
echo IMPORTANTE: Verificando OPENAI_API_KEY...
if "%OPENAI_API_KEY%"=="" (
    echo ERRO: OPENAI_API_KEY nao foi carregada do arquivo .env
    echo Verifique se o arquivo .env existe e contem a chave
    pause
    exit /b 1
) else (
    echo OK: OPENAI_API_KEY configurada (iniciando com: %OPENAI_API_KEY:~0,20%...)
)

echo.
echo Iniciando Spring Boot...
mvn -t ../.mvn/toolchains.xml spring-boot:run
