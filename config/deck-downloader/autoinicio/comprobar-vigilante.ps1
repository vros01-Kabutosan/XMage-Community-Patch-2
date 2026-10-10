<#
  XMage-VigilanteDecks
  Comprueba si ya hay un vigilante de mazos en ejecucion.

  Se invoca desde INICIAR-VIGILANTE-DECKS.cmd antes de arrancar el vigilante.
  Codigo de salida:
    0 = ya hay una instancia (no arrancar otra)
    1 = no hay ninguna (arrancar)
    2 = no se pudo comprobar (arrancar igualmente)
#>

$ErrorActionPreference = 'SilentlyContinue'

try {
    $vivos = Get-CimInstance Win32_Process -Filter "Name='python.exe' OR Name='pythonw.exe'" |
        Where-Object { $_.CommandLine -like '*vigilante_decks.py*' }
} catch {
    exit 2
}

if ($vivos) {
    exit 0
}
exit 1