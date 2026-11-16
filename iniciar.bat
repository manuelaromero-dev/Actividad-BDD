@echo off
cd /d "%~dp0"
java Servidor.java
if errorlevel 1 (
    echo.
    echo No se pudo iniciar el servidor. Hace falta Java 11 o superior: https://adoptium.net
)
pause
