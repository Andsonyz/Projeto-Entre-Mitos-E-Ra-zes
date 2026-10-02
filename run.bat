@echo off
setlocal
set "PROJECT_DIR=%~dp0"
set "OUTPUT_DIR=%PROJECT_DIR%out"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
javac -d "%OUTPUT_DIR%" "%PROJECT_DIR%src\main\java\br\com\raizes\EntreMitosERaizes.java"
if errorlevel 1 exit /b %errorlevel%

java -cp "%OUTPUT_DIR%" br.com.raizes.EntreMitosERaizes
