@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Auditoria de compatibilidad XMage RC1 1.4.61

set "ROOT=%~dp0"
set "LOGDIR=%ROOT%logs"
set "LOG=%LOGDIR%\compatibility-audit-%RANDOM%.log"
set "JAVA=%ROOT%java\jre1.8.0_201\bin\java.exe"

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1

(
  echo ================================================================
  echo XMAGE CLIENT/SERVER COMPATIBILITY AUDIT
  echo RC1 1.4.61
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo ================================================================
  echo.
  echo [INSTALLED PROPERTIES]
  if exist "%ROOT%installed.properties" findstr /b /c:"java.version=" /c:"xmage.version=" /c:"xmage.client.javaopts=" /c:"xmage.server.javaopts=" "%ROOT%installed.properties"
  echo.
  echo [JAVA USED BY LAUNCHER]
  if exist "%JAVA%" "%JAVA%" -version
  echo.
  echo [PORT 17171]
  netstat -ano | findstr ":17171"
  echo.
  echo [DIRECTORY LINKS]
) > "%LOG%" 2>&1

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$r=$env:ROOT; foreach($p in @('xmage\mage-client','xmage\mage-server','java\jre1.8.0_201')){ $i=Get-Item -LiteralPath (Join-Path $r $p) -Force -ErrorAction SilentlyContinue; if($i){ $i | Select-Object FullName,Attributes,LinkType,Target | Format-List } else { 'MISSING '+(Join-Path $r $p) } }" >> "%LOG%" 2>&1

(
  echo.
  echo [XMAGE JAVA PROCESSES]
) >> "%LOG%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match '(?i)mage|xmage' } | Select-Object ProcessId,ParentProcessId,Name,CommandLine | Format-List" >> "%LOG%" 2>&1

(
  echo.
  echo [JAR INVENTORY AND SHA256]
) >> "%LOG%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "Add-Type -AssemblyName System.IO.Compression.FileSystem; $r=$env:ROOT; $dirs=@('client\lib','server\lib','xmage\mage-client\lib','xmage\mage-server\lib') | ForEach-Object { Join-Path $r $_ }; foreach($d in $dirs){ if(Test-Path $d){ 'DIR '+$d; Get-ChildItem -LiteralPath $d -Filter '*.jar' -File | Sort-Object Name | ForEach-Object { $h=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash; '{0} | {1} bytes | {2} | SHA256 {3}' -f $_.FullName,$_.Length,$_.LastWriteTime.ToString('s'),$h; try{ $z=[IO.Compression.ZipFile]::OpenRead($_.FullName); $e=$z.GetEntry('META-INF/MANIFEST.MF'); if($e){ $sr=New-Object IO.StreamReader($e.Open()); $m=$sr.ReadToEnd(); $sr.Dispose(); $z.Dispose(); $v=($m -split '\r?\n' | Where-Object { $_ -match '^(Implementation-Version|Implementation-Title|Build-Time|Build-Date):' }); if($v){ 'MANIFEST '+($v -join ' | ') } } else { $z.Dispose() } } catch { 'MANIFEST_ERROR '+$_.Exception.Message } } } else { 'MISSING_DIR '+$d } }" >> "%LOG%" 2>&1

(
  echo.
  echo [RECENT XMAGE LOGS - LAST 60 LINES]
) >> "%LOG%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$r=$env:ROOT; $d=Join-Path $r 'logs'; if(Test-Path $d){ $fs=Get-ChildItem -LiteralPath $d -File | Where-Object { $_.Name -match '^(server|client|launcher|launcher-ui-lock|server-diagnostic|compatibility-audit)-.*\.log$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 8; foreach($f in $fs){ '--- '+$f.FullName+' | '+$f.LastWriteTime; Get-Content -LiteralPath $f.FullName -Tail 60 } } else { 'MISSING_LOG_DIR '+$d }" >> "%LOG%" 2>&1

echo. >> "%LOG%"
echo [END] Audit complete. >> "%LOG%"
echo [LOG] %LOG% >> "%LOG%"

echo.
echo Auditoria completada. No se ha modificado ningun archivo.
echo Log generado:
echo %LOG%
echo.
echo Adjunta ese log para alinear cliente y servidor de una vez.
pause
exit /b 0
