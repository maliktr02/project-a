@echo off
chcp 65001 >nul
title Verification System
color 0f
cls

echo.
echo  ==================================================
echo                VERIFICATION SYSTEM
echo  ==================================================
echo.

<nul set /p "= [1/3] System components are being checked       ."
for /l %%i in (1,1,4) do (
    <nul set /p "=."
    timeout /t 1 >nul
)
echo  [COMPLETED]

<nul set /p "= [2/3] Verifying files                           ."
for /l %%i in (1,1,4) do (
    <nul set /p "=."
    timeout /t 1 >nul
)
echo  [COMPLETED]

<nul set /p "= [3/3] Establishing server connection            ."
for /l %%i in (1,1,4) do (
    <nul set /p "=."
    timeout /t 1 >nul
)
echo  [UNSUCCESSFUL]

echo.
echo --------------------------------------------------
echo.

color 0c
echo  [ERROR CODE: 0x80070005]
echo  Access denied! Please try again after 
echo  verifying the integrity of the files...
echo.
echo --------------------------------------------------
echo.

pause >nul