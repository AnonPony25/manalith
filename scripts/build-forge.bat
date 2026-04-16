@echo off
REM ============================================================
REM build-forge.bat — Clone and build Forge engine modules
REM Publishes forge-core, forge-game, forge-ai to local .m2
REM ============================================================

set FORGE_DIR=%~dp0..\..\forge

if not exist "%FORGE_DIR%" (
    echo Cloning Forge...
    git clone https://github.com/Card-Forge/forge.git "%FORGE_DIR%"
) else (
    echo Updating Forge...
    cd /d "%FORGE_DIR%"
    git pull
)

cd /d "%FORGE_DIR%"

echo Building Forge engine modules (this may take 5-10 minutes)...
call mvn clean install -pl forge-core,forge-game,forge-ai -am -DskipTests ^
     -Drevision=LOCAL-SNAPSHOT

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Forge build failed. Check output above.
    exit /b 1
)

echo.
echo [OK] Forge engine published to local .m2 repository.
echo      Version: LOCAL-SNAPSHOT
echo      Artifacts: forge-core, forge-game, forge-ai
