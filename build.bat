@echo off
setlocal
set "JDK_PATH=C:\Program Files\Java\jdk-25.0.2\bin"
if exist "%JDK_PATH%\javac.exe" (
    set "PATH=%JDK_PATH%;%PATH%"
)

echo [1/4] Compiling Encryptor tool...
javac tools\Encryptor\encryptor_source\Encryptor.java -d tools\Encryptor\encryptor_source\
if %errorlevel% neq 0 (
    echo Encryptor compilation failed!
    exit /b 1
)

jar cfe tools\Encryptor\Encryptor.jar Encryptor -C tools\Encryptor\encryptor_source\ .
echo Encryptor.jar created successfully.

echo [2/4] Generating production data.bin...
pushd tools\Encryptor
java -jar Encryptor.jar
popd

echo [3/4] Compiling Project A Game source code...
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\com\projecta\*.java
if %errorlevel% neq 0 (
    echo Game compilation failed!
    exit /b 1
)

echo [4/4] Packaging ProjectA.jar executable...
jar cfe ProjectA.jar com.projecta.Main -C out .
echo ProjectA.jar created successfully!

echo BUILD COMPLETE!
