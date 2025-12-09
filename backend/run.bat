@echo off
echo Starting Helpdesk AI Backend...
echo.
echo IMPORTANTE: Configure a variavel OPENAI_API_KEY antes de executar!
echo Exemplo: set OPENAI_API_KEY=sk-sua-chave-aqui
echo.
if "%OPENAI_API_KEY%"=="" (
    echo ERRO: OPENAI_API_KEY nao esta configurada!
    echo Configure com: set OPENAI_API_KEY=sk-sua-chave-aqui
    pause
    exit /b 1
)
mvn -t ../.mvn/toolchains.xml spring-boot:run
