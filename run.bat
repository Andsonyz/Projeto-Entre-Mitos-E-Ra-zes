@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "OUTPUT_DIR=%PROJECT_DIR%out"
set "SOURCE_LIST=%OUTPUT_DIR%\sources.txt"
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

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if exist "%SOURCE_LIST%" del "%SOURCE_LIST%"
for /r "%PROJECT_DIR%src\main\java" %%F in (*.java) do echo "%%F">>"%SOURCE_LIST%"

javac -encoding UTF-8 --module-path "%PROJECT_DIR%lib\javafx" --add-modules "%JAVAFX_MODULES%" -d "%OUTPUT_DIR%" @"%SOURCE_LIST%"
set "BUILD_RESULT=%ERRORLEVEL%"
del "%SOURCE_LIST%"
if not "%BUILD_RESULT%"=="0" exit /b %BUILD_RESULT%

if exist "%PROJECT_DIR%Image" xcopy "%PROJECT_DIR%Image" "%OUTPUT_DIR%\Image" /E /I /Y >nul
if exist "%PROJECT_DIR%src\main\resources" xcopy "%PROJECT_DIR%src\main\resources\*" "%OUTPUT_DIR%\" /E /I /Y >nul
if exist "%PROJECT_DIR%Videos" xcopy "%PROJECT_DIR%Videos" "%OUTPUT_DIR%\Videos" /E /I /Y >nul

cd /d "%PROJECT_DIR%"
java -cp "%OUTPUT_DIR%" --module-path "%PROJECT_DIR%lib\javafx" --add-modules "%JAVAFX_MODULES%" --enable-native-access=javafx.graphics,javafx.media br.com.raizes.Main
