[CmdletBinding()]
param(
    [string]$RunRoot = ''
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$ArchiveRoot = 'J:\mtg\_ARCHIVO'
$LogRoot = 'J:\mtg\_LOGS'
$RunPrefix = 'PREPARAR-T-RC1.3-v-1.0.1.5-'
$LauncherVersion = '1.0.1.8'
$Port = 17171
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$LogPath = Join-Path $LogRoot "SMOKE-DIRECTO-T-RC1.3-v-$LauncherVersion-$Stamp.log"
$ServerProcess = $null
$ClientProcess = $null

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'), $Message
    $line | Tee-Object -FilePath $LogPath -Append
}

function Stop-Failure {
    param([string]$Message)
    Write-Log "ERROR: $Message"
    throw $Message
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Stop-Failure "Falta archivo para SHA-256: $Path"
    }
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Stop-JavaProcesses {
    $javaProcesses = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
    if ($javaProcesses.Count -eq 0) {
        Write-Log 'JAVA_PROCESS_GUARD: PASS (no habia procesos java/javaw)'
        return
    }
    Write-Log "JAVA_AUTO_CLOSE: detectados $($javaProcesses.Count) procesos java/javaw."
    foreach ($process in $javaProcesses) {
        Write-Log "JAVA_PROCESS_ACTIVE: Name=$($process.ProcessName) Id=$($process.Id)"
        try {
            if ($process.MainWindowHandle -ne 0) {
                $closed = $process.CloseMainWindow()
                Write-Log "JAVA_GRACEFUL_CLOSE_REQUEST: Id=$($process.Id) Result=$closed"
            } else {
                Write-Log "JAVA_GRACEFUL_CLOSE_REQUEST: Id=$($process.Id) Result=NO_WINDOW"
            }
        } catch {
            Write-Log "JAVA_GRACEFUL_CLOSE_ERROR: Id=$($process.Id) Message=$($_.Exception.Message)"
        }
    }
    Start-Sleep -Seconds 3
    $remaining = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
    foreach ($process in $remaining) {
        try {
            Stop-Process -Id $process.Id -Force -ErrorAction Stop
            Write-Log "JAVA_FORCE_CLOSE: Name=$($process.ProcessName) Id=$($process.Id) PASS"
        } catch {
            Write-Log "JAVA_FORCE_CLOSE: Name=$($process.ProcessName) Id=$($process.Id) ERROR=$($_.Exception.Message)"
        }
    }
    Start-Sleep -Seconds 1
    $stillRunning = @(Get-Process -Name java, javaw -ErrorAction SilentlyContinue)
    if ($stillRunning.Count -gt 0) {
        foreach ($process in $stillRunning) {
            Write-Log "JAVA_PROCESS_REMAINS: Name=$($process.ProcessName) Id=$($process.Id)"
        }
        Stop-Failure 'No se pudieron cerrar todos los procesos Java.'
    }
    Write-Log 'JAVA_PROCESS_GUARD: PASS (procesos cerrados)'
}

function Stop-StaleXmageBatchShells {
    try {
        $shells = @(Get-CimInstance Win32_Process -Filter "Name = 'cmd.exe'" -ErrorAction Stop | Where-Object {
            $_.CommandLine -and $_.CommandLine -match '(?i)start(Server|Client)\.bat'
        })
        foreach ($shell in $shells) {
            Write-Log "STALE_XMAGE_CMD: PID=$($shell.ProcessId) COMMAND=$($shell.CommandLine)"
            Stop-Process -Id $shell.ProcessId -Force -ErrorAction Stop
            Write-Log "STALE_XMAGE_CMD_CLOSED: PID=$($shell.ProcessId) PASS"
        }
    } catch {
        Write-Log "STALE_XMAGE_CMD_SCAN: no se pudo completar ($($_.Exception.Message))"
    }
}

function Get-PortListeners {
    try {
        return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop)
    } catch {
        return @()
    }
}

