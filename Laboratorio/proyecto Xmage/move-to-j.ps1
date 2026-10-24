$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$pairs = @(
    @{ Src = 'C:\Users\vros0\.codex\sessions';                          Dst = 'J:\Migracion-C\codex-sessions' },
    @{ Src = 'C:\Users\vros0\.bun';                                    Dst = 'J:\Migracion-C\bun' },
    @{ Src = 'C:\Users\vros0\AppData\Local\runt-nightly\envs';         Dst = 'J:\Migracion-C\runt-envs' },
    @{ Src = 'C:\Users\vros0\AppData\Local\hermes';                    Dst = 'J:\Migracion-C\hermes' },
    @{ Src = 'C:\Users\vros0\AppData\Local\Claude-3p\vm_bundles';      Dst = 'J:\Migracion-C\claude-vm-bundles' },
    @{ Src = 'C:\Users\vros0\Downloads';                               Dst = 'J:\Migracion-C\Downloads' },
    @{ Src = 'C:\Users\vros0\Documents\Codex';                         Dst = 'J:\Migracion-C\Documents-Codex' }
)

function Get-Size {
    param([string]$Path)
    $s = (Get-ChildItem -LiteralPath $Path -Recurse -File -Force -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum
    if ($null -eq $s) { 0 } else { $s }
}

$freeBefore = (Get-PSDrive C).Free
"INICIO libre C: {0:N1} GB" -f ($freeBefore / 1GB)

foreach ($p in $pairs) {
    $src, $dst = $p.Src, $p.Dst
    "=================================================="
    "ITEM $src"

    if (-not (Test-Path -LiteralPath $src)) { "  SKIP: el origen no existe"; continue }
    $item = Get-Item -LiteralPath $src -Force
    if ($item.LinkType -eq 'Junction') { "  SKIP: ya es una junction"; continue }

    $before = Get-Size $src
    "  origen: {0:N2} GB" -f ($before / 1GB)

    if (Test-Path -LiteralPath $dst) { "  destino ya existe, se reutiliza" }
    New-Item -ItemType Directory -Path $dst -Force | Out-Null

    $rc = (robocopy $src $dst /E /R:1 /W:1 /NFL /NDL /NP /NJH /NJS) | Out-Null
    $rc = $LASTEXITCODE
    "  robocopy exit=$rc"

    $after = Get-Size $dst
    "  copiado: {0:N2} GB" -f ($after / 1GB)

    $ok = ($rc -lt 8) -and ($before -gt 0) -and ($after -ge [long]($before * 0.995))
    if (-not $ok) {
        "  RESULTADO: NO movido (copia incompleta); se conserva el original"
        continue
    }

    Remove-Item -LiteralPath $src -Recurse -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $src) {
        "  RESULTADO: copia OK pero no se pudo borrar el original; se deja como está"
        continue
    }

    cmd /c mklink /J "$src" "$dst" | Out-Null
    $isJunction = (Get-Item -LiteralPath $src -Force).LinkType -eq 'Junction'
    "  RESULTADO: {0}  (junction={1})" -f 'movido', $isJunction
}

$freeAfter = (Get-PSDrive C).Free
"FIN libre C: {0:N1} GB  ganado en este paso: {1:N1} GB" -f ($freeAfter / 1GB), (($freeAfter - $freeBefore) / 1GB)
"DONE"
