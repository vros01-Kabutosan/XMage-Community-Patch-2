@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title XMage RC1 1.4.61 - reconstruir pareja cliente-servidor

rem ============================================================================
rem XMage RC1 1.4.61 - RECONSTRUCCION DE PAREJA CLIENTE/SERVIDOR v2
rem
rem Debe estar en la raiz del clon oficial, junto a installed.properties,
rem XMageLauncher-0.3.8.jar, client, server y java.
rem
rem No toca J:\MTG\xmage. Conserva config e installed.properties, crea backup,
rem deja log completo y mantiene -Dsun.java2d.uiScale=1.5.
rem ============================================================================

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "RUN=%RANDOM%%RANDOM%"
set "LOGDIR=%ROOT%\logs\pair-rebuild"
set "LOG=%LOGDIR%\pair-rebuild-%RUN%.log"
set "BACKUP=%ROOT%\backups\pair-fix-%RUN%"
set "OUTPUT=J:\MTG\XMage-RC1-PAIR-1.4.61-%RUN%"
set "STAGE=%OUTPUT%\staged"
set "MAVEN_REPO=J:\MTG\XMage-RC1-M2-REPOSITORY"
set "MAVEN_TEMP=J:\MTG\XMage-RC1-TEMP"
set "MAVEN_HOME=J:\MTG\XMage-RC1-MAVEN-LOCAL"
set "MVN="
set "SOURCE="
set "JAVA=%ROOT%\java\jre1.8.0_201\bin\java.exe"
set "LAUNCHER=%ROOT%\XMageLauncher-0.3.8.jar"
set "UIFLAG=-Dsun.java2d.uiScale=1.5"
set "JAVA_TOOL_OPTIONS=%UIFLAG%"
set "SERVER_LOG=%LOGDIR%\server-live-%RUN%.log"
set "LAUNCHER_LOG=%LOGDIR%\launcher-live-%RUN%.log"

set "XMAGE_ROOT=%ROOT%"
set "XMAGE_LOG=%LOG%"
set "XMAGE_SOURCE="
set "XMAGE_MVN="
set "XMAGE_REPO=%MAVEN_REPO%"
set "XMAGE_STAGE=%STAGE%"
set "XMAGE_OUTPUT=%OUTPUT%"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
if not exist "%LOGDIR%" (
  echo ERROR: no se pudo crear la carpeta de logs:
  echo %LOGDIR%
  pause
  exit /b 1
)

(
  echo ================================================================================
  echo XMAGE RC1 1.4.61 - RECONSTRUIR PAREJA CLIENTE/SERVIDOR v2
  echo Fecha: %DATE% %TIME%
  echo ROOT: %ROOT%
  echo OUTPUT: %OUTPUT%
  echo BACKUP: %BACKUP%
  echo LOG: %LOG%
  echo UI_LOCK: %UIFLAG%
  echo MOD_POLICY: source port-1.4.61V1; stable RC1 mods only
  echo BUILD_POLICY: Maven -T 1C, -Xmx8g, cache/temp en J:\MTG
  echo SAFETY: no se toca J:\MTG\xmage
  echo ================================================================================
) > "%LOG%" 2>&1

call :say "Inicio visible. Esta ventana no se cierra durante la reconstruccion."
call :say "Log en tiempo real: %LOG%"
call :say "La interfaz queda blindada con %UIFLAG%."

if not exist "%ROOT%\installed.properties" goto :fail_root
if not exist "%ROOT%\client\lib" goto :fail_client
if not exist "%ROOT%\server\lib" goto :fail_server
if not exist "%JAVA%" goto :fail_java
if not exist "%LAUNCHER%" goto :fail_launcher

call :say "[1/10] Localizando la fuente exacta 1.4.61V1..."
set "CANDIDATE1=%ROOT%\..\..\migration-workspace\port-1.4.61V1\source"
set "CANDIDATE2=J:\xmage repositorio\XMage-Community-Patch-hardening-update-architecture\tools\migration\migration-workspace\port-1.4.61V1\source"
set "CANDIDATE3=J:\Proyecto XMAGE"
for %%S in ("%CANDIDATE1%" "%CANDIDATE2%" "%CANDIDATE3%") do (
  if not defined SOURCE if exist "%%~S\pom.xml" if exist "%%~S\Mage.Client\pom.xml" if exist "%%~S\Mage.Server\pom.xml" set "SOURCE=%%~S"
)
if not defined SOURCE goto :fail_source
set "XMAGE_SOURCE=%SOURCE%"
call :say "SOURCE=%SOURCE%"

