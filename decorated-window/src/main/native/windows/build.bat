@echo off
REM Compiles NucleusWinBridge.c into per-architecture DLLs (x64 + ARM64).
REM The outputs are placed in the JAR resources so they ship with the library.
REM
REM Prerequisites: Visual Studio Build Tools (MSVC) with ARM64 support.
REM Usage: build.bat [x64|arm64]

setlocal enabledelayedexpansion

set "ARCH=%~1"
if "%ARCH%"=="" set "ARCH=x64"

set "SCRIPT_DIR=%~dp0"
set "SRC=%SCRIPT_DIR%NucleusWinBridge.c"
set "RESOURCE_DIR=%SCRIPT_DIR%..\..\resources\nucleus\native"

REM Check JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set. >&2
    exit /b 1
)
if not exist "%JAVA_HOME%\include\jni.h" (
    echo ERROR: JNI headers not found at %JAVA_HOME%\include >&2
    exit /b 1
)

set "JNI_INCLUDE=%JAVA_HOME%\include"
set "JNI_INCLUDE_WIN32=%JAVA_HOME%\include\win32"

REM Locate vcvarsall.bat
set "VCVARSALL="
for %%v in (2022 2019 2017) do (
    for %%e in (Enterprise Professional Community BuildTools) do (
        if exist "C:\Program Files\Microsoft Visual Studio\%%v\%%e\VC\Auxiliary\Build\vcvarsall.bat" (
            set "VCVARSALL=C:\Program Files\Microsoft Visual Studio\%%v\%%e\VC\Auxiliary\Build\vcvarsall.bat"
            goto :found_vc
        )
        if exist "C:\Program Files (x86)\Microsoft Visual Studio\%%v\%%e\VC\Auxiliary\Build\vcvarsall.bat" (
            set "VCVARSALL=C:\Program Files (x86)\Microsoft Visual Studio\%%v\%%e\VC\Auxiliary\Build\vcvarsall.bat"
            goto :found_vc
        )
    )
)
:found_vc
if "%VCVARSALL%"=="" (
    echo ERROR: Could not locate vcvarsall.bat. Install Visual Studio Build Tools. >&2
    exit /b 1
)

echo Using vcvarsall.bat: %VCVARSALL%

if "%ARCH%"=="x64" (
    set "OUT_DIR=%RESOURCE_DIR%\win32-x64"
) else if "%ARCH%"=="arm64" (
    set "OUT_DIR=%RESOURCE_DIR%\win32-aarch64"
) else (
    echo ERROR: Invalid architecture. Use x64 or arm64. >&2
    exit /b 1
)

REM Create output directory
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

REM Build selected architecture
if "%ARCH%"=="arm64" (
    set "VCVARS_ARG=x64_arm64"
) else (
    set "VCVARS_ARG=%ARCH%"
)
echo.
echo === Building %ARCH% DLL ===
call "%VCVARSALL%" %VCVARS_ARG%
if errorlevel 1 (
    echo ERROR: vcvarsall %VCVARS_ARG% failed >&2
    exit /b 1
)

cl /LD /O2 /nologo ^
    /I"%JNI_INCLUDE%" /I"%JNI_INCLUDE_WIN32%" ^
    "%SRC%" ^
    /Fe:"%OUT_DIR%\nucleus_windows.dll" ^
    /link user32.lib dwmapi.lib advapi32.lib kernel32.lib "%JAVA_HOME%\lib\jawt.lib"
if errorlevel 1 (
    echo ERROR: %ARCH% compilation failed >&2
    exit /b 1
)

REM Clean up intermediate files
del /q "%OUT_DIR%\*.obj" "%OUT_DIR%\*.lib" "%OUT_DIR%\*.exp" 2>nul

echo.
echo Built: %OUT_DIR%\nucleus_windows.dll
if exist "%OUT_DIR%\nucleus_windows.dll" dir "%OUT_DIR%\nucleus_windows.dll"

endlocal
