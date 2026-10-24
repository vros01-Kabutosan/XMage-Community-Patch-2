@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title XMage RC1 1.4.61 - servidor listo antes del cliente

set "ROOT=%~dp0"
set "LOGDIR=%ROOT%logs"
if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
set "LOG=%LOGDIR%\sequential-launch-%RANDOM%.log"
set "SERVER_OUT=%LOGDIR%\server-live-%RANDOM%.log"
set "SERVER_ERR=%LOGDIR%\server-live-errors-%RANDOM%.log"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"
set "LAUNCHER=%ROOT%XMageLauncher-0.3.8.jar"
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "XMAGE_ROOT=%ROOT%"
set "XMAGE_LOG=%LOG%"
set "XMAGE_SERVER_OUT=%SERVER_OUT%"
set "XMAGE_SERVER_ERR=%SERVER_ERR%"
set "XMAGE_JAVA=%JAVA%"
set "XMAGE_LAUNCHER=%LAUNCHER%"
set "JAVA_TOOL_OPTIONS=-Dsun.java2d.uiScale=1.5"

(
  echo ================================================================
  echo XMAGE RC1 1.4.61 - SERVER READY THEN OFFICIAL LAUNCHER
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo Java: %JAVA%
  echo Mandatory UI flag: -Dsun.java2d.uiScale=1.5
  echo ================================================================
) > "%LOG%"

call :say "Comprobando Java 8, lanzador y configuracion..."
if not exist "%JAVA%" (
  call :fail "No existe Java 8u201 en java\jre1.8.0_201\bin\java.exe"
  exit /b 1
)
if not exist "%LAUNCHER%" (
  call :fail "No existe XMageLauncher-0.3.8.jar"
  exit /b 1
)
if not exist "%ROOT%installed.properties" (
  call :fail "No existe installed.properties"
  exit /b 1
)
if not exist "%ROOT%xmage\mage-server\lib" (
  call :fail "No existe xmage\mage-server\lib"
  exit /b 1
)

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue'; $root=$env:XMAGE_ROOT; $log=$env:XMAGE_LOG; $java=$env:XMAGE_JAVA;" ^
  "function Emit([string]$s){ Write-Host $s; Add-Content -LiteralPath $log -Value $s -Encoding UTF8 };" ^
  "Emit '[CHECK] installed.properties'; $p=Join-Path $root 'installed.properties'; Get-Content -LiteralPath $p -ErrorAction SilentlyContinue | Where-Object { $_ -match '^(java.version|xmage.version|xmage.client.javaopts|xmage.server.javaopts)=' } | ForEach-Object { Emit ([string]$_) };" ^
  "Emit '[CHECK] Java'; if(Test-Path -LiteralPath $java){ & $java -version 2>&1 | ForEach-Object { Emit ([string]$_) } } else { Emit 'MISSING JAVA' };" ^
  "Emit '[CHECK] puerto 17171 y procesos previos'; $rootMark=$root.TrimEnd('\'); $procs=@(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '(?i)^java(w)?\.exe$' -and ([string]$_.CommandLine) -match '(?i)mage-server' -and ([string]$_.CommandLine) -match [regex]::Escape($rootMark) }); foreach($p0 in $procs){ Emit ('EXACT_SERVER_PID='+$p0.ProcessId+' CMD='+[string]$p0.CommandLine) }; $port=@(& netstat.exe -ano 2>&1 | Select-String -Pattern ':17171\s'); foreach($n in $port){ Emit ('PORT_LINE='+[string]$n.Line) };" ^
  "if($port.Count -gt 0 -and $procs.Count -eq 0){ Emit 'ERROR: 17171 esta ocupado por un proceso que no pertenece a este clon'; exit 21 };" ^
  "if($procs.Count -gt 0){ Emit 'SERVER_ACTION=REUSE_EXISTING_EXACT_SERVER' } else { $line=(Get-Content -LiteralPath $p -ErrorAction SilentlyContinue | Where-Object { $_ -match '^xmage.server.javaopts=' } | Select-Object -First 1); $opts='-Xms512m -Xmx4g -Djava.net.preferIPv4Stack=true'; if($line){ $opts=$line.Substring($line.IndexOf('=')+1).Replace('\=','=') }; $out=$env:XMAGE_SERVER_OUT; $err=$env:XMAGE_SERVER_ERR; $cp=[char]34+(Join-Path $root 'xmage\mage-server\lib\*')+[char]34; $args=($opts+' -cp '+$cp+' mage.server.Main'); Emit ('SERVER_ARGS='+$args); $pr=Start-Process -FilePath $java -ArgumentList $args -WorkingDirectory $root -RedirectStandardOutput $out -RedirectStandardError $err -PassThru; Emit ('SERVER_STARTED_PID='+$pr.Id); }" ^
  "exit 0"
