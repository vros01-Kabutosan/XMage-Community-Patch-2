#requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$Activate,
    [switch]$Launch
)

$ErrorActionPreference = "Stop"
$Version = "ACTIVAR-MOD-HIBRIDO-v-1.0.0.3"
$Repo = "https://github.com/vros01-Kabutosan/XMage-Community-Patch-2.git"
$Branch = "work/importacion-instalacion-v-1.0.0"
$RequiredJavaCommit = "34977be6537ae17e3e79fc72c5b3e4ccd24b9d71"
$Project = "J:\MTG\PROYECTO-20260829"
$Active = "J:\MTG\xmage"
$Logs = Join-Path $Project "05-LOGS"
$Backups = Join-Path $Project "02-MOD\BACKUPS"
$BuildRoot = Join-Path $Project "03-BUILD"
$LocalSource = Join-Path $Project "01-FUENTE"
$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$Log = Join-Path $Logs "$Version-$Stamp.log"
$BuildLog = Join-Path $Logs "$Version-$Stamp-BUILD.log"
$Backup = Join-Path $Backups "$Version-$Stamp"
$ClientTarget = Join-Path $Active "client\lib\mage-client-1.4.61.jar"
$ServerTarget = Join-Path $Active "server\lib\mage-server-1.4.61.jar"
$Source = $LocalSource
$ClientSource = Join-Path $Source "Mage.Client\target\mage-client-1.4.61.jar"
$ServerSource = Join-Path $Source "Mage.Server\target\mage-server-1.4.61.jar"

New-Item -ItemType Directory -Force -Path $Logs,$Backups,$BuildRoot | Out-Null
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

function Get-GitPath {
    $known = @(
        "C:\Program Files\Git\cmd\git.exe",
        "C:\Program Files\Git\bin\git.exe"
    )
    foreach ($path in $known) {
        if (Test-Path -LiteralPath $path -PathType Leaf) { return $path }
    }
    $command = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw "Git no disponible."
}

function Get-MavenPath {
    $candidates = @(
        (Join-Path $Project "03-BUILD\tools\apache-maven-3.9.9\bin\mvn.cmd"),
        (Join-Path $Project "_TOOLS\apache-maven-3.9.9\bin\mvn.cmd"),
        (Join-Path $env:LOCALAPPDATA "Temp\T-tools\maven\apache-maven-3.9.9\bin\mvn.cmd")
    )
    foreach ($path in $candidates) {
        if (Test-Path -LiteralPath $path -PathType Leaf) { return $path }
    }
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw "Maven no disponible. Instala Apache Maven 3.9.9 en 03-BUILD\tools."
}

