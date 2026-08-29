[CmdletBinding()]
param(
    [switch]$Activate,
    [switch]$Launch,
    [switch]$ForceCloseXmage
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$Version = "v-1.0.1.1"
$RepoUrl = "https://github.com/vros01-Kabutosan/XMage-Community-Patch-2.git"
$Branch = "work/importacion-instalacion-v-1.0.0"
$Active = "J:\MTG\xmage"
$FixedProject = "J:\MTG\PROYECTO-20260829"

$ProjectRoot = $null
if (Test-Path -Path $FixedProject -PathType Container) {
    $ProjectRoot = $FixedProject
}
elseif ($PSScriptRoot -and (Test-Path -Path (Join-Path $PSScriptRoot "01-FUENTE") -PathType Container)) {
    $ProjectRoot = $PSScriptRoot
}
else {
    throw "No se encuentra el proyecto. Se esperaba $FixedProject o una carpeta con 01-FUENTE junto al script."
}

$RunId = Get-Date -Format "yyyyMMdd-HHmmss-fff"
$LogDir = Join-Path $ProjectRoot "05-LOGS"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$Log = Join-Path $LogDir ("ACTIVAR-MOD-HIBRIDO-T-TRIGGER-" + $Version + "-" + $RunId + ".log")
New-Item -ItemType File -Force -Path $Log | Out-Null
$script:Log = $Log
$TranscriptLog = Join-Path $LogDir ("ACTIVAR-MOD-HIBRIDO-T-TRIGGER-" + $Version + "-" + $RunId + "-TRANSCRIPT.log")
$TranscriptStarted = $false
try {
    Start-Transcript -Path $TranscriptLog -Force -IncludeInvocationHeader | Out-Null
    $TranscriptStarted = $true
}
catch {
    Add-Content -Path $script:Log -Value ("[" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff") + "] TRANSCRIPT_START=FAIL;ERROR=" + $_.Exception.Message) -Encoding UTF8
}

$ExitCode = 1
$BackupRoot = $null
$BackupItems = @()
$OverlayPlans = @()
$ReplacedPlans = @()
$Git = $null
$Maven = $null
$BuildJavaHome = $null
$BuildJava = $null
$Source = $null
$BuiltClient = $null
$BuiltCommon = $null

function Write-Log {
    param([string]$Message)
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $line = "[" + $stamp + "] " + $Message
    Add-Content -Path $script:Log -Value $line -Encoding UTF8
    Write-Host $line
}

function Format-NativeCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )
    $parts = @('"' + $FilePath + '"')
    foreach ($argument in @($Arguments)) {
        $value = [string]$argument
        $parts += '"' + $value.Replace('"', '\"') + '"'
    }
    return ($parts -join " ")
}

function Invoke-NativeLogged {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$Label
    )
    if (-not (Test-Path -Path $FilePath -PathType Leaf)) {
        throw "$Label no existe: $FilePath"
    }

    Write-Log ("CMD_" + $Label + "=" + (Format-NativeCommand $FilePath $Arguments))
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = @()
    $code = 1
    try {
        $global:LASTEXITCODE = 0
        $output = @(& $FilePath @Arguments 2>&1 | ForEach-Object { [string]$_ })
        $code = [int]$global:LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $savedPreference
    }

    foreach ($line in $output) {
        Write-Log ("OUT_" + $Label + "=" + [string]$line)
    }
    Write-Log ("EXIT_" + $Label + "=" + $code)

    if ($code -ne 0) {
        throw "$Label terminó con código $code."
    }
    return ,$output
}

function Get-Hash {
    param([string]$Path)
    $hash = (Get-FileHash -Path $Path -Algorithm SHA256).Hash.ToUpperInvariant()
    return $hash
}

function Add-ArtifactLog {
    param(
        [string]$Label,
        [string]$Path
    )
    $item = Get-Item -Path $Path
    Write-Log ($Label + "=" + $Path + ";BYTES=" + $item.Length + ";SHA256=" + (Get-Hash $Path))
}

function Get-ZipHashMap {
    param([string]$Path)
    $result = @{}
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName.EndsWith("/")) {
                continue
            }
            $stream = $entry.Open()
            $sha = [Security.Cryptography.SHA256]::Create()
            try {
                $hash = ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace("-", "").ToUpperInvariant()
                $result[$entry.FullName] = $hash
            }
            finally {
                $sha.Dispose()
                $stream.Dispose()
            }
        }
    }
    finally {
        $zip.Dispose()
    }
    return ,$result
}

