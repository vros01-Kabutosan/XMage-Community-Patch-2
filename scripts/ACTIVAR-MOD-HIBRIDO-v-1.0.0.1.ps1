#requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$Activate,
    [switch]$Launch
)

$ErrorActionPreference = "Stop"
$Version = "ACTIVAR-MOD-HIBRIDO-v-1.0.0.1"
$Project = "J:\MTG\PROYECTO-20260829"
$Source = Join-Path $Project "01-FUENTE"
$Active = "J:\MTG\xmage"
$Logs = Join-Path $Project "05-LOGS"
$Backups = Join-Path $Project "02-MOD\BACKUPS"
$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$Log = Join-Path $Logs "$Version-$Stamp.log"
$Backup = Join-Path $Backups "$Version-$Stamp"
$ClientSource = Join-Path $Source "Mage.Client\target\mage-client-1.4.61.jar"
$ServerSource = Join-Path $Source "Mage.Server\target\mage-server-1.4.61.jar"
$BuildLog = Join-Path $Logs "$Version-$Stamp-BUILD.log"
$MavenHome = Join-Path $Project "03-BUILD\tools\apache-maven-3.9.9"
$Maven = Join-Path $MavenHome "bin\mvn.cmd"
$ClientTarget = Join-Path $Active "client\lib\mage-client-1.4.61.jar"
$ServerTarget = Join-Path $Active "server\lib\mage-server-1.4.61.jar"

New-Item -ItemType Directory -Force -Path $Logs,$Backups | Out-Null
Start-Transcript -LiteralPath $Log -Append | Out-Null

function Write-Log([string]$Message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"), $Message
    Write-Host $line
}

function Require-File([string]$Path,[string]$Label) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Label no existe: $Path"
    }
}

