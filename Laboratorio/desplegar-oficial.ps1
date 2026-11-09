# Despliegue coherente de jars XMage del fork a la instalacion oficial.
# Regla de oro aprendida el 2026-11-09: NUNCA desplegar un subset de jars
# recompilados (core/cartas/servidor/plugins se despliegan SIEMPRE juntos y
# SIEMPRE desde un build con `clean`, nunca incremental).
#
# Uso:  .\desplegar-oficial.ps1                       (copia core+sets+server+common+player-ai+plugins)
#       .\desplegar-oficial.ps1 -SoloLib              (solo lib: mage, common, sets, server, player-ai)
#
# Antes de copiar: pide confirmacion, crea backup-jars-<version>-<fecha>_<tag>\
# y exige que el servidor oficial este PARADO (no puede sobrescribir jars en uso).

param(
    [switch]$SoloLib,
    [switch]$Yes,
    [string]$Tag = 'sync-full-deploy'
)

$ErrorActionPreference = 'Stop'

$inst    = 'J:\MTG\Instalacion-XMage\xmage'
$src     = 'J:\MTG\Laboratorio\XMage-1.4.62V1-src'
$version = '1.4.63'

$libJars = @(
    "$src\Mage\target\mage-$version.jar",
    "$src\Mage.Common\target\mage-common-$version.jar",
    "$src\Mage.Sets\target\mage-sets-$version.jar",
    "$src\Mage.Server\target\mage-server-$version.jar",
    "$src\Mage.Server.Plugins\Mage.Player.AI\target\mage-player-ai-$version.jar"
)

$pluginJars = @(Get-ChildItem "$src\Mage.Server.Plugins" -Recurse -Filter "mage-*$version.jar" |
    Where-Object { $_.FullName -match '\\target\\' } | Select-Object -ExpandProperty FullName)

if ($SoloLib) { $pluginJars = @() }

if (($libJars + $pluginJars) | Where-Object { -not (Test-Path $_) }) {
    throw "Faltan jars del build. Ejecuta antes: mvn -B clean package -DskipTests -pl <modulos> -am"
}

# El servidor debe estar parado para poder sobrescribir sus jars
$listener = Get-NetTCPConnection -LocalPort 17171 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    throw "El servidor oficial sigue escuchando en 17171 (PID $($listener.OwningProcess)). Paralo antes de desplegar."
}

$stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
$bakDir  = "$inst\backup-jars-$version-$stamp`_$Tag"
New-Item -ItemType Directory -Force $bakDir | Out-Null

Write-Host "== Backup de jars actuales -> $bakDir =="
Copy-Item "$inst\mage-server\lib\mage-$version.jar","$inst\mage-server\lib\mage-common-$version.jar","$inst\mage-server\lib\mage-sets-$version.jar","$inst\mage-server\lib\mage-server-$version.jar","$inst\mage-server\lib\mage-player-ai-$version.jar" $bakDir -ErrorAction SilentlyContinue
Copy-Item "$inst\mage-server\plugins\mage-*$version.jar" $bakDir -ErrorAction SilentlyContinue

if (-not $Yes) {
    $r = Read-Host "Sobrescribir $($libJars.Count + $pluginJars.Count) jars en la instalacion oficial? (s/N)"
    if ($r -ne 's') { Write-Host 'Cancelado (backup conservado).'; exit 1 }
}

Write-Host "== Copiando lib =="
foreach ($j in $libJars) {
    Copy-Item $j "$inst\mage-server\lib\$(Split-Path $j -Leaf)" -Force
    Write-Host ("   " + (Split-Path $j -Leaf))
}
if ($pluginJars.Count -gt 0) {
    Write-Host "== Copiando plugins =="
    foreach ($j in $pluginJars) {
        Copy-Item $j "$inst\mage-server\plugins\$(Split-Path $j -Leaf)" -Force
    }
    Write-Host ("   " + $pluginJars.Count + " plugins")
}

Write-Host "== Despliegue completado. Arranca el servidor con: $inst\mage-server\startServer.bat =="
Write-Host "Rollback: copia de vuelta el contenido de $bakDir"
