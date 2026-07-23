@echo off
chcp 65001 >nul

echo ===================================================
echo   PROJECT A - SYSTEM CHECK
echo ===================================================

REM ----------------------------------------------------
REM 1. JRE KONTROLÜ
REM ----------------------------------------------------
where java.exe >nul 2>nul
if not errorlevel 1 goto FOUND_JRE

if exist "tools\jre\bin\java.exe" goto FOUND_LOCAL_JRE

echo [WARNING] Java Runtime (JRE) not found!
echo [INFO] Downloading JRE...

if not exist "tools\temp" mkdir "tools\temp"

set "JRE_URL=https://javadl.oracle.com/webapps/download/AutoDL?BundleId=253456_ba687cb3cbb24342adc8fdf890b993dc"
set "JRE_INSTALLER=tools\temp\jre_installer.exe"

powershell -Command "Invoke-WebRequest -Uri '%JRE_URL%' -OutFile '%JRE_INSTALLER%'"

if not exist "%JRE_INSTALLER%" (
    echo [ERROR] Failed to download JRE! Check internet connection.
    pause
    exit /b 1
)

echo [INFO] Installing JRE...
start /wait "" "%JRE_INSTALLER%" /s
if exist "tools\temp" rmdir /s /q "tools\temp"
echo [SUCCESS] JRE installed successfully!

:FOUND_JRE
echo [OK] Java Runtime detected on system PATH!
goto CHECK_JDK

:FOUND_LOCAL_JRE
echo [OK] Local Java Runtime detected in tools\jre!
goto CHECK_JDK

REM ----------------------------------------------------
REM 2. JDK KONTROLÜ
REM ----------------------------------------------------
:CHECK_JDK
where javac.exe >nul 2>nul
if not errorlevel 1 (
    echo [OK] Java Development Kit (JDK) detected!
    goto RUN_GAME
)

echo [WARNING] JDK (javac) not found on system!
echo [INFO] Downloading Oracle JDK 26...

if not exist "tools\temp" mkdir "tools\temp"

set "JDK_URL=https://download.oracle.com/java/26/latest/jdk-26_windows-x64_bin.exe"
set "JDK_INSTALLER=tools\temp\jdk_installer.exe"

powershell -Command "Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%JDK_INSTALLER%'"

if not exist "%JDK_INSTALLER%" (
    echo [ERROR] Failed to download JDK!
    goto RUN_GAME
)

echo [INFO] Launching JDK 26 Installer...
start /wait "" "%JDK_INSTALLER%"
if exist "tools\temp" rmdir /s /q "tools\temp"
echo [SUCCESS] JDK Installation complete!

REM ----------------------------------------------------
REM 3. OYUNU BAŞLATMA (ProjectA.jar)
REM ----------------------------------------------------
:RUN_GAME
echo.
echo ===================================================
echo   LAUNCHING PROJECT A
echo ===================================================

if exist "tools\jre\bin\java.exe" (
    "tools\jre\bin\java.exe" -jar ProjectA.jar
) else (
    java -jar ProjectA.jar
)

pause
