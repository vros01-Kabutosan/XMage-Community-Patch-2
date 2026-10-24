[CmdletBinding()]
param(
    [string]$RunRoot = ''
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$ArchiveRoot = 'J:\mtg\_ARCHIVO'
$LogRoot = 'J:\mtg\_LOGS'
$RunPrefix = 'PREPARAR-T-RC1.3-v-1.0.1.5-'
$Port = 17171
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$LogPath = Join-Path $LogRoot "SMOKE-T-RC1.3-v-1.0.1.5-$Stamp.log"
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

try {
    New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
    New-Item -ItemType File -Force -Path $LogPath | Out-Null
    Write-Log 'INICIO: lanzamiento visual aislado de T RC1.3 v-1.0.1'
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
    $serverDir = Join-Path $smokeInstall 'server'
    $clientDir = Join-Path $smokeInstall 'client'
    $serverBat = Join-Path $serverDir 'startServer.bat'
    $clientBat = Join-Path $clientDir 'startClient.bat'
    $imagesDir = Join-Path $clientDir 'plugins\images'
    $installedProperties = Join-Path $smokeInstall 'installed.properties'

    Write-Log "SELECTED_RUN: $selectedRun"
    Write-Log "SMOKE_ROOT: $smokeInstall"
    foreach ($path in @($packageClient, $packageCommon, $smokeClient, $smokeCommonClient, $smokeCommonServer, $serverBat, $clientBat, $installedProperties)) {
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
    $listeners = @(Get-PortListeners)
    if ($listeners.Count -gt 0) {
        $owners = ($listeners | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique) -join ','
        Stop-Failure "El puerto $Port sigue ocupado despues de cerrar Java. PID(s): $owners"
    }
    Write-Log "PORT_$Port: FREE"

    $serverOut = Join-Path $selectedRun 'SMOKE\server-launch.stdout.log'
    $serverErr = Join-Path $selectedRun 'SMOKE\server-launch.stderr.log'
    $clientOut = Join-Path $selectedRun 'SMOKE\client-launch.stdout.log'
    $clientErr = Join-Path $selectedRun 'SMOKE\client-launch.stderr.log'
    Write-Log "SERVER_COMMAND: $env:ComSpec /d /c call startServer.bat"
    $ServerProcess = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', 'call', 'startServer.bat') -WorkingDirectory $serverDir -RedirectStandardOutput $serverOut -RedirectStandardError $serverErr -PassThru
    Write-Log "SERVER_PROCESS_STARTED: PID=$($ServerProcess.Id)"

    $deadline = (Get-Date).AddSeconds(90)
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 1
        $listeners = @(Get-PortListeners)
        if ($listeners.Count -gt 0) {
            $ready = $true
            $owners = ($listeners | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique) -join ','
            Write-Log "SERVER_READY: PASS port=$Port ownerPID=$owners"
            break
        }
        if ($ServerProcess.HasExited) {
            Stop-Failure "El startServer.bat termino antes de abrir el puerto. ExitCode=$($ServerProcess.ExitCode)"
        }
    }
    if (-not $ready) {
        Stop-Failure "El servidor no abrio el puerto $Port en 90 segundos."
    }

    Write-Log "CLIENT_COMMAND: $env:ComSpec /d /c call startClient.bat"
    $ClientProcess = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', 'call', 'startClient.bat') -WorkingDirectory $clientDir -RedirectStandardOutput $clientOut -RedirectStandardError $clientErr -PassThru
    Write-Log "CLIENT_PROCESS_STARTED: PID=$($ClientProcess.Id)"
    Write-Log 'SMOKE_VISUAL: READY'
    Write-Host ''
    Write-Host 'SMOKE VISUAL DE T INICIADO.' -ForegroundColor Green
    Write-Host "Clon probado: $smokeInstall"
    Write-Host 'Prueba la interfaz y las cartas con Trigger. No se toca la instalacion real.'
    Write-Host 'Cuando termines, cierra el cliente y el servidor manualmente.'
    Read-Host 'Pulsa ENTER para cerrar este registro'
    Write-Log 'SMOKE_VISUAL: USER_FINISHED'
    Write-Log "RESULT: SMOKE_LAUNCH_SUCCESS"
} catch {
    Write-Log "RESULT: ABORTED - $($_.Exception.Message)"
    Write-Host ''
    Write-Host 'SMOKE ABORTADO. La instalacion real no se modifica.' -ForegroundColor Red
    Write-Host "Log: $LogPath"
    exit 1
}