function Resolve-SuccessfulRun {
    if (-not [string]::IsNullOrWhiteSpace($RunRoot)) {
        if (-not (Test-Path -LiteralPath $RunRoot -PathType Container)) {
            Stop-Failure "No existe el RUN_ROOT indicado: $RunRoot"
        }
        return (Resolve-Path -LiteralPath $RunRoot).Path
    }
    $candidates = @(Get-ChildItem -LiteralPath $ArchiveRoot -Directory -Filter "$RunPrefix*" | Sort-Object Name -Descending)
    foreach ($candidate in $candidates) {
        $runLog = Join-Path $candidate.FullName 'RUN.log'
        if ((Test-Path -LiteralPath $runLog -PathType Leaf) -and (Select-String -LiteralPath $runLog -Pattern 'RESULT: PREPARATION_SUCCESS' -Quiet)) {
            return $candidate.FullName
        }
    }
    Stop-Failure "No se encontro ningun SMOKE exitoso con el prefijo $RunPrefix"
}

function Get-SmokeJavaCandidates {
    param([Parameter(Mandatory = $true)][string]$SmokeInstall)
    $candidates = @()
    $candidates += @(Get-ChildItem -Path (Join-Path $SmokeInstall 'java\jre1.8*\bin\java.exe') -File -ErrorAction SilentlyContinue)
    $candidates += @(Get-ChildItem -Path (Join-Path $SmokeInstall 'java\jre17*\bin\java.exe') -File -ErrorAction SilentlyContinue)
    $candidates += @(Get-ChildItem -Path (Join-Path $SmokeInstall 'java\*\bin\java.exe') -File -ErrorAction SilentlyContinue)
    $seen = @{}
    foreach ($candidate in $candidates) {
        if (-not $seen.ContainsKey($candidate.FullName)) {
            $seen[$candidate.FullName] = $true
            $candidate.FullName
        }
    }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $command -and -not $seen.ContainsKey($command.Source)) {
        $command.Source
    }
}

function Write-FileTailToLog {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Path
    )
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        foreach ($line in @(Get-Content -LiteralPath $Path -Tail 30 -ErrorAction SilentlyContinue)) {
            Write-Log "$Label $line"
        }
    }
}