function New-OverlayJar {
    param(
        [string]$ActivePath,
        [string]$BuiltPath,
        [string[]]$TargetEntries,
        [string]$TempPath
    )
    if (Test-Path -Path $TempPath) {
        throw "El temporal ya existe; no se sobrescribe: $TempPath"
    }

    Copy-Item -Path $ActivePath -Destination $TempPath -Force
    Write-Log "TEMP_COPY=$ActivePath -> $TempPath"

    $destinationZip = $null
    $sourceZip = $null
    try {
        $destinationZip = [IO.Compression.ZipFile]::Open($TempPath, [IO.Compression.ZipArchiveMode]::Update)
        $sourceZip = [IO.Compression.ZipFile]::OpenRead($BuiltPath)

        foreach ($entryName in $TargetEntries) {
            $sourceEntry = $sourceZip.GetEntry($entryName)
            if ($null -eq $sourceEntry) {
                throw "La entrada requerida no existe en el JAR construido: $entryName"
            }

            $oldEntry = $destinationZip.GetEntry($entryName)
            if ($null -ne $oldEntry) {
                $oldEntry.Delete()
            }

            $newEntry = $destinationZip.CreateEntry($entryName)
            $entryIn = $sourceEntry.Open()
            $entryOut = $newEntry.Open()
            try {
                $entryIn.CopyTo($entryOut)
            }
            finally {
                $entryOut.Dispose()
                $entryIn.Dispose()
            }
            Write-Log "OVERLAY_ENTRY=$entryName;SOURCE=$BuiltPath"
        }
    }
    finally {
        if ($null -ne $sourceZip) {
            $sourceZip.Dispose()
        }
        if ($null -ne $destinationZip) {
            $destinationZip.Dispose()
        }
    }
}

function Assert-Overlay {
    param(
        [string]$Label,
        [string]$ActivePath,
        [string]$BuiltPath,
        [string[]]$TargetEntries,
        [hashtable]$Before
    )
    $built = Get-ZipHashMap $BuiltPath
    $after = Get-ZipHashMap $ActivePath
    $allowed = @{}
    foreach ($entryName in $TargetEntries) {
        $allowed[$entryName] = $true
        if (-not $built.ContainsKey($entryName)) {
            throw "$($Label): entrada construida ausente: $entryName"
        }
        if (-not $after.ContainsKey($entryName)) {
            throw "$($Label): entrada final ausente: $entryName"
        }
        if ($built[$entryName] -ne $after[$entryName]) {
            throw "$($Label): hash de entrada no coincide: $entryName"
        }
        Write-Log ("VERIFY_" + $Label + "_ENTRY=" + $entryName + ";SHA256=" + $after[$entryName])
    }

    $untouched = 0
    foreach ($entryName in $Before.Keys) {
        $name = [string]$entryName
        if ($allowed.ContainsKey($name)) {
            continue
        }
        if (-not $after.ContainsKey($name)) {
            throw "$($Label): se eliminó una entrada no autorizada: $name"
        }
        if ($Before[$name] -ne $after[$name]) {
            throw "$($Label): cambió una entrada no autorizada: $name"
        }
        $untouched++
    }

    foreach ($entryName in $after.Keys) {
        $name = [string]$entryName
        if ((-not $Before.ContainsKey($name)) -and (-not $allowed.ContainsKey($name))) {
            throw "$($Label): apareció una entrada no autorizada: $name"
        }
    }

    Write-Log ("VERIFY_" + $Label + "=PASS;BEFORE_ENTRIES=" + $Before.Count + ";AFTER_ENTRIES=" + $after.Count + ";UNTOUCHED_ENTRIES=" + $untouched)
    return ,$after
}

function Test-ValidSource {
    param(
        [string]$Candidate,
        [string]$ExpectedRemoteSha
    )
    if (-not (Test-Path -Path (Join-Path $Candidate ".git"))) {
        return $false
    }
    if (-not (Test-Path -Path (Join-Path $Candidate "pom.xml"))) {
        return $false
    }

    try {
        $remoteOutput = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Candidate, "config", "--get", "remote.origin.url") -Label "GIT_SOURCE_REMOTE"
        $branchOutput = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Candidate, "branch", "--show-current") -Label "GIT_SOURCE_BRANCH"
        $headOutput = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Candidate, "rev-parse", "HEAD") -Label "GIT_SOURCE_HEAD"
        $statusOutput = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Candidate, "status", "--short") -Label "GIT_SOURCE_STATUS"

        $remote = ([string](@($remoteOutput)[0])).Trim().TrimEnd("/")
        $expectedRemote = $RepoUrl.TrimEnd("/")
        $branch = ([string](@($branchOutput)[0])).Trim()
        $head = ([string](@($headOutput)[0])).Trim()
        $status = (@($statusOutput) -join "").Trim()

        Write-Log "SOURCE_CANDIDATE=$Candidate;REMOTE=$remote;BRANCH=$branch;HEAD=$head;STATUS_BYTES=$($status.Length)"
        if (($remote -ieq $expectedRemote) -and ($branch -eq $Branch) -and ($head -eq $ExpectedRemoteSha) -and ($status.Length -eq 0)) {
            return $true
        }
    }
    catch {
        Write-Log "SOURCE_CANDIDATE_REJECTED=$Candidate;ERROR=$($_.Exception.Message)"
    }
    return $false
}

