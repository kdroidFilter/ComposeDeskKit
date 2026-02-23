@echo off
setlocal

rem Compiles NucleusWinBridge.c into a 64-bit DLL.
rem Prerequisites: Visual Studio 2022 (Community or Build Tools).
rem Usage: build.bat

rem --- Locate vcvarsall.bat and initialize MSVC environment ---
set "VCVARSALL="
for /f "tokens=*" %%i in ('where vcvarsall.bat 2^>nul') do set "VCVARSALL=%%i"
if "%VCVARSALL%"=="" (
    if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" (
        set "VCVARSALL=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat"
    )
)
if "%VCVARSALL%"=="" (
    if exist "C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvarsall.bat" (
        set "VCVARSALL=C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvarsall.bat"
    )
)
if "%VCVARSALL%"=="" (
    echo ERROR: Could not find vcvarsall.bat. Install Visual Studio 2022 or Build Tools.
    exit /b 1
)
call "%VCVARSALL%" x64 >nul 2>&1

rem --- Locate JAVA_HOME for JNI/JAWT headers and libraries ---
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set.
    exit /b 1
)

set JNI_INCLUDE=%JAVA_HOME%\include
set JNI_INCLUDE_WIN32=%JAVA_HOME%\include\win32
set JNI_LIB=%JAVA_HOME%\lib

if not exist "%JNI_INCLUDE%\jni.h" (
    echo ERROR: JNI headers not found at %JNI_INCLUDE%
    exit /b 1
)

if not exist "%JNI_LIB%\jawt.lib" (
    echo ERROR: jawt.lib not found at %JNI_LIB%
    exit /b 1
)

set SCRIPT_DIR=%~dp0
set RESOURCE_DIR=%SCRIPT_DIR%..\..\resources\nucleus\native
set OUT_DIR=%RESOURCE_DIR%\win32-x64

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

cl /nologo /LD /O2 ^
   /I"%JNI_INCLUDE%" /I"%JNI_INCLUDE_WIN32%" ^
   "%SCRIPT_DIR%NucleusWinBridge.c" ^
   user32.lib dwmapi.lib "%JNI_LIB%\jawt.lib" ^
   /Fe:"%OUT_DIR%\nucleus_windows.dll" ^
   /link /OPT:REF /OPT:ICF

if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

rem Clean up intermediate files
del /q "%SCRIPT_DIR%NucleusWinBridge.obj" 2>nul
del /q "%SCRIPT_DIR%NucleusWinBridge.exp" 2>nul
del /q "%SCRIPT_DIR%NucleusWinBridge.lib" 2>nul

echo Built: %OUT_DIR%\nucleus_windows.dll
dir "%OUT_DIR%\nucleus_windows.dll"