try {
    New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
    New-Item -ItemType File -Force -Path $LogPath | Out-Null
    Write-Log "INICIO: lanzamiento directo visual aislado de T RC1.3 v-$LauncherVersion"
    Write-Log "ARCHIVE_ROOT: $ArchiveRoot"
    Write-Log "LOG_FINAL: $LogPath"

    if (-not (Test-Path -LiteralPath 'J:\' -PathType Container)) {
        Stop-Failure 'La unidad J: no esta disponible.'
    }
    $selectedRun = Resolve-SuccessfulRun
    $smokeInstall = Join-Path $selectedRun 'SMOKE\xmage'
    $packageClient = Join-Path $selectedRun 'T-PACKAGE\client\lib\mage-client-1.4.61.jar'
    $packageCommon = Join-Path $selectedRun 'T-PACKAGE\client\lib\mage-common-1.4.61.jar'
    $smokeClient = Join-Path $smokeInstall 'client\lib\mage-client-1.4.61.jar'
    $smokeCommonClient = Join-Path $smokeInstall 'client\lib\mage-common-1.4.61.jar'
    $smokeCommonServer = Join-Path $smokeInstall 'server\lib\mage-common-1.4.61.jar'
    $serverJar = Join-Path $smokeInstall 'server\lib\mage-server-1.4.61.jar'
    $clientDir = Join-Path $smokeInstall 'client'
    $serverDir = Join-Path $smokeInstall 'server'
    $imagesDir = Join-Path $clientDir 'plugins\images'
    $installedProperties = Join-Path $smokeInstall 'installed.properties'

    Write-Log "SELECTED_RUN: $selectedRun"
    Write-Log "SMOKE_ROOT: $smokeInstall"
    foreach ($path in @($packageClient, $packageCommon, $smokeClient, $smokeCommonClient, $smokeCommonServer, $serverJar, $installedProperties)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            Stop-Failure "Falta elemento obligatorio del SMOKE: $path"
        }
    }
    if (-not (Test-Path -LiteralPath $imagesDir -PathType Container)) {
        Stop-Failure "Falta la carpeta de imagenes del SMOKE: $imagesDir"
    }
    $imageCount = @(Get-ChildItem -LiteralPath $imagesDir -Recurse -File -ErrorAction Stop).Count
    if ($imageCount -eq 0) {
        Stop-Failure 'La carpeta de imagenes del SMOKE esta vacia.'
    }
    Write-Log "SMOKE_IMAGES: PASS files=$imageCount"

    $packageClientHash = Get-Sha256 $packageClient
    $packageCommonHash = Get-Sha256 $packageCommon
    if ((Get-Sha256 $smokeClient) -ne $packageClientHash) {
        Stop-Failure 'El client JAR del SMOKE no coincide con el paquete compilado.'
    }
    foreach ($path in @($smokeCommonClient, $smokeCommonServer)) {
        if ((Get-Sha256 $path) -ne $packageCommonHash) {
            Stop-Failure "El common JAR del SMOKE no coincide: $path"
        }
    }
    Write-Log "SMOKE_JAR_INTEGRITY: PASS client=$packageClientHash common=$packageCommonHash"

    Stop-JavaProcesses
    Stop-StaleXmageBatchShells
    $listeners = @(Get-PortListeners)
    if ($listeners.Count -gt 0) {
        $owners = ($listeners | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique) -join ','
        Stop-Failure "El puerto $Port sigue ocupado despues de cerrar Java. PID(s): $owners"
    }
    Write-Log "PORT_$($Port): FREE"

    $javaCandidates = @(Get-SmokeJavaCandidates -SmokeInstall $smokeInstall)
    if ($javaCandidates.Count -eq 0) {
        Stop-Failure 'No se encontro Java incluido en el SMOKE ni java.exe en PATH.'
    }
    $javaCandidates = @($javaCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -Unique)
    $selectedJava = $null
    $serverReady = $false
    $attempt = 0
    foreach ($javaCandidate in $javaCandidates) {
        $attempt++
        $serverOut = Join-Path $selectedRun ("SMOKE\server-direct-{0}-{1}.stdout.log" -f $Stamp, $attempt)
        $serverErr = Join-Path $selectedRun ("SMOKE\server-direct-{0}-{1}.stderr.log" -f $Stamp, $attempt)
        Write-Log ("JAVA_CANDIDATE_{0}: {1}" -f $attempt, $javaCandidate)
        $versionText = ''
        $savedErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $versionText = (& $javaCandidate -version 2>&1 | Out-String)
        } finally {
            $ErrorActionPreference = $savedErrorActionPreference
        }
        foreach ($versionLine in ($versionText -split '\r?\n')) {
            if (-not [string]::IsNullOrWhiteSpace($versionLine)) {
                Write-Log ("JAVA_VERSION_{0}: {1}" -f $attempt, $versionLine)
            }
        }

        $serverArgs = @(
            '-Xms512m',
            '-Xmx2048m',
            '-Dfile.encoding=UTF-8',
            '-Djava.net.preferIPv4Stack=true',
            '-jar',
            '.\lib\mage-server-1.4.61.jar'
        )
        Write-Log ("SERVER_DIRECT_COMMAND_{0}: {1} {2}" -f $attempt, $javaCandidate, ($serverArgs -join ' '))
        $ServerProcess = Start-Process -FilePath $javaCandidate -ArgumentList $serverArgs -WorkingDirectory $serverDir -RedirectStandardOutput $serverOut -RedirectStandardError $serverErr -WindowStyle Hidden -PassThru
        Write-Log "SERVER_DIRECT_PROCESS_STARTED: PID=$($ServerProcess.Id) JAVA=$javaCandidate"

        $deadline = (Get-Date).AddSeconds(25)
        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 1
            $listeners = @(Get-PortListeners)
            if ($listeners.Count -gt 0) {
                $serverReady = $true
                $selectedJava = $javaCandidate
                $owners = ($listeners | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique) -join ','
                Write-Log "SERVER_READY: PASS port=$Port ownerPID=$owners JAVA=$selectedJava"
                break
            }
            if ($ServerProcess.HasExited) {
                break
            }
        }
        if ($serverReady) {
            break
        }
        if ($ServerProcess.HasExited) {
            Write-Log "SERVER_DIRECT_EXIT: PID=$($ServerProcess.Id) ExitCode=$($ServerProcess.ExitCode)"
        } else {
            Write-Log "SERVER_DIRECT_TIMEOUT: PID=$($ServerProcess.Id)"
            Stop-Process -Id $ServerProcess.Id -Force -ErrorAction SilentlyContinue
        }
        Write-FileTailToLog -Label ("SERVER_STDERR_{0}:" -f $attempt) -Path $serverErr
        Write-FileTailToLog -Label ("SERVER_STDOUT_{0}:" -f $attempt) -Path $serverOut
        $listeners = @(Get-PortListeners)
        if ($listeners.Count -gt 0) {
            $owners = ($listeners | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique) -join ','
            Stop-Failure "Un intento de servidor dejo ocupado el puerto $Port. PID(s): $owners"
        }
    }
    if (-not $serverReady) {
        Stop-Failure 'No se pudo arrancar el servidor directo con ningun Java disponible. Revisa los server-direct-*.stderr.log.'
    }

    $javawCandidate = Join-Path (Split-Path $selectedJava -Parent) 'javaw.exe'
    if (-not (Test-Path -LiteralPath $javawCandidate -PathType Leaf)) {
        $javawCandidate = $selectedJava
    }
    $clientOut = Join-Path $selectedRun "SMOKE\client-direct-$Stamp.stdout.log"
    $clientErr = Join-Path $selectedRun "SMOKE\client-direct-$Stamp.stderr.log"
    $clientArgs = @(
        '-Xms1024m',
        '-Xmx4096m',
        '-Dfile.encoding=UTF-8',
        '-Djava.net.preferIPv4Stack=true',
        '-Dsun.java2d.uiScale=1.5',
        '-jar',
        '.\lib\mage-client-1.4.61.jar'
    )
    Write-Log ("CLIENT_DIRECT_COMMAND: {0} {1}" -f $javawCandidate, ($clientArgs -join ' '))
    $ClientProcess = Start-Process -FilePath $javawCandidate -ArgumentList $clientArgs -WorkingDirectory $clientDir -RedirectStandardOutput $clientOut -RedirectStandardError $clientErr -WindowStyle Hidden -PassThru
    Write-Log "CLIENT_DIRECT_PROCESS_STARTED: PID=$($ClientProcess.Id) JAVA=$javawCandidate"
    Start-Sleep -Seconds 5
    if ($ClientProcess.HasExited) {
        Write-Log "CLIENT_DIRECT_EXIT: PID=$($ClientProcess.Id) ExitCode=$($ClientProcess.ExitCode)"
        Write-FileTailToLog -Label 'CLIENT_STDERR:' -Path $clientErr
        Write-FileTailToLog -Label 'CLIENT_STDOUT:' -Path $clientOut
        Stop-Failure 'El proceso real del cliente termino antes de 5 segundos; no se declara SMOKE listo.'
    }

    Write-Log 'CLIENT_PROCESS_ALIVE: PASS'
    Write-Log 'SMOKE_VISUAL: READY'
    Write-Host ''
    Write-Host 'SMOKE VISUAL DE T INICIADO CORRECTAMENTE.' -ForegroundColor Green
    Write-Host "Clon probado: $smokeInstall"
    Write-Host 'Servidor y cliente Java reales estan activos sin consolas negras.'
    Write-Host 'Prueba la interfaz y las cartas con Trigger. No se toca la instalacion real.'
    Write-Host 'Cuando termines, cierra el cliente y el servidor manualmente.'
    Write-Log 'RESULT: SMOKE_LAUNCH_SUCCESS'
} catch {
    Write-Log "RESULT: ABORTED - $($_.Exception.Message)"
    Write-Host ''
    Write-Host 'SMOKE ABORTADO. La instalacion real no se modifica.' -ForegroundColor Red
    Write-Host "Log: $LogPath"
    exit 1
}