function Find-Java17 {
    $homes = @()
    if ($env:JAVA_HOME) {
        $homes += $env:JAVA_HOME
    }
    $homes += "C:\Program Files\BellSoft\LibericaJDK-17"
    $homes += "C:\Program Files\Eclipse Adoptium\jdk-17"
    $homes += "C:\Program Files\Java\jdk-17"
    $javaParent = "C:\Program Files\Java"
    if (Test-Path -Path $javaParent -PathType Container) {
        $homes += @(Get-ChildItem -Path $javaParent -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "jdk-17*" } | ForEach-Object { $_.FullName })
    }

    foreach ($home in @($homes | Where-Object { $_ } | Select-Object -Unique)) {
        $exe = Join-Path $home "bin\java.exe"
        if (-not (Test-Path -Path $exe -PathType Leaf)) {
            continue
        }
        try {
            $versionOutput = Invoke-NativeLogged -FilePath $exe -Arguments @("-version") -Label "JAVA_VERSION"
            $version = (@($versionOutput) -join " ")
            if ($version -match 'version "17\.') {
                return [PSCustomObject]@{ Home = $home; Exe = $exe }
            }
        }
        catch {
            Write-Log "JAVA_CANDIDATE_REJECTED=$home;ERROR=$($_.Exception.Message)"
        }
    }
    throw "No se encontró un JDK 17 ejecutable para compilar."
}

function Find-Maven {
    $candidates = @(
        (Join-Path $ProjectRoot "03-BUILD\tools\apache-maven-3.9.9\bin\mvn.cmd"),
        "J:\MTG\_TOOLS\apache-maven-3.9.9\bin\mvn.cmd",
        (Join-Path $ProjectRoot "03-BUILD\apache-maven-3.9.9\bin\mvn.cmd")
    )
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        $candidates += $command.Source
    }

    foreach ($candidate in @($candidates | Where-Object { $_ } | Select-Object -Unique)) {
        if (Test-Path -Path $candidate -PathType Leaf) {
            return $candidate
        }
    }

    $toolsDir = Join-Path $ProjectRoot "03-BUILD\tools"
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $archive = Join-Path $toolsDir "apache-maven-3.9.9-bin.zip"
    $checksumFile = Join-Path $toolsDir "apache-maven-3.9.9-bin.zip.sha512"
    $archiveUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    $checksumUrl = $archiveUrl + ".sha512"

    if (-not (Test-Path -Path $archive -PathType Leaf)) {
        Write-Log "MAVEN_DOWNLOAD=$archiveUrl"
        Invoke-WebRequest -UseBasicParsing -Uri $archiveUrl -OutFile $archive
    }
    if (-not (Test-Path -Path $checksumFile -PathType Leaf)) {
        Write-Log "MAVEN_CHECKSUM_DOWNLOAD=$checksumUrl"
        Invoke-WebRequest -UseBasicParsing -Uri $checksumUrl -OutFile $checksumFile
    }

    $expected = [regex]::Match((Get-Content -Path $checksumFile -Raw), "[0-9a-fA-F]{128}").Value.ToUpperInvariant()
    if ($expected.Length -ne 128) {
        throw "No se pudo leer SHA-512 de Apache Maven."
    }
    $actual = (Get-FileHash -Path $archive -Algorithm SHA512).Hash.ToUpperInvariant()
    Write-Log "MAVEN_ARCHIVE_SHA512=$actual;EXPECTED=$expected"
    if ($actual -ne $expected) {
        throw "La verificación SHA-512 de Maven falló."
    }

    $mavenDir = Join-Path $toolsDir "apache-maven-3.9.9"
    if (-not (Test-Path -Path (Join-Path $mavenDir "bin\mvn.cmd") -PathType Leaf)) {
        Write-Log "MAVEN_EXPAND=$mavenDir"
        Expand-Archive -Path $archive -DestinationPath $toolsDir -Force
    }
    $resolved = Join-Path $mavenDir "bin\mvn.cmd"
    if (-not (Test-Path -Path $resolved -PathType Leaf)) {
        throw "Maven 3.9.9 no quedó disponible en $resolved"
    }
    return $resolved
}


function Test-PortOpen {
    param(
        [string]$ComputerName,
        [int]$Port
    )
    $tcp = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $tcp.BeginConnect($ComputerName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(750)) {
            return $false
        }
        $tcp.EndConnect($async)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $tcp.Close()
    }
}