try {
    Write-Log "INICIO=$Version"
    Write-Log "REPO=$Repo"
    Write-Log "BRANCH=$Branch"
    Write-Log "REQUIRED_JAVA_COMMIT=$RequiredJavaCommit"
    Write-Log "PROJECT=$Project"
    Write-Log "ACTIVE=$Active"
    Write-Log "ACTIVATE=$Activate"
    Write-Log "LAUNCH=$Launch"

    Require-File $ClientTarget "JAR_CLIENTE_ACTIVO"
    Require-File $ServerTarget "JAR_SERVIDOR_ACTIVO"

    $git = Get-GitPath
    Write-Log "GIT=$git"

    $sourceIsExact = $false
    if (Test-Path -LiteralPath (Join-Path $LocalSource ".git") -PathType Container) {
        $localHead = (& $git -C $LocalSource rev-parse HEAD 2>$null).Trim()
        Write-Log "LOCAL_SOURCE_HEAD=$localHead"
        if ($localHead -eq $ExpectedCommit) {
            $sourceIsExact = $true
            Write-Log "SOURCE_MODE=LOCAL_EXACT_COMMIT"
        } else {
            Write-Log "SOURCE_MODE=LOCAL_RECHAZADA_COMMIT_NO_COINCIDE"
        }
    } else {
        Write-Log "SOURCE_MODE=LOCAL_RECHAZADA_SIN_GIT"
    }

    if (-not $sourceIsExact) {
        $stage = Join-Path $BuildRoot "$Version-$Stamp-source"
        Write-Log "CLEAN_CLONE=$stage"
        $cloneOut = Join-Path $Logs "$Version-$Stamp-GIT-OUT.log"
        $cloneErr = Join-Path $Logs "$Version-$Stamp-GIT-ERR.log"
        & $git clone --quiet --single-branch --branch $Branch $Repo $stage 1> $cloneOut 2> $cloneErr
        $cloneCode = $LASTEXITCODE
        if (Test-Path -LiteralPath $cloneOut) {
            Get-Content -LiteralPath $cloneOut | ForEach-Object { Write-Log ("GIT_OUT: " + $_) }
        }
        if (Test-Path -LiteralPath $cloneErr) {
            Get-Content -LiteralPath $cloneErr | ForEach-Object { Write-Log ("GIT_ERR: " + $_) }
        }
        Write-Log "GIT_CLONE_EXITCODE=$cloneCode"
        if ($cloneCode -ne 0) { throw "No se pudo clonar la rama del mod." }

        $clonedHead = (& $git -C $stage rev-parse HEAD 2>$null).Trim()
        Write-Log "CLONED_HEAD=$clonedHead"
        & $git -C $stage merge-base --is-ancestor $RequiredJavaCommit $clonedHead 2>$null
        $ancestorCode = $LASTEXITCODE
        Write-Log "REQUIRED_JAVA_COMMIT=$RequiredJavaCommit"
        Write-Log "REQUIRED_JAVA_COMMIT_IS_ANCESTOR_EXITCODE=$ancestorCode"
        if ($ancestorCode -ne 0) {
            throw "El clon no contiene el commit Java validado del mod."
        }
        $Source = $stage
        $ClientSource = Join-Path $Source "Mage.Client\target\mage-client-1.4.61.jar"
        $ServerSource = Join-Path $Source "Mage.Server\target\mage-server-1.4.61.jar"
        Write-Log "SOURCE_MODE=CLEAN_EXACT_CLONE"
    }

    Write-Log "SOURCE=$Source"
    Write-Log "CLIENT_SOURCE=$ClientSource"
    Write-Log "SERVER_SOURCE=$ServerSource"

    if (-not (Test-Path -LiteralPath $ClientSource -PathType Leaf) -or
        -not (Test-Path -LiteralPath $ServerSource -PathType Leaf)) {
        $maven = Get-MavenPath
        Write-Log "MAVEN=$maven"
        Write-Log "BUILD_BEGIN=$Source"
        if (-not (Test-Path -LiteralPath $BuildLog)) {
            New-Item -ItemType File -Force -Path $BuildLog | Out-Null
        }
        & $maven -B -ntp -T 1C -f (Join-Path $Source "pom.xml") package -DskipTests *> $BuildLog
        $buildCode = $LASTEXITCODE
        Write-Log "BUILD_EXITCODE=$buildCode"
        Get-Content -LiteralPath $BuildLog | ForEach-Object { Write-Log ("BUILD: " + $_) }
        if ($buildCode -ne 0) {
            throw "Compilacion fallida en fuente exacta. Revisar: $BuildLog"
        }
    } else {
        Write-Log "BUILD=JARS_EXISTENTES_EN_FUENTE_EXACTA"
    }

    Require-File $ClientSource "JAR_CLIENTE_FUENTE_EXACTA"
    Require-File $ServerSource "JAR_SERVIDOR_FUENTE_EXACTA"

    $sourceClientHash = (Get-FileHash -LiteralPath $ClientSource -Algorithm SHA256).Hash
    $sourceServerHash = (Get-FileHash -LiteralPath $ServerSource -Algorithm SHA256).Hash
    Write-Log "SOURCE_CLIENT_SHA256=$sourceClientHash"
    Write-Log "SOURCE_SERVER_SHA256=$sourceServerHash"

    if (-not $Activate) {
        Write-Log "PREFLIGHT=PASS"
        Write-Log "RESULTADO=COMPLETADO_SIN_CAMBIOS"
        Write-Host "Preflight correcto. Para activar, ejecute de nuevo con -Activate."
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
    Start-Sleep -Milliseconds 700

    $remaining = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match "^(java|javaw|java.exe|javaw.exe)$" -and
            $_.CommandLine -and $_.CommandLine -like "*$Active*"
        })
    if ($remaining.Count -gt 0) { throw "Quedan procesos XMage activos." }
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
        if ($launcher.Count -eq 0) { throw "No se encontro launcher de cliente." }
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