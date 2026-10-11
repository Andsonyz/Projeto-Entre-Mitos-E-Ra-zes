@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "OUTPUT_DIR=%PROJECT_DIR%out"
set "SOURCE_LIST=%OUTPUT_DIR%\sources.txt"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if exist "%SOURCE_LIST%" del "%SOURCE_LIST%"
for /r "%PROJECT_DIR%src\main\java" %%F in (*.java) do echo "%%F">>"%SOURCE_LIST%"

javac -encoding UTF-8 -cp "%PROJECT_DIR%lib\javafx\*" -d "%OUTPUT_DIR%" @"%SOURCE_LIST%"
set "BUILD_RESULT=%ERRORLEVEL%"
del "%SOURCE_LIST%"
if not "%BUILD_RESULT%"=="0" exit /b %BUILD_RESULT%

if exist "%PROJECT_DIR%Image" xcopy "%PROJECT_DIR%Image" "%OUTPUT_DIR%\Image" /E /I /Y >nul
if exist "%PROJECT_DIR%src\main\resources" xcopy "%PROJECT_DIR%src\main\resources\*" "%OUTPUT_DIR%\" /E /I /Y >nul
if exist "%PROJECT_DIR%Videos" xcopy "%PROJECT_DIR%Videos" "%OUTPUT_DIR%\Videos" /E /I /Y >nul

cd /d "%PROJECT_DIR%"
java -cp "%OUTPUT_DIR%;%PROJECT_DIR%lib\javafx\*" br.com.raizes.Main