function Start-ExistingLauncher {
    param(
        [string]$Path,
        [string]$Label
    )
    if ($Path.EndsWith(".ps1", [System.StringComparison]::OrdinalIgnoreCase)) {
        $powershell = (Get-Command powershell.exe -ErrorAction Stop).Source
        $process = Start-Process -FilePath $powershell -ArgumentList @(
            "-NoLogo",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            $Path
        ) -WorkingDirectory (Split-Path -Parent $Path) -PassThru
    }
    else {
        $cmd = (Get-Command cmd.exe -ErrorAction Stop).Source
        $quotedPath = '"' + $Path + '"'
        $process = Start-Process -FilePath $cmd -ArgumentList @("/d", "/c", $quotedPath) -WorkingDirectory (Split-Path -Parent $Path) -PassThru
    }
    Write-Log ("LAUNCH_" + $Label + "=STARTED;PATH=" + $Path + ";PID=" + $process.Id)
    return $process
}

function Find-ActiveJava8 {
    $candidates = @(
        (Join-Path $Active "java\jre1.8.0_201\bin\java.exe"),
        (Join-Path $Active "java\jre1.8.0_201\jre\bin\java.exe"),
        (Join-Path $Active "java\bin\java.exe")
    )
    foreach ($candidate in @($candidates | Select-Object -Unique)) {
        if (-not (Test-Path -Path $candidate -PathType Leaf)) {
            continue
        }
        try {
            $versionOutput = Invoke-NativeLogged -FilePath $candidate -Arguments @("-version") -Label "JAVA_ACTIVE_VERSION"
            $versionText = (@($versionOutput) -join " ")
            if ($versionText -match 'version "1\.8\.') {
                return $candidate
            }
        }
        catch {
            Write-Log "JAVA_ACTIVE_CANDIDATE_REJECTED=$candidate;ERROR=$($_.Exception.Message)"
        }
    }
    throw "No se encontró Java 8 dentro de la instalación activa; se evita el error de versiones cliente/servidor."
}

