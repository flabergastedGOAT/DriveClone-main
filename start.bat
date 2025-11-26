@echo off
setlocal ENABLEEXTENSIONS

REM ------------------------------------------------------------
REM Unified helper for DriveClone
REM Usage:
REM   start           -> run server (default)
REM   start run       -> run server
REM   start setup     -> configure IDE + dependencies
REM ------------------------------------------------------------

if "%1"=="" goto run
if /I "%1"=="run" goto run
if /I "%1"=="setup" goto setup
goto help

:help
echo.
echo DriveClone helper script
echo ------------------------
echo   start         Run the backend (same as ^"start run^")
echo   start run     Compile dependencies and start the Java server
echo   start setup   Clean, compile, copy dependencies, and generate IDE files
echo.
exit /b 0

:setup
echo.
echo [1/3] Cleaning and compiling project...
call mvn clean compile -q
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo [2/3] Copying runtime dependencies...
call mvn dependency:copy-dependencies -DoutputDirectory=target/lib -q

echo [3/3] Generating Eclipse project files...
call mvn eclipse:eclipse -q

echo.
echo IDE setup complete!
echo - VS Code: install "Extension Pack for Java" and reload the window.
echo - IntelliJ: File ^> Open ^> select this folder (auto-import Maven).
echo - Eclipse: File ^> Import ^> Existing Projects into Workspace.
echo.
exit /b 0

:run
echo.
echo Starting DriveClone backend...
echo Ensure prerequisites are satisfied:
echo   1. Java 17
echo   2. Maven installed
echo   3. .env configured
echo.

echo Compiling sources...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo Copying dependencies...
call mvn dependency:copy-dependencies -DoutputDirectory=target/lib -q

echo Launching server at http://localhost:8080
echo Press Ctrl+C to stop.
echo.
java -cp "target/classes;target/lib/*" com.driveclone.DriveCloneApp
exit /b %ERRORLEVEL%