"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $p=Join-Path $env:XMAGE_SOURCE 'pom.xml'; $x=[xml](Get-Content -LiteralPath $p -Raw); if([string]$x.project.version -ne '1.4.61'){ throw ('Version de root no valida: '+[string]$x.project.version) }; foreach($rel in @('Mage.Client\pom.xml','Mage.Server\pom.xml')){ if(-not(Test-Path -LiteralPath (Join-Path $env:XMAGE_SOURCE $rel))){ throw ('Falta '+$rel) } }; Write-Host 'SOURCE_VERSION=1.4.61'; Add-Content -LiteralPath $env:XMAGE_LOG -Value 'SOURCE_VERSION=1.4.61' -Encoding UTF8" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_source_version

call :say "[2/10] Seleccionando Maven y preparando cache temporal..."
if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN if exist "%SOURCE%\mvnw.cmd" set "MVN=%SOURCE%\mvnw.cmd"
if not defined MVN for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MVN set "MVN=%%M"
if not defined MVN goto :fail_maven
set "XMAGE_MVN=%MVN%"
if not exist "J:\MTG" mkdir "J:\MTG" >> "%LOG%" 2>&1
if not exist "%MAVEN_REPO%" mkdir "%MAVEN_REPO%" >> "%LOG%" 2>&1
if not exist "%MAVEN_TEMP%" mkdir "%MAVEN_TEMP%" >> "%LOG%" 2>&1
set "MAVEN_OPTS=-Xms2g -Xmx8g -XX:+UseG1GC -Djava.io.tmpdir=%MAVEN_TEMP%"
call :say "MAVEN=%MVN%"
call :say "MAVEN_REPO=%MAVEN_REPO%"
call :say "MAVEN_OPTS=%MAVEN_OPTS%"