try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Write-Log "INICIO=ACTIVAR-MOD-HIBRIDO-T-TRIGGER-$Version"
    Write-Log "SCRIPT=$PSCommandPath"
    Write-Log "POWERSHELL_VERSION=$($PSVersionTable.PSVersion)"
    Write-Log "POWERSHELL_HOME=$PSHOME"
    Write-Log "COMPATIBILITY=NO_LITERALPATH_SWITCHES"
    Write-Log "TRANSCRIPT_LOG=$TranscriptLog;STARTED=$TranscriptStarted"
    Write-Log "PROJECT=$ProjectRoot"
    Write-Log "REPOSITORY=$RepoUrl"
    Write-Log "BRANCH=$Branch"
    Write-Log "ACTIVE=$Active"
    Write-Log "ACTIVATE=$Activate;LAUNCH=$Launch;FORCE_CLOSE_XMAGE=$ForceCloseXmage"
    Write-Log "POLICY=NO_MIR;NO_FORCE_PUSH;NO_COMPLETE_JAR_COPY;ENTRY_OVERLAY_ONLY"
    Write-Log "PROTECTED=CONFIG_IMAGES_DECKS_LAUNCHER_AND_SERVER_MODS"

    if (-not (Test-Path -Path $Active -PathType Container)) {
        throw "No existe la instalación activa: $Active"
    }

    $gitCommand = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($null -ne $gitCommand) {
        $Git = $gitCommand.Source
    }
    elseif (Test-Path -Path "C:\Program Files\Git\cmd\git.exe" -PathType Leaf) {
        $Git = "C:\Program Files\Git\cmd\git.exe"
    }
    else {
        throw "Git no está disponible."
    }
    Write-Log "GIT=$Git"

    $remoteHeadOutput = Invoke-NativeLogged -FilePath $Git -Arguments @("ls-remote", $RepoUrl, ("refs/heads/" + $Branch)) -Label "GIT_REMOTE_HEAD"
    $remoteHeadLine = [string](@($remoteHeadOutput)[0])
    $RemoteHead = (($remoteHeadLine -split "\s+")[0]).Trim()
    if ($RemoteHead -notmatch "^[0-9a-fA-F]{40}$") {
        throw "No se pudo verificar el HEAD remoto de la rama."
    }
    Write-Log "REMOTE_HEAD=$RemoteHead"

    $sourceCandidate = Join-Path $ProjectRoot "01-FUENTE"
    $cloneCandidate = Join-Path $ProjectRoot "03-BUILD\SOURCE-FINAL"
    if (Test-ValidSource -Candidate $sourceCandidate -ExpectedRemoteSha $RemoteHead) {
        $Source = $sourceCandidate
        Write-Log "SOURCE_SELECTED=$Source;MODE=EXISTING_VALID_CLONE"
    }
    elseif (Test-Path -Path $cloneCandidate) {
        if (Test-ValidSource -Candidate $cloneCandidate -ExpectedRemoteSha $RemoteHead) {
            $Source = $cloneCandidate
            Write-Log "SOURCE_SELECTED=$Source;MODE=EXISTING_VALID_CLONE"
        }
        else {
            throw "Existe una fuente no válida o modificada en $cloneCandidate; se conserva y no se sobrescribe."
        }
    }
    else {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $cloneCandidate) | Out-Null
        $null = Invoke-NativeLogged -FilePath $Git -Arguments @("clone", "--single-branch", "--branch", $Branch, $RepoUrl, $cloneCandidate) -Label "GIT_CLONE_FINAL_SOURCE"
        if (-not (Test-ValidSource -Candidate $cloneCandidate -ExpectedRemoteSha $RemoteHead)) {
            throw "El clon final no coincide con el HEAD remoto o está sucio."
        }
        $Source = $cloneCandidate
        Write-Log "SOURCE_SELECTED=$Source;MODE=NEW_CLONE"
    }

    $requiredFiles = @(
        "Mage.Client/src/main/java/mage/client/MageFrame.java",
        "Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java",
        "Mage.Client/src/main/java/mage/client/components/ability/AbilityPicker.java",
        "Mage.Client/src/main/java/mage/client/game/GamePanel.java",
        "Mage.Client/src/main/java/mage/client/util/gui/GuiDisplayUtil.java",
        "Mage.Client/src/main/java/org/mage/card/arcane/CardPanelRenderModeImage.java",
        "Mage.Client/src/main/java/org/mage/card/arcane/CardRenderer.java",
        "Mage.Client/src/main/java/org/mage/plugins/card/CardPluginImpl.java",
        "Mage.Common/src/main/java/mage/view/PermanentView.java"
    )
    foreach ($relative in $requiredFiles) {
        $full = Join-Path $Source ($relative -replace "/", "\")
        if (-not (Test-Path -Path $full -PathType Leaf)) {
            throw "Falta archivo de fuente requerido: $relative"
        }
        Write-Log "SOURCE_FILE=PASS;$relative"
    }

    $markerChecks = @(
        @{ Relative = "Mage.Client/src/main/java/mage/client/components/ability/AbilityPicker.java"; Pattern = "class CheckmarkIcon"; Name = "CHECKMARK_ICON" },
        @{ Relative = "Mage.Client/src/main/java/org/mage/card/arcane/CardPanelRenderModeImage.java"; Pattern = "triggerIndicatorPanel"; Name = "TRIGGER_INDICATOR" },
        @{ Relative = "Mage.Common/src/main/java/mage/view/PermanentView.java"; Pattern = "hasActiveTrigger"; Name = "ACTIVE_TRIGGER_API" },
        @{ Relative = "Mage.Client/src/main/java/org/mage/card/arcane/CardRenderer.java"; Pattern = "int height = setSymbol.getHeight"; Name = "CARD_RENDERER_DECOMP_FIX" }
    )
    foreach ($check in $markerChecks) {
        $full = Join-Path $Source ($check.Relative -replace "/", "\")
        if (-not (Select-String -Path $full -Pattern $check.Pattern -SimpleMatch -Quiet)) {
            throw "No pasa marcador $($check.Name) en $($check.Relative)"
        }
        Write-Log "SOURCE_MARKER=$($check.Name);PASS"
    }

    $badFiles = @(
        "Mage.Client/src/main/java/mage/client/MageFrame.java",
        "Mage.Client/src/main/java/mage/client/cards/DragCardGrid.java",
        "Mage.Client/src/main/java/mage/client/game/GamePanel.java",
        "Mage.Client/src/main/java/org/mage/card/arcane/CardRenderer.java"
    )
    foreach ($relative in $badFiles) {
        $full = Join-Path $Source ($relative -replace "/", "\")
        if (Select-String -Path $full -Pattern "void var22_", "for (void " -SimpleMatch -Quiet) {
            throw "Patrón decompilado inválido en $relative"
        }
    }

    $statusAfterValidation = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Source, "status", "--short") -Label "GIT_STATUS_BEFORE_BUILD"
    $null = Invoke-NativeLogged -FilePath $Git -Arguments @("-C", $Source, "diff", "--check") -Label "GIT_DIFF_CHECK"
    if ((@($statusAfterValidation) -join "").Trim().Length -ne 0) {
        throw "La fuente no está limpia antes del build."
    }

    $javaInfo = Find-Java17
    $BuildJavaHome = $javaInfo.Home
    $BuildJava = $javaInfo.Exe
    $env:JAVA_HOME = $BuildJavaHome
    $env:Path = (Join-Path $BuildJavaHome "bin") + ";" + $env:Path
    Write-Log "JAVA_HOME_BUILD=$BuildJavaHome"
    Write-Log "JAVA_BUILD=$BuildJava"

    $Maven = Find-Maven
    Write-Log "MAVEN=$Maven"

    $buildArgs = @(
        "-B",
        "-ntp",
        "-T",
        "1C",
        "-f",
        (Join-Path $Source "pom.xml"),
        "clean",
        "package",
        "-Dxmage.dataCollectors.printGameLogs=false"
    )
    $null = Invoke-NativeLogged -FilePath $Maven -Arguments $buildArgs -Label "MAVEN_CLEAN_PACKAGE_TEST"
    Write-Log "BUILD_TEST=PASS"

    $BuiltClient = Join-Path $Source "Mage.Client\target\mage-client-1.4.61.jar"
    $BuiltCommon = Join-Path $Source "Mage.Common\target\mage-common-1.4.61.jar"
    foreach ($artifact in @($BuiltClient, $BuiltCommon)) {
        if (-not (Test-Path -Path $artifact -PathType Leaf)) {
            throw "No se generó artefacto requerido: $artifact"
        }
    }
    Add-ArtifactLog -Label "BUILT_CLIENT" -Path $BuiltClient
    Add-ArtifactLog -Label "BUILT_COMMON" -Path $BuiltCommon

    if (-not $Activate) {
        Write-Log "ACTIVATION=NOT_REQUESTED"
        $ExitCode = 0
    }
    else {
        $processes = @(Get-CimInstance -ClassName Win32_Process -ErrorAction SilentlyContinue | Where-Object {
            $exe = [string]$_.ExecutablePath
            $cmd = [string]$_.CommandLine
            $exe.StartsWith($Active, [System.StringComparison]::OrdinalIgnoreCase) -or ($cmd.IndexOf($Active, [System.StringComparison]::OrdinalIgnoreCase) -ge 0)
        })
        foreach ($process in $processes) {
            Write-Log "XMAGE_PROCESS=PID:$($process.ProcessId);NAME:$($process.Name);COMMAND:$([string]$process.CommandLine)"
        }
        if (($processes.Count -gt 0) -and (-not $ForceCloseXmage)) {
            throw "Hay procesos XMage activos. Repite con -ForceCloseXmage; no se toca una instalación en uso."
        }
        foreach ($process in $processes) {
            Stop-Process -Id ([int]$process.ProcessId) -Force -ErrorAction Stop
            Write-Log "PROCESS_STOP=PASS;PID=$($process.ProcessId)"
        }
        Start-Sleep -Milliseconds 500

        $activeClientMatches = @()
        if (Test-Path -Path (Join-Path $Active "client\lib") -PathType Container) {
            $activeClientMatches += @(Get-ChildItem -Path (Join-Path $Active "client\lib") -Filter "mage-client-1.4.61.jar" -File -ErrorAction SilentlyContinue)
        }
        if ($activeClientMatches.Count -eq 0) {
            $activeClientMatches += @(Get-ChildItem -Path $Active -Filter "mage-client-1.4.61.jar" -File -Recurse -ErrorAction SilentlyContinue)
        }
        $activeClientMatches = @($activeClientMatches | Sort-Object -Property FullName -Unique)
        if ($activeClientMatches.Count -ne 1) {
            throw "Se esperaba exactamente un mage-client-1.4.61.jar activo y se encontraron $($activeClientMatches.Count)."
        }
        $ActiveClient = $activeClientMatches[0].FullName

        $activeCommonMatches = @()
        foreach ($libraryDir in @((Join-Path $Active "client\lib"), (Join-Path $Active "server\lib"))) {
            if (Test-Path -Path $libraryDir -PathType Container) {
                $activeCommonMatches += @(Get-ChildItem -Path $libraryDir -Filter "mage-common-1.4.61.jar" -File -ErrorAction SilentlyContinue)
            }
        }
        $activeCommonMatches = @($activeCommonMatches | Sort-Object -Property FullName -Unique)
        if ($activeCommonMatches.Count -eq 0) {
            throw "No se encontró mage-common-1.4.61.jar en client\lib o server\lib."
        }

        Write-Log "ACTIVE_CLIENT_JAR=$ActiveClient"
        foreach ($jar in $activeCommonMatches) {
            Write-Log "ACTIVE_COMMON_JAR=$($jar.FullName)"
        }

        $BackupRoot = Join-Path (Join-Path $ProjectRoot "02-MOD\BACKUPS") ("ACTIVAR-MOD-HIBRIDO-T-TRIGGER-" + $Version + "-" + $RunId)
        New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null
        Write-Log "BACKUP_ROOT=$BackupRoot"

        $allActive = @([PSCustomObject]@{ Path = $ActiveClient; Built = $BuiltClient; Targets = @('mage/client/components/ability/AbilityPicker.class', 'mage/client/components/ability/AbilityPicker$CheckmarkIcon.class', 'org/mage/card/arcane/CardPanelRenderModeImage.class') })
        foreach ($jar in $activeCommonMatches) {
            $allActive += [PSCustomObject]@{ Path = $jar.FullName; Built = $BuiltCommon; Targets = @("mage/view/PermanentView.class") }
        }

        foreach ($item in $allActive) {
            $backupName = (($item.Path -replace ":", "_") -replace "\\", "_") + ".before.jar"
            $backupPath = Join-Path $BackupRoot $backupName
            Copy-Item -Path $item.Path -Destination $backupPath -Force
            $beforeHash = Get-Hash $item.Path
            $backupHash = Get-Hash $backupPath
            if ($beforeHash -ne $backupHash) {
                throw "Backup no coincide para $($item.Path)"
            }
            $backupRecord = [PSCustomObject]@{ Active = $item.Path; Backup = $backupPath; Before = $beforeHash }
            $BackupItems += $backupRecord
            Write-Log "BACKUP=PASS;ACTIVE=$($item.Path);PATH=$backupPath;SHA256=$beforeHash"
        }

        foreach ($item in $allActive) {
            $beforeEntries = Get-ZipHashMap $item.Path
            $tempPath = $item.Path + "." + $RunId + ".overlay.tmp"
            New-OverlayJar -ActivePath $item.Path -BuiltPath $item.Built -TargetEntries $item.Targets -TempPath $tempPath
            $afterEntries = Assert-Overlay -Label (($item.Path -replace "[\\:]", "_")) -ActivePath $tempPath -BuiltPath $item.Built -TargetEntries $item.Targets -Before $beforeEntries
            $OverlayPlans += [PSCustomObject]@{ Active = $item.Path; Built = $item.Built; Targets = $item.Targets; Temp = $tempPath; Before = $beforeEntries; PlannedAfter = $afterEntries }
            Write-Log "OVERLAY_PLAN=PASS;ACTIVE=$($item.Path);TEMP=$tempPath"
        }

        foreach ($plan in $OverlayPlans) {
            [IO.File]::Replace($plan.Temp, $plan.Active, $null)
            $ReplacedPlans += $plan
            Write-Log "ATOMIC_REPLACE=PASS;ACTIVE=$($plan.Active)"
        }

        foreach ($plan in $OverlayPlans) {
            $null = Assert-Overlay -Label (($plan.Active -replace "[\\:]", "_") + "_FINAL") -ActivePath $plan.Active -BuiltPath $plan.Built -TargetEntries $plan.Targets -Before $plan.Before
            Add-ArtifactLog -Label "ACTIVE_FINAL" -Path $plan.Active
        }

        foreach ($plan in $OverlayPlans) {
            Write-Log "ACTIVE_ONLY_AND_NON_TARGET_ENTRIES=PRESERVED;JAR=$($plan.Active)"
        }
        Write-Log "SERVER_JAR=NOT_MODIFIED"
        Write-Log "MAGE_SETS_JAR=NOT_MODIFIED"
        Write-Log "CONFIG_IMAGES_DECKS_LAUNCHER=NOT_MODIFIED"
        Write-Log "ACTIVE_INSTALL_MODIFIED=ONLY_ENTRY_OVERLAYS"
        $ExitCode = 0

        if ($Launch) {
            $activeJava8 = Find-ActiveJava8
            $activeJavaBin = Split-Path -Parent $activeJava8
            $env:JAVA_HOME = Split-Path -Parent $activeJavaBin
            Write-Log "JAVA_HOME_RUNTIME=$env:JAVA_HOME"
            Write-Log "JAVA_RUNTIME=$activeJava8"

            $serverLaunchers = @(
                (Join-Path $Active "startServer.bat"),
                (Join-Path $Active "server\startServer.bat"),
                (Join-Path $Active "xmage\startServer.bat"),
                (Join-Path $Active "startServer.ps1"),
                (Join-Path $Active "server\startServer.ps1"),
                (Join-Path $Active "xmage\startServer.ps1")
            )
            $serverLauncher = @($serverLaunchers | Where-Object { Test-Path -Path $_ -PathType Leaf } | Select-Object -First 1)
            if ($serverLauncher.Count -eq 0) {
                $serverLauncher = @(Get-ChildItem -Path $Active -Filter "*.bat" -File -Recurse -ErrorAction SilentlyContinue |
                    Where-Object { Select-String -Path $_.FullName -Pattern "mage.server.Main" -SimpleMatch -Quiet -ErrorAction SilentlyContinue } |
                    Select-Object -First 1 | ForEach-Object { $_.FullName })
            }
            if ($serverLauncher.Count -eq 0) {
                throw "No se encontró un launcher de servidor XMage dentro de $Active; no se inicia solo el cliente."
            }
            $serverPath = [string]$serverLauncher[0]
            Write-Log "SERVER_LAUNCHER=$serverPath"
            $null = Start-ExistingLauncher -Path $serverPath -Label "SERVER"

            $serverReady = $false
            for ($attempt = 0; $attempt -lt 60; $attempt++) {
                if (Test-PortOpen -ComputerName "127.0.0.1" -Port 17171) {
                    $serverReady = $true
                    Write-Log "SERVER_PORT_17171=OPEN;ATTEMPTS=$($attempt + 1)"
                    break
                }
                Start-Sleep -Seconds 1
            }
            if (-not $serverReady) {
                throw "El servidor no abrió el puerto 17171 tras 60 segundos."
            }

            $clientLaunchers = @(
                (Join-Path $Active "startClient.bat"),
                (Join-Path $Active "client\startClient.bat"),
                (Join-Path $Active "xmage\startClient.bat"),
                (Join-Path $Active "startClient.ps1"),
                (Join-Path $Active "client\startClient.ps1"),
                (Join-Path $Active "xmage\startClient.ps1")
            )
            $clientLauncher = @($clientLaunchers | Where-Object { Test-Path -Path $_ -PathType Leaf } | Select-Object -First 1)
            if ($clientLauncher.Count -eq 0) {
                $clientLauncher = @(Get-ChildItem -Path $Active -Filter "*.bat" -File -Recurse -ErrorAction SilentlyContinue |
                    Where-Object { Select-String -Path $_.FullName -Pattern "mage.client.MageFrame", "mage.client.Main" -SimpleMatch -Quiet -ErrorAction SilentlyContinue } |
                    Select-Object -First 1 | ForEach-Object { $_.FullName })
            }
            if ($clientLauncher.Count -eq 0) {
                throw "No se encontró un launcher de cliente XMage dentro de $Active."
            }
            $clientPath = [string]$clientLauncher[0]
            Write-Log "CLIENT_LAUNCHER=$clientPath"
            $null = Start-ExistingLauncher -Path $clientPath -Label "CLIENT"
            Write-Log "LAUNCH=PASS;SERVER_AND_CLIENT=STARTED"
        }
    }
}
catch {
    Write-Log "ERROR=$($_.Exception.Message)"
    if ($Launch -and $Activate) {
        $launched = @(Get-CimInstance -ClassName Win32_Process -ErrorAction SilentlyContinue | Where-Object {
            $exe = [string]$_.ExecutablePath
            $cmd = [string]$_.CommandLine
            $exe.StartsWith($Active, [System.StringComparison]::OrdinalIgnoreCase) -or ($cmd.IndexOf($Active, [System.StringComparison]::OrdinalIgnoreCase) -ge 0)
        })
        foreach ($process in $launched) {
            try {
                Stop-Process -Id ([int]$process.ProcessId) -Force -ErrorAction Stop
                Write-Log "LAUNCH_ROLLBACK_STOP=PASS;PID=$($process.ProcessId);NAME=$($process.Name)"
            }
            catch {
                Write-Log "LAUNCH_ROLLBACK_STOP=FAIL;PID=$($process.ProcessId);ERROR=$($_.Exception.Message)"
            }
        }
    }
    if ($Activate -and ($BackupItems.Count -gt 0)) {
        Write-Log "ROLLBACK_BEGIN=YES"
        foreach ($backup in $BackupItems) {
            try {
                Copy-Item -Path $backup.Backup -Destination $backup.Active -Force
                if ((Get-Hash $backup.Active) -ne $backup.Before) {
                    throw "SHA256 restaurado no coincide"
                }
                Write-Log "ROLLBACK=PASS;ACTIVE=$($backup.Active)"
            }
            catch {
                Write-Log "ROLLBACK=FAIL;ACTIVE=$($backup.Active);ERROR=$($_.Exception.Message)"
            }
        }
    }
    Write-Log "RESULTADO=ABORTADO"
    $ExitCode = 1
}
finally {
    foreach ($plan in $OverlayPlans) {
        if ((Test-Path -Path $plan.Temp -PathType Leaf) -and ($ExitCode -eq 0)) {
            Remove-Item -Path $plan.Temp -Force
            Write-Log "TEMP_CLEAN=PASS;PATH=$($plan.Temp)"
        }
        elseif (Test-Path -Path $plan.Temp -PathType Leaf) {
            Write-Log "TEMP_PRESERVED_FOR_DIAGNOSTIC=$($plan.Temp)"
        }
    }
    Write-Log "LOG_FINAL=$script:Log"
    Write-Log ("RESULTADO_FINAL=" + $(if ($ExitCode -eq 0) { "COMPLETADO" } else { "ABORTADO" }))
    if ($TranscriptStarted) {
        try {
            Stop-Transcript | Out-Null
            $TranscriptStarted = $false
            if (Test-Path -Path $TranscriptLog -PathType Leaf) {
                Add-Content -Path $script:Log -Value ("[" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff") + "] TRANSCRIPT_FINAL=PASS;PATH=" + $TranscriptLog + ";BYTES=" + (Get-Item -Path $TranscriptLog).Length + ";SHA256=" + (Get-Hash $TranscriptLog)) -Encoding UTF8
            }
        }
        catch {
            Add-Content -Path $script:Log -Value ("[" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff") + "] TRANSCRIPT_FINAL=FAIL;ERROR=" + $_.Exception.Message) -Encoding UTF8
        }
    }
}

exit $ExitCode