try {
    Write-Log "INICIO=$Version"
    Write-Log "PROJECT=$Project"
    Write-Log "SOURCE=$Source"
    Write-Log "ACTIVE=$Active"
    Write-Log "ACTIVATE=$Activate"
    Write-Log "LAUNCH=$Launch"

    if (-not (Test-Path -LiteralPath $ClientSource -PathType Leaf) -or
        -not (Test-Path -LiteralPath $ServerSource -PathType Leaf)) {
        Write-Log "JAR_FUENTE=FALTAN;BUILD_AUTOMATICO=INICIADO"
        $mavenCandidates = @(
            (Join-Path $Project "03-BUILD\tools\apache-maven-3.9.9\bin\mvn.cmd"),
            (Join-Path $Project "_TOOLS\apache-maven-3.9.9\bin\mvn.cmd"),
            (Join-Path $env:LOCALAPPDATA "Temp\T-tools\maven\apache-maven-3.9.9\bin\mvn.cmd")
        )
        $maven = $mavenCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
        if (-not $maven) {
            $mavenZip = Join-Path $env:TEMP "apache-maven-3.9.9-bin.zip"
            $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
            Write-Log "MAVEN_DOWNLOAD_URL=$mavenUrl"
            Invoke-WebRequest -UseBasicParsing -Uri $mavenUrl -OutFile $mavenZip
            New-Item -ItemType Directory -Force -Path (Split-Path $MavenHome) | Out-Null
            Expand-Archive -LiteralPath $mavenZip -DestinationPath (Split-Path $MavenHome) -Force
            $maven = Join-Path $MavenHome "bin\mvn.cmd"
        }
        if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) {
            throw "Maven no disponible tras la deteccion/descarga."
        }
        Write-Log "MAVEN=$maven"
        if (-not (Test-Path -LiteralPath $BuildLog)) { New-Item -ItemType File -Path $BuildLog -Force | Out-Null }
        & $maven -B -ntp -T 1C -f (Join-Path $Source "pom.xml") package -DskipTests *> $BuildLog
        $buildCode = $LASTEXITCODE
        Write-Log "BUILD_EXITCODE=$buildCode"
        Get-Content -LiteralPath $BuildLog | ForEach-Object { Write-Log ("BUILD: " + $_) }
        if ($buildCode -ne 0) {
            throw "Compilacion automatica fallida. Revisar: $BuildLog"
        }
    }

    if (-not (Test-Path -LiteralPath $ClientSource -PathType Leaf)) {
        throw "JAR_CLIENTE_FUENTE no existe despues de compilar: $ClientSource"
    }
    if (-not (Test-Path -LiteralPath $ServerSource -PathType Leaf)) {
        throw "JAR_SERVIDOR_FUENTE no existe despues de compilar: $ServerSource"
    }
    Require-File $ClientSource "JAR_CLIENTE_FUENTE"
    Require-File $ServerSource "JAR_SERVIDOR_FUENTE"
    Require-File $ClientTarget "JAR_CLIENTE_ACTIVO"
    Require-File $ServerTarget "JAR_SERVIDOR_ACTIVO"

    $sourceClientHash = (Get-FileHash -LiteralPath $ClientSource -Algorithm SHA256).Hash
    $sourceServerHash = (Get-FileHash -LiteralPath $ServerSource -Algorithm SHA256).Hash
    Write-Log "SOURCE_CLIENT_SHA256=$sourceClientHash"
    Write-Log "SOURCE_SERVER_SHA256=$sourceServerHash"

    if (-not $Activate) {
        Write-Log "PREFLIGHT=PASS"
        Write-Log "RESULTADO=COMPLETADO_SIN_CAMBIOS"
        Write-Host "Preflight correcto. Para activar: vuelva a ejecutar con -Activate."
        exit 0
    }

    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match "^(java|javaw|java.exe|javaw.exe)$" -and
            $_.CommandLine -and $_.CommandLine -like "*$Active*"
        })

    foreach ($process in $processes) {
        Write-Log "PROCESS_STOP_BEGIN=PID:$($process.ProcessId);NAME:$($process.Name)"
        Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
        Write-Log "PROCESS_STOP=PASS;PID:$($process.ProcessId)"
    }

    Start-Sleep -Milliseconds 500
    $remaining = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match "^(java|javaw|java.exe|javaw.exe)$" -and
            $_.CommandLine -and $_.CommandLine -like "*$Active*"
        })
    if ($remaining.Count -gt 0) {
        throw "Quedan procesos XMage activos."
    }
    Write-Log "XMAGE_PROCESSES=0"

    New-Item -ItemType Directory -Force -Path $Backup | Out-Null
    Copy-Item -LiteralPath $ClientTarget -Destination (Join-Path $Backup "mage-client-1.4.61.jar.before") -Force
    Copy-Item -LiteralPath $ServerTarget -Destination (Join-Path $Backup "mage-server-1.4.61.jar.before") -Force
    Write-Log "BACKUP=$Backup"
    Write-Log "BACKUP_CLIENT_SHA256=$((Get-FileHash (Join-Path $Backup "mage-client-1.4.61.jar.before") -Algorithm SHA256).Hash)"
    Write-Log "BACKUP_SERVER_SHA256=$((Get-FileHash (Join-Path $Backup "mage-server-1.4.61.jar.before") -Algorithm SHA256).Hash)"

    Copy-Item -LiteralPath $ClientSource -Destination $ClientTarget -Force
    Copy-Item -LiteralPath $ServerSource -Destination $ServerTarget -Force

    $activeClientHash = (Get-FileHash -LiteralPath $ClientTarget -Algorithm SHA256).Hash
    $activeServerHash = (Get-FileHash -LiteralPath $ServerTarget -Algorithm SHA256).Hash
    Write-Log "ACTIVE_CLIENT_SHA256=$activeClientHash"
    Write-Log "ACTIVE_SERVER_SHA256=$activeServerHash"

    if ($activeClientHash -ne $sourceClientHash) { throw "Verificacion cliente fallida." }
    if ($activeServerHash -ne $sourceServerHash) { throw "Verificacion servidor fallida." }
    Write-Log "POST_ACTIVATION_VERIFY=PASS"

    if ($Launch) {
        $launcher = @(Get-ChildItem -LiteralPath $Active -Filter "*.bat" -File -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match "start.*client|client.*start" } |
            Select-Object -First 1)
        if ($launcher.Count -eq 0) {
            throw "No se encontro un launcher de cliente dentro de $Active"
        }
        Write-Log "LAUNCHER=$($launcher.FullName)"
        Start-Process -FilePath $launcher.FullName -WorkingDirectory $launcher.DirectoryName
        Write-Log "LAUNCH=STARTED"
    }

    Write-Log "CONFIG_IMAGES_DECKS=UNCHANGED"
    Write-Log "RESULTADO=COMPLETADO"
    Write-Host "ACTIVACION COMPLETADA. Backup: $Backup"
}
catch {
    Write-Log "RESULTADO=ABORTADO"
    Write-Log "ERROR=$($_.Exception.Message)"
    throw
}
finally {
    Stop-Transcript | Out-Null
}
