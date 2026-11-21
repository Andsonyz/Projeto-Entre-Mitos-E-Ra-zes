@echo off
setlocal EnableDelayedExpansion
set "PROJECT_DIR=%~dp0"
set "OUTPUT_DIR=%PROJECT_DIR%out\runtime"
set "JAVAFX_MODULES=javafx.controls,javafx.media,javafx.swing"

where java >nul 2>nul
if errorlevel 1 (
    echo Java nao foi encontrado no PATH. Instale um JDK 25 ou superior.
    exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
    echo javac nao foi encontrado no PATH. Instale um JDK 25 ou superior.
    exit /b 1
)
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%V in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%V"
if not defined JAVA_MAJOR (
    echo Nao foi possivel identificar a versao do Java.
    exit /b 1
)
if %JAVA_MAJOR% LSS 25 (
    echo JavaFX 25 requer JDK 25 ou superior. Versao detectada: %JAVA_VERSION%.
    exit /b 1
)
if not exist "%PROJECT_DIR%lib\javafx\javafx-base-25.0.1-win.jar" (
    echo Bibliotecas JavaFX ausentes em "%PROJECT_DIR%lib\javafx".
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%scripts\build-project.ps1"
if errorlevel 1 exit /b 1

cd /d "%PROJECT_DIR%"
java -cp "%OUTPUT_DIR%" --module-path "%PROJECT_DIR%lib\javafx" --add-modules "%JAVAFX_MODULES%" --enable-native-access=javafx.graphics,javafx.media br.com.raizes.Main
