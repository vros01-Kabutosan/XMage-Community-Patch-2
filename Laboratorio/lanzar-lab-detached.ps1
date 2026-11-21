$ErrorActionPreference = "Stop"
$labDir = "J:\MTG\Laboratorio\LAB-UI-1.4.62-V2"
$java = "J:\MTG\Instalacion-XMage\java\jre1.8.0_201\bin\java.exe"

# Lanzamiento DESACOPLADO de la consola: Start-Process crea una consola nueva para el proceso,
# de modo que el Ctrl+C que el harness envia al cerrar cada comando de bash no lo mata.
# (Con Win32_Process.Create los JVM del lab morian ~1 s despues de cada comando: ver LEEME.)

$srvJar = (Get-ChildItem -Path "$labDir\servidor\lib" -Filter "mage-server-*.jar").FullName | Select-Object -First 1
$cliJar = (Get-ChildItem -Path "$labDir\cliente\lib" -Filter "mage-client-*.jar").FullName | Select-Object -First 1

$seedDir = "$labDir\tools"
$seedClass = "$seedDir\classes\PrefsSeed.class"
if (Test-Path $seedClass) {
    & $java "-Dxmage.prefs.node=XMageLabUIv2/mage/client" -cp "$seedDir\classes" PrefsSeed localhost 17191 KabutosanLab
}
Write-Host "Preferencias de laboratorio sembradas (localhost:17191, usuario KabutosanLab)"

Write-Host "== Arrancando servidor de laboratorio (consola nueva) =="
$srvArgs = @(
    "-Xmx1024m",
    "-Djava.util.prefs.userRoot=/Software/XMageLabUIv2srv",
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-jar", "`"$srvJar`""
)
$srv = Start-Process -FilePath $java -ArgumentList $srvArgs -WorkingDirectory "$labDir\servidor" `
    -RedirectStandardOutput "$labDir\servidor-lab.log" -RedirectStandardError "$labDir\servidor-lab.err.log" `
    -WindowStyle Hidden -PassThru
Write-Host "Servidor PID $($srv.Id). Esperando puerto 17191..."

$listening = $false
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if (Get-NetTCPConnection -LocalPort 17191 -State Listen -ErrorAction SilentlyContinue) {
        $listening = $true
        Write-Host "Servidor escuchando en puerto 17191 tras $($i+1)s!"
        break
    }
}
if (-not $listening) { Write-Warning "El servidor no escucha en 17191 tras 60s." }

Write-Host "== Arrancando cliente de laboratorio (consola nueva) =="
$cliArgs = @(
    "-Xmx2000m",
    "-Dxmage.prefs.node=XMageLabUIv2/mage/client",
    "-Dxcp.layout.dump=true",
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-jar", "`"$cliJar`""
)
$cli = Start-Process -FilePath $java -ArgumentList $cliArgs -WorkingDirectory "$labDir\cliente" `
    -RedirectStandardOutput "$labDir\cliente-lab.log" -RedirectStandardError "$labDir\cliente-lab.err.log" `
    -WindowStyle Hidden -PassThru
Write-Host "Cliente de laboratorio PID $($cli.Id)."