call :say "[3/10] Cerrando solo procesos XMage de este clon y comprobando 17171..."
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; $root=(Resolve-Path -LiteralPath $env:XMAGE_ROOT).Path.TrimEnd('\'); $ps=@(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '(?i)^java(w)?\.exe$' -and ([string]$_.CommandLine) -match [regex]::Escape($root) -and ([string]$_.CommandLine) -match '(?i)(mage-server|mage-client|XMageLauncher)' }); foreach($p in $ps){ Write-Host ('STOP PID='+$p.ProcessId); Add-Content -LiteralPath $env:XMAGE_LOG -Value ('STOP PID='+$p.ProcessId+' CMD='+[string]$p.CommandLine) -Encoding UTF8; Stop-Process -Id ([int]$p.ProcessId) -Force -ErrorAction SilentlyContinue }; Start-Sleep -Milliseconds 500; $lines=@(& netstat.exe -ano 2>&1 | Select-String -Pattern ':17171\s'); foreach($l in $lines){ Write-Host ([string]$l.Line); Add-Content -LiteralPath $env:XMAGE_LOG -Value ('PORT='+[string]$l.Line) -Encoding UTF8 }; if($lines.Count -gt 0){ exit 23 }" >> "%LOG%" 2>&1
if errorlevel 23 goto :fail_port

call :say "[4/10] Compilando toda la pareja desde una sola ejecucion Maven..."
call :say "La compilacion puede tardar; Maven mostrara el progreso aqui y lo guardara en el log."
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; & $env:XMAGE_MVN '-T' '1C' ('-Dmaven.repo.local='+$env:XMAGE_REPO) '-DskipTests' '-Djacoco.skip=true' 'clean' 'install' 2>&1 | Tee-Object -File $env:XMAGE_LOG -Append; exit $LASTEXITCODE" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_build

call :say "[5/10] Preparando paquete temporal sin tocar la instalacion activa..."
if exist "%OUTPUT%" rmdir /s /q "%OUTPUT%" >> "%LOG%" 2>&1
mkdir "%OUTPUT%" >> "%LOG%" 2>&1
mkdir "%STAGE%\client" >> "%LOG%" 2>&1
mkdir "%STAGE%\server" >> "%LOG%" 2>&1
set "XMAGE_CLIENT_STAGE=%STAGE%\client"
set "XMAGE_SERVER_STAGE=%STAGE%\server"

call :say "[6/10] Intentando ensamblado oficial de cliente y servidor..."
set "CLIENT_ZIP=%SOURCE%\Mage.Client\target\mage-client.zip"
set "SERVER_ZIP=%SOURCE%\Mage.Server\target\mage-server.zip"
if exist "%CLIENT_ZIP%" del /q "%CLIENT_ZIP%" >> "%LOG%" 2>&1
if exist "%SERVER_ZIP%" del /q "%SERVER_ZIP%" >> "%LOG%" 2>&1

"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; & $env:XMAGE_MVN '-T' '1C' ('-Dmaven.repo.local='+$env:XMAGE_REPO) '-DskipTests' '-Djacoco.skip=true' '-f' (Join-Path $env:XMAGE_SOURCE 'Mage.Client\pom.xml') 'assembly:single' 2>&1 | Tee-Object -File $env:XMAGE_LOG -Append; exit $LASTEXITCODE" >> "%LOG%" 2>&1
if exist "%CLIENT_ZIP%" (call :say "CLIENT_ZIP=OK") else (call :say "CLIENT_ZIP=NO; se usara staging Maven de dependencias")

"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; & $env:XMAGE_MVN '-T' '1C' ('-Dmaven.repo.local='+$env:XMAGE_REPO) '-DskipTests' '-Djacoco.skip=true' '-f' (Join-Path $env:XMAGE_SOURCE 'Mage.Server\pom.xml') 'assembly:single' 2>&1 | Tee-Object -File $env:XMAGE_LOG -Append; exit $LASTEXITCODE" >> "%LOG%" 2>&1
if exist "%SERVER_ZIP%" (call :say "SERVER_ZIP=OK") else (call :say "SERVER_ZIP=NO; se usara staging Maven de dependencias")

call :say "[7/10] Extrayendo distribuciones o generando lib desde el reactor..."
if exist "%CLIENT_ZIP%" (
  "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath $env:CLIENT_ZIP -DestinationPath (Join-Path $env:XMAGE_STAGE 'client') -Force" >> "%LOG%" 2>&1
  if errorlevel 1 goto :fail_client_package
)
if exist "%SERVER_ZIP%" (
  "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath $env:SERVER_ZIP -DestinationPath (Join-Path $env:XMAGE_STAGE 'server') -Force" >> "%LOG%" 2>&1
  if errorlevel 1 goto :fail_server_package
)

set "CLIENT_JAR="
set "SERVER_JAR="
for /f "delims=" %%F in ('"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -Command "Get-ChildItem -LiteralPath (Join-Path $env:XMAGE_SOURCE 'Mage.Client\target') -Filter 'mage-client-1.4.61.jar' -File -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName"') do if not defined CLIENT_JAR set "CLIENT_JAR=%%F"
for /f "delims=" %%F in ('"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -Command "Get-ChildItem -LiteralPath (Join-Path $env:XMAGE_SOURCE 'Mage.Server\target') -Filter 'mage-server-1.4.61.jar' -File -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName"') do if not defined SERVER_JAR set "SERVER_JAR=%%F"
if not defined CLIENT_JAR goto :fail_client_jar
if not defined SERVER_JAR goto :fail_server_jar
call :say "CLIENT_BUILD_JAR=%CLIENT_JAR%"
call :say "SERVER_BUILD_JAR=%SERVER_JAR%"

if not exist "%STAGE%\client\lib" mkdir "%STAGE%\client\lib" >> "%LOG%" 2>&1
if not exist "%STAGE%\server\lib" mkdir "%STAGE%\server\lib" >> "%LOG%" 2>&1
copy /y "%CLIENT_JAR%" "%STAGE%\client\lib\mage-client-1.4.61.jar" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_client_jar
copy /y "%SERVER_JAR%" "%STAGE%\server\lib\mage-server-1.4.61.jar" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_server_jar

if not exist "%STAGE%\client\lib\mage-client-1.4.61.jar" goto :fail_client_jar
if not exist "%STAGE%\server\lib\mage-server-1.4.61.jar" goto :fail_server_jar

call :say "Copiando dependencias desde el mismo repositorio Maven (fallback seguro si falto ZIP)..."
set "CLIENT_DEP=%STAGE%\client\lib"
set "SERVER_DEP=%STAGE%\server\lib"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; & $env:XMAGE_MVN '-T' '1C' ('-Dmaven.repo.local='+$env:XMAGE_REPO) '-DskipTests' '-DincludeScope=runtime' '-DexcludeArtifactIds=mage-client' ('-DoutputDirectory='+$env:CLIENT_DEP) '-f' (Join-Path $env:XMAGE_SOURCE 'Mage.Client\pom.xml') 'dependency:copy-dependencies' 2>&1 | Tee-Object -File $env:XMAGE_LOG -Append; exit $LASTEXITCODE" >> "%LOG%" 2>&1
if errorlevel 1 if not exist "%CLIENT_ZIP%" goto :fail_client_dependencies

"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; & $env:XMAGE_MVN '-T' '1C' ('-Dmaven.repo.local='+$env:XMAGE_REPO) '-DskipTests' '-DincludeScope=runtime' '-DexcludeArtifactIds=mage-server' ('-DoutputDirectory='+$env:SERVER_DEP) '-f' (Join-Path $env:XMAGE_SOURCE 'Mage.Server\pom.xml') 'dependency:copy-dependencies' 2>&1 | Tee-Object -File $env:XMAGE_LOG -Append; exit $LASTEXITCODE" >> "%LOG%" 2>&1
if errorlevel 1 if not exist "%SERVER_ZIP%" goto :fail_server_dependencies

if not exist "%STAGE%\client\plugins" mkdir "%STAGE%\client\plugins" >> "%LOG%" 2>&1
if not exist "%STAGE%\server\plugins" mkdir "%STAGE%\server\plugins" >> "%LOG%" 2>&1
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p=Join-Path $env:XMAGE_CLIENT_STAGE 'lib'; $d=Join-Path $env:XMAGE_CLIENT_STAGE 'plugins'; Get-ChildItem -LiteralPath $p -Filter 'mage-counter-plugin-*.jar' -File -ErrorAction SilentlyContinue | Move-Item -Destination $d -Force" >> "%LOG%" 2>&1
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p=Join-Path $env:XMAGE_SERVER_STAGE 'lib'; $d=Join-Path $env:XMAGE_SERVER_STAGE 'plugins'; $keep='^(mage-1\.4\.61|mage-common-1\.4\.61|mage-sets-1\.4\.61|mage-server-1\.4\.61)\.jar$'; Get-ChildItem -LiteralPath $p -Filter 'mage-*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch $keep } | Copy-Item -Destination $d -Force" >> "%LOG%" 2>&1

call :say "[8/10] Conservando recursos y configuracion del clon actual..."
robocopy "%ROOT%\client" "%STAGE%\client" /E /R:1 /W:1 /XD "%ROOT%\client\lib" "%ROOT%\client\plugins" >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_overlay
robocopy "%ROOT%\server" "%STAGE%\server" /E /R:1 /W:1 /XD "%ROOT%\server\lib" "%ROOT%\server\plugins" >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_overlay

call :say "[9/10] Validando JAR, manifiesto y hash antes de activar..."
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.IO.Compression.FileSystem; $pairs=@(@{role='client';path=(Join-Path $env:XMAGE_CLIENT_STAGE 'lib\mage-client-1.4.61.jar')},@{role='server';path=(Join-Path $env:XMAGE_SERVER_STAGE 'lib\mage-server-1.4.61.jar')}); $out=Join-Path $env:XMAGE_OUTPUT 'pair-validation.txt'; foreach($p in $pairs){ if(-not(Test-Path -LiteralPath $p.path)){ throw ('Falta '+$p.path) }; $h=(Get-FileHash -LiteralPath $p.path -Algorithm SHA256).Hash; $z=[IO.Compression.ZipFile]::OpenRead($p.path); $e=$z.GetEntry('META-INF/MANIFEST.MF'); $m=''; if($e){$sr=New-Object IO.StreamReader($e.Open());$m=$sr.ReadToEnd();$sr.Dispose()};$z.Dispose(); $lines=@($m -split '\r?\n' | Where-Object { $_ -match '^(Implementation-Version|Implementation-Title|Build-Time|Build-Date|Main-Class):' }); $row=('{0} | {1} bytes | SHA256 {2}' -f $p.role,(Get-Item -LiteralPath $p.path).Length,$h); Write-Host $row; Add-Content -LiteralPath $out -Value $row -Encoding UTF8; foreach($l in $lines){Write-Host ($p.role+' | '+$l);Add-Content -LiteralPath $out -Value ($p.role+' | '+$l) -Encoding UTF8} }" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_validation

if not exist "%BACKUP%" mkdir "%BACKUP%" >> "%LOG%" 2>&1
copy /y "%ROOT%\installed.properties" "%BACKUP%\installed.properties.before" >> "%LOG%" 2>&1
if errorlevel 1 goto :fail_backup
robocopy "%ROOT%\client\lib" "%BACKUP%\client-lib" /E /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_backup
if exist "%ROOT%\client\plugins" robocopy "%ROOT%\client\plugins" "%BACKUP%\client-plugins" /E /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_backup
robocopy "%ROOT%\server\lib" "%BACKUP%\server-lib" /E /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_backup
if exist "%ROOT%\server\plugins" robocopy "%ROOT%\server\plugins" "%BACKUP%\server-plugins" /E /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :fail_backup

call :say "Activando solo las librerias; client\\config y server\\config quedan intactas."
robocopy "%STAGE%\client\lib" "%ROOT%\client\lib" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :rollback
robocopy "%STAGE%\client\plugins" "%ROOT%\client\plugins" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :rollback
robocopy "%STAGE%\server\lib" "%ROOT%\server\lib" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :rollback
robocopy "%STAGE%\server\plugins" "%ROOT%\server\plugins" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if errorlevel 8 goto :rollback

call :say "Reparando/verificando enlaces oficiales xmage\\mage-client y xmage\\mage-server..."
set "LINK_CLIENT_OK="
set "LINK_SERVER_OK="
for /f "delims=" %%R in ('"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -Command "$root=(Resolve-Path -LiteralPath $env:XMAGE_ROOT).Path; $i=Get-Item -LiteralPath (Join-Path $root 'xmage\mage-client') -Force -ErrorAction SilentlyContinue; if($i -and $i.LinkType -eq 'Junction' -and (Resolve-Path -LiteralPath $i.Target -ErrorAction SilentlyContinue).Path -eq (Join-Path $root 'client')){'OK'} else {'BAD'}"') do set "LINK_CLIENT_OK=%%R"
for /f "delims=" %%R in ('"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -Command "$root=(Resolve-Path -LiteralPath $env:XMAGE_ROOT).Path; $i=Get-Item -LiteralPath (Join-Path $root 'xmage\mage-server') -Force -ErrorAction SilentlyContinue; if($i -and $i.LinkType -eq 'Junction' -and (Resolve-Path -LiteralPath $i.Target -ErrorAction SilentlyContinue).Path -eq (Join-Path $root 'server')){'OK'} else {'BAD'}"') do set "LINK_SERVER_OK=%%R"
if /i not "%LINK_CLIENT_OK%"=="OK" call :repair_link "client"
if /i not "%LINK_SERVER_OK%"=="OK" call :repair_link "server"
if not exist "%ROOT%\xmage\mage-client\lib\mage-client-1.4.61.jar" goto :rollback
if not exist "%ROOT%\xmage\mage-server\lib\mage-server-1.4.61.jar" goto :rollback

call :say "Reforzando en installed.properties el flag sagrado de interfaz..."
set "XMAGE_PROP=%ROOT%\installed.properties"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $p=$env:XMAGE_PROP; $lines=[IO.File]::ReadAllLines($p); $found=$false; for($i=0;$i -lt $lines.Count;$i++){ if($lines[$i] -match '^xmage\.client\.javaopts='){ $found=$true; $lines[$i]=[regex]::Replace($lines[$i],'-Dsun\.java2d\.uiScale\\?=[^\s]+','').TrimEnd()+' -Dsun.java2d.uiScale\=1.5' } }; if(-not $found){throw 'No existe xmage.client.javaopts'}; [IO.File]::WriteAllLines($p,$lines,(New-Object Text.UTF8Encoding($false))); $ok=(Get-Content -LiteralPath $p | Where-Object {$_ -match '^xmage\.client\.javaopts=' -and $_ -match 'Dsun\.java2d\.uiScale\\?=1\.5'}); if(-not $ok){throw 'No se pudo verificar uiScale'}; Write-Host $ok; Add-Content -LiteralPath $env:XMAGE_LOG -Value ('UI_LOCK_FINAL='+$ok) -Encoding UTF8" >> "%LOG%" 2>&1
if errorlevel 1 goto :rollback

call :say "[10/10] Probando servidor nuevo y abriendo el lanzador oficial..."
set "SERVER_OPTS=-Xms512m -Xmx4g -Djava.net.preferIPv4Stack=true"
start "XMage Server RC1 1.4.61" /D "%ROOT%\server" /B "%ComSpec%" /d /c ""%JAVA%" %SERVER_OPTS% -cp "%ROOT%\server\lib\*" mage.server.Main >> "%SERVER_LOG%" 2>&1"
if errorlevel 1 goto :rollback

set /a WAIT=0
:wait_server
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',17171);$c.Close();exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 goto :server_ready
set /a WAIT+=5
call :say "Servidor cargando: !WAIT! / 180 segundos"
if !WAIT! GEQ 180 goto :fail_server_start
timeout /t 5 /nobreak >nul
goto :wait_server

:server_ready
call :say "SERVIDOR NUEVO LISTO en 127.0.0.1:17171"
call :say "Abriendo XMageLauncher-0.3.8.jar con Java 8 y uiScale blindado..."
start "XMage Launcher 0.3.8" /D "%ROOT%" /B "%ComSpec%" /d /c ""%JAVA%" -jar "%LAUNCHER%" >> "%LAUNCHER_LOG%" 2>&1"
if errorlevel 1 goto :fail_launcher_start

(
  echo.
  echo [SUCCESS] PAREJA CLIENTE/SERVIDOR RECONSTRUIDA Y ACTIVADA.
  echo [SUCCESS] Ambos JAR provienen de la misma compilacion Maven 1.4.61.
  echo [SUCCESS] server\config, client\config e installed.properties conservados.
  echo [SUCCESS] uiScale blindado: -Dsun.java2d.uiScale=1.5
  echo [BACKUP] %BACKUP%
  echo [PACKAGE] %OUTPUT%
  echo [LOG] %LOG%
  echo [SERVER LOG] %SERVER_LOG%
  echo [LAUNCHER LOG] %LAUNCHER_LOG%
) >> "%LOG%"
call :say "REPARACION COMPLETADA. El lanzador oficial ya esta abierto."
call :say "Pulsa solo Launch Client: el servidor ya esta levantado con la misma pareja."
call :say "Backup: %BACKUP%"
call :say "Log: %LOG%"
echo.
pause
exit /b 0

:repair_link
set "KIND=%~1"
set "TARGET=%ROOT%\%KIND%"
set "ALIAS=%ROOT%\xmage\mage-%KIND%"
if not exist "%ROOT%\xmage" mkdir "%ROOT%\xmage" >> "%LOG%" 2>&1
if exist "%ALIAS%" (
  if not exist "%BACKUP%\link-%KIND%" move "%ALIAS%" "%BACKUP%\link-%KIND%" >> "%LOG%" 2>&1
)
mklink /J "%ALIAS%" "%TARGET%" >> "%LOG%" 2>&1
if not exist "%ALIAS%\lib" (
  call :say "Junction %ALIAS% no se pudo crear; se hace copia compatible recuperable."
  robocopy "%TARGET%" "%ALIAS%" /E /R:1 /W:1 >> "%LOG%" 2>&1
)
exit /b 0

:rollback
call :say "FALLO DURANTE ACTIVACION. Restaurando librerias desde el backup..."
if exist "%BACKUP%\client-lib" robocopy "%BACKUP%\client-lib" "%ROOT%\client\lib" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if exist "%BACKUP%\client-plugins" robocopy "%BACKUP%\client-plugins" "%ROOT%\client\plugins" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if exist "%BACKUP%\server-lib" robocopy "%BACKUP%\server-lib" "%ROOT%\server\lib" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if exist "%BACKUP%\server-plugins" robocopy "%BACKUP%\server-plugins" "%ROOT%\server\plugins" /MIR /R:1 /W:1 >> "%LOG%" 2>&1
if exist "%BACKUP%\installed.properties.before" copy /y "%BACKUP%\installed.properties.before" "%ROOT%\installed.properties" >> "%LOG%" 2>&1
call :say "Rollback terminado. La instalacion anterior queda restaurada."
goto :fail_common

:fail_server_start
call :say "ERROR: el servidor nuevo no abrio 17171 en 180 segundos."
if exist "%SERVER_LOG%" powershell.exe -NoLogo -NoProfile -Command "Get-Content -LiteralPath $env:SERVER_LOG -Tail 80" >> "%LOG%" 2>&1
goto :rollback

:fail_root
call :say "ERROR: este CMD debe estar en la raiz del clon; falta installed.properties."
goto :fail_common
:fail_client
call :say "ERROR: falta client\lib en la instalacion activa."
goto :fail_common
:fail_server
call :say "ERROR: falta server\lib en la instalacion activa."
goto :fail_common
:fail_java
call :say "ERROR: falta Java 8u201 en java\jre1.8.0_201\bin\java.exe."
goto :fail_common
:fail_launcher
call :say "ERROR: falta XMageLauncher-0.3.8.jar."
goto :fail_common
:fail_source
call :say "ERROR: no se encontro la fuente 1.4.61V1 en ninguna ruta prevista."
call :say "Revisadas: %CANDIDATE1% / %CANDIDATE2% / %CANDIDATE3%"
goto :fail_common
:fail_source_version
call :say "ERROR: la fuente encontrada no declara exactamente version root 1.4.61. No se toca la instalacion."
goto :fail_common
:fail_maven
call :say "ERROR: no se encontro Maven. No se ha modificado la instalacion."
goto :fail_common
:fail_port
call :say "ERROR: 17171 sigue ocupado por un proceso ajeno a este clon. No se toca nada."
goto :fail_common
:fail_build
call :say "ERROR: Maven fallo. La instalacion activa no se ha tocado."
goto :fail_common
:fail_client_package
call :say "ERROR: no se pudo extraer el paquete de cliente."
goto :fail_common
:fail_server_package
call :say "ERROR: no se pudo extraer el paquete de servidor."
goto :fail_common
:fail_client_jar
call :say "ERROR: no se genero mage-client-1.4.61.jar. Instalacion intacta."
goto :fail_common
:fail_server_jar
call :say "ERROR: no se genero mage-server-1.4.61.jar. Instalacion intacta."
goto :fail_common
:fail_client_dependencies
call :say "ERROR: no se pudieron reunir las dependencias del cliente."
goto :fail_common
:fail_server_dependencies
call :say "ERROR: no se pudieron reunir las dependencias del servidor."
goto :fail_common
:fail_overlay
call :say "ERROR: no se pudieron conservar los recursos/configuracion actuales."
goto :fail_common
:fail_validation
call :say "ERROR: la validacion previa de la pareja no paso. No se activo nada."
goto :fail_common
:fail_backup
call :say "ERROR: no se pudo completar el backup. No se ha activado nada."
goto :fail_common
:fail_launcher_start
call :say "ERROR: el servidor esta listo pero Windows no pudo abrir el lanzador."
goto :fail_common

:fail_common
(
  echo.
  echo [FAILED] La reparacion no se completo.
  echo [LOG] %LOG%
  echo [BACKUP] %BACKUP%
  echo [OUTPUT] %OUTPUT%
) >> "%LOG%"
echo.
echo LA REPARACION NO SE COMPLETO.
echo La instalacion anterior se ha conservado/restaurado siempre que ya existiera backup.
echo Log: %LOG%
echo Backup: %BACKUP%
echo.
pause
exit /b 1

:say
echo %~1
>>"%LOG%" echo %~1
exit /b 0

