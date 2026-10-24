$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$root = 'J:\RelocatedFromC\Users\vros0'
$pairs = @(
    @{ Src = 'C:\Users\vros0\.gradle';                        Dst = "$root\.gradle" },
    @{ Src = 'C:\Users\vros0\.vscode';                        Dst = "$root\.vscode" },
    @{ Src = 'C:\Users\vros0\.cache';                         Dst = "$root\.cache" },
    @{ Src = 'C:\Users\vros0\openwebui_env';                  Dst = "$root\openwebui_env" },
    @{ Src = 'C:\Users\vros0\Nox_share';                      Dst = "$root\Nox_share" },
    @{ Src = 'C:\Users\vros0\Steam_respaldo_mods_20261011';   Dst = "$root\Steam_respaldo_mods_20261011" },
    @{ Src = 'C:\Users\vros0\.evox';                          Dst = "$root\.evox" },
    @{ Src = 'C:\Users\vros0\AppData\Local\evox';             Dst = "$root\AppData\Local\evox" },
    @{ Src = 'C:\Users\vros0\AppData\Roaming\evox-desktop';   Dst = "$root\AppData\Roaming\evox-desktop" },
    @{ Src = 'C:\Users\vros0\AppData\Local\KiloDesktop';      Dst = "$root\AppData\Local\KiloDesktop" },
    @{ Src = 'C:\Users\vros0\AppData\Local\Perplexity';       Dst = "$root\AppData\Local\Perplexity" },
    @{ Src = 'C:\Users\vros0\AppData\Local\SteaMidra';        Dst = "$root\AppData\Local\SteaMidra" },
    @{ Src = 'C:\Users\vros0\AppData\Local\Larian Studios';   Dst = "$root\AppData\Local\Larian Studios" }
)

function Get-Size {
    param([string]$Path)
    $s = (Get-ChildItem -LiteralPath $Path -Recurse -File -Force -ErrorAction SilentlyContinue |
          Measure-Object Length -Sum).Sum
    if ($null -eq $s) { 0 } else { $s }
}

$freeBefore = (Get-PSDrive C).Free
"INICIO libre C: {0:N1} GB" -f ($freeBefore / 1GB)

foreach ($p in $pairs) {
    $src, $dst = $p.Src, $p.Dst
    "=================================================="
    "ITEM $src"
    if (-not (Test-Path -LiteralPath $src)) { "  SKIP: no existe"; continue }
    if ((Get-Item -LiteralPath $src -Force).LinkType -eq 'Junction') { "  SKIP: ya es junction"; continue }

    $before = Get-Size $src
    "  origen: {0:N2} GB" -f ($before / 1GB)
    if ($before -lt 50MB) { "  SKIP: menos de 50 MB, no compensa"; continue }

    New-Item -ItemType Directory -Path $dst -Force | Out-Null
    robocopy $src $dst /E /COPY:DAT /DCOPY:DT /R:1 /W:1 /NFL /NDL /NP /NJH /NJS | Out-Null
    $rc = $LASTEXITCODE
    $after = Get-Size $dst
    "  robocopy={0} copiado={1:N2} GB" -f $rc, ($after / 1GB)

    if ($rc -ge 8 -or $after -lt [long]($before * 0.995)) {
        "  RESULTADO: NO movido (copia incompleta); original intacto"
        continue
    }
    Remove-Item -LiteralPath $src -Recurse -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $src) { "  AVISO: quedan restos en el original (archivos en uso); se deja como esta"; continue }

    cmd /c mklink /J "$src" "$dst" | Out-Null
    $isJ = (Get-Item -LiteralPath $src -Force).LinkType -eq 'Junction'
    "  RESULTADO: movido junction={0}" -f $isJ
}

$freeAfter = (Get-PSDrive C).Free
"FIN libre C: {0:N1} GB  ganado: {1:N1} GB" -f ($freeAfter / 1GB), (($freeAfter - $freeBefore) / 1GB)
"DONE"
