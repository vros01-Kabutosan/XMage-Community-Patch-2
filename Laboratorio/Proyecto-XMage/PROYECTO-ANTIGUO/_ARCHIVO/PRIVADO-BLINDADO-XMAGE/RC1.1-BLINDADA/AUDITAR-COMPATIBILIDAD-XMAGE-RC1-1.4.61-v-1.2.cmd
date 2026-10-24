@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
title Auditoria XMage RC1 1.4.61 - progreso visible

set "ROOT=%~dp0"
set "LOGDIR=%ROOT%logs"
if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
set "LOG=%LOGDIR%\compatibility-audit-%RANDOM%.log"
set "XMAGE_ROOT=%ROOT%"
set "XMAGE_AUDIT_LOG=%LOG%"
set "PS=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

(
  echo ================================================================
  echo XMAGE CLIENT/SERVER COMPATIBILITY AUDIT v1.2
  echo Date: %DATE% %TIME%
  echo Root: %ROOT%
  echo ================================================================
) > "%LOG%"

echo.
echo XMage: auditoria iniciada.
echo No se modifica ningun archivo del proyecto.
echo Log en tiempo real: %LOG%
echo.

if not exist "%PS%" (
  echo ERROR: no se encuentra Windows PowerShell.
  >>"%LOG%" echo ERROR: no se encuentra Windows PowerShell.
  pause
  exit /b 1
)