if errorlevel 21 (
  call :fail "El puerto 17171 pertenece a otro proceso; no se ha tocado nada."
  exit /b 1
)
if errorlevel 1 (
  call :fail "No se pudo preparar el servidor."
  exit /b 1
)

call :say "Esperando a que el servidor XMage este realmente listo..."
set /a WAIT=0
:wait_server
"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$c=New-Object Net.Sockets.TcpClient; try{ $c.Connect('127.0.0.1',17171); $c.Close(); exit 0 } catch{ exit 1 }" >nul 2>&1
if not errorlevel 1 goto server_ready
set /a WAIT+=2
call :say "Esperando servidor: !WAIT! / 180 segundos"
if !WAIT! GEQ 180 (
  call :say "ERROR: el servidor no ha abierto 17171 en 180 segundos."
  call :dump_logs
  pause
  exit /b 2
)
timeout /t 2 /nobreak >nul
goto wait_server

:server_ready
call :say "SERVIDOR LISTO en 127.0.0.1:17171"
call :say "Abriendo el lanzador oficial. Pulsa Launch Client; no pulses Launch Client and Server."

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p=Start-Process -FilePath $env:XMAGE_JAVA -ArgumentList @('-jar',$env:XMAGE_LAUNCHER) -WorkingDirectory $env:XMAGE_ROOT -PassThru; Add-Content -LiteralPath $env:XMAGE_LOG -Value ('OFFICIAL_LAUNCHER_PID='+$p.Id) -Encoding UTF8; Write-Host ('OFFICIAL_LAUNCHER_PID='+$p.Id)"
if errorlevel 1 (
  call :fail "No se pudo abrir el lanzador oficial."
  exit /b 1
)

call :say "Listo. El servidor ya estaba preparado antes de abrir el cliente."
call :say "Server stdout: %SERVER_OUT%"
call :say "Server stderr: %SERVER_ERR%"
call :say "Wrapper log: %LOG%"
echo.
echo IMPORTANTE: en el lanzador pulsa solo "Launch Client".
echo No pulses "Launch Client and Server", porque el servidor ya esta activo.
echo.
pause
exit /b 0

:dump_logs
call :say "--- ULTIMAS LINEAS SERVER STDOUT ---"
if exist "%SERVER_OUT%" "%PS%" -NoLogo -NoProfile -Command "Get-Content -LiteralPath $env:XMAGE_SERVER_OUT -Tail 40" 2>nul
call :say "--- ULTIMAS LINEAS SERVER STDERR ---"
if exist "%SERVER_ERR%" "%PS%" -NoLogo -NoProfile -Command "Get-Content -LiteralPath $env:XMAGE_SERVER_ERR -Tail 40" 2>nul
exit /b 0

:say
echo %~1
>>"%LOG%" echo %~1
exit /b 0

:fail
echo ERROR: %~1
>>"%LOG%" echo ERROR: %~1
echo Log: %LOG%
pause
exit /b 1