"%PS%" -NoLogo -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Continue';" ^
  "$root=$env:XMAGE_ROOT; $log=$env:XMAGE_AUDIT_LOG;" ^
  "function Emit([string]$s){ Write-Host $s; Add-Content -LiteralPath $log -Value $s -Encoding UTF8 };" ^
  "Emit ''; Emit '[1/7] INSTALLED.PROPERTIES';" ^
  "$ip=Join-Path $root 'installed.properties'; $props=@();" ^
  "if(Test-Path -LiteralPath $ip){ $props=@(Get-Content -LiteralPath $ip); foreach($line in $props){ if($line -match '^(java.version|xmage.version|xmage.launcher.version|xmage.client.javaopts|xmage.server.javaopts)='){ Emit ([string]$line) } } } else { Emit ('MISSING: '+$ip) };" ^
  "$ui=$props | Where-Object { $_ -match '^xmage.client.javaopts=' }; if($ui -and ($ui.Contains('Dsun.java2d.uiScale\=1.5') -or $ui.Contains('Dsun.java2d.uiScale=1.5'))){ Emit 'UI LOCK: OK -Dsun.java2d.uiScale=1.5' } else { Emit 'UI LOCK: WARNING - no se ha encontrado uiScale=1.5' };" ^
  "Emit ''; Emit '[2/7] JAVA EXACTO DEL LANZADOR';" ^
  "$java=Join-Path $root 'java\jre1.8.0_201\bin\java.exe'; Emit ('JAVA: '+$java);" ^
  "if(Test-Path -LiteralPath $java){ try{ & $java -version 2>&1 | ForEach-Object { Emit ([string]$_) } } catch{ Emit ('JAVA_ERROR: '+$_.Exception.Message) } } else { Emit ('MISSING: '+$java) };" ^
  "Emit ''; Emit '[3/7] PUERTO 17171';" ^
  "$portLines=@(& netstat.exe -ano 2>&1 | Select-String -Pattern ':17171\s'); if($portLines.Count -eq 0){ Emit 'PORT: libre o sin escucha' } else { foreach($x in $portLines){ Emit ([string]$x.Line) } };" ^
  "Emit ''; Emit '[4/7] RUTAS Y ENLACES DEL LANZADOR';" ^
  "$check=@('xmage\mage-client','xmage\mage-server','java\jre1.8.0_201'); foreach($rel in $check){ $full=Join-Path $root $rel; $item=Get-Item -LiteralPath $full -Force -ErrorAction SilentlyContinue; if($item){ Emit ('PATH: '+$item.FullName); Emit ('ATTRIBUTES: '+[string]$item.Attributes); if($item.PSObject.Properties.Name -contains 'LinkType'){ Emit ('LINKTYPE: '+[string]$item.LinkType) }; if($item.PSObject.Properties.Name -contains 'Target'){ Emit ('TARGET: '+([string]($item.Target -join '; '))) } } else { Emit ('MISSING: '+$full) } };" ^
  "Emit ''; Emit '[5/7] PROCESOS XMAGE';" ^
  "$procs=@(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '(?i)java' -and $_.CommandLine -match '(?i)mage|xmage' }); if($procs.Count -eq 0){ Emit 'No hay procesos Java XMage visibles' } else { foreach($p in $procs){ Emit ('PID='+$p.ProcessId+' PARENT='+$p.ParentProcessId+' NAME='+$p.Name); Emit ('CMD='+[string]$p.CommandLine) } };" ^
  "Emit ''; Emit '[6/7] HASH SHA256 DE LOS JAR QUE USA EL LANZADOR';" ^
  "$jarDirs=@(@{Role='client';Rel='xmage\mage-client\lib'},@{Role='server';Rel='xmage\mage-server\lib'}); $records=New-Object System.Collections.Generic.List[object];" ^
  "foreach($entry in $jarDirs){ $dir=Join-Path $root $entry.Rel; if(-not(Test-Path -LiteralPath $dir)){ Emit ('MISSING_DIR: '+$dir); continue }; $files=@(Get-ChildItem -LiteralPath $dir -Filter '*.jar' -File -ErrorAction SilentlyContinue | Sort-Object Name); Emit ('DIR: '+$entry.Role+' | '+$dir+' | JARS='+$files.Count); $n=0; foreach($f in $files){ $n++; Emit(('HASH '+$entry.Role+' [{0}/{1}] {2}' -f $n,$files.Count,$f.Name)); try{ $h=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash; Emit(('JAR: '+$entry.Role+' | '+$f.Name+' | '+$f.Length+' bytes | '+$f.LastWriteTime.ToString('s')+' | SHA256 '+$h)); $records.Add([pscustomobject]@{Role=$entry.Role;Name=$f.Name;Hash=$h}) } catch{ Emit('HASH_ERROR: '+$f.FullName+' | '+$_.Exception.Message) } } };" ^
  "Emit ''; Emit 'COMPARACION DE JAR CON EL MISMO NOMBRE'; $groups=$records | Group-Object Name | Where-Object { $_.Count -gt 1 } | Sort-Object Name; foreach($g in $groups){ $c=@($g.Group | Where-Object Role -eq 'client'); $s=@($g.Group | Where-Object Role -eq 'server'); if($c.Count -gt 0 -and $s.Count -gt 0){ $state='DIFFERENT'; if($c[0].Hash -eq $s[0].Hash){$state='SAME'}; Emit ('COMPARE: '+$g.Name+' | '+$state+' | client='+$c[0].Hash+' | server='+$s[0].Hash) } };" ^
  "Emit ''; Emit '[7/7] ULTIMOS LOGS DE CLIENTE, SERVIDOR Y LANZADOR';" ^
  "$ld=Join-Path $root 'logs'; if(Test-Path -LiteralPath $ld){ $recent=@(Get-ChildItem -LiteralPath $ld -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(server|client|launcher|launcher-ui-lock|server-diagnostic)-.*\.log$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 6); foreach($f in $recent){ Emit ('--- '+$f.Name+' | '+$f.LastWriteTime); try{ Get-Content -LiteralPath $f.FullName -Tail 40 -ErrorAction Stop | ForEach-Object { Emit ('  '+[string]$_) } } catch{ Emit ('LOG_ERROR: '+$_.Exception.Message) } } } else { Emit ('MISSING_LOG_DIR: '+$ld) };" ^
  "Emit ''; Emit '[END] Auditoria completada.'; Emit ('LOG: '+$log)"

set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" echo La auditoria termino con codigo %RC%; revisa el log.
echo Auditoria terminada. No se ha modificado ningun archivo del proyecto.
echo Log generado: %LOG%
echo.
pause
exit /b %RC%
