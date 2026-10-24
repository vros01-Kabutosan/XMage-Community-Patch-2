$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$root = 'J:\RelocatedFromC'
$pairs = @(
    @{ Src = 'C:\Users\vros0\.codex\sessions';                   Dst = "$root\Users\vros0\.codex\sessions" },
    @{ Src = 'C:\Users\vros0\.bun';                              Dst = "$root\Users\vros0\.bun" },
    @{ Src = 'C:\Users\vros0\AppData\Local\runt-nightly\envs';   Dst = "$root\Users\vros0\AppData\Local\runt-nightly\envs" },
    @{ Src = 'C:\Users\vros0\Downloads';                         Dst = "$root\Users\vros0\Downloads" },
    @{ Src = 'C:\Users\vros0\Documents\Codex';                   Dst = "$root\Users\vros0\Documents\Codex" }
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
    "  destino: $dst"

    if (-not (Test-Path -LiteralPath $src)) { "  SKIP: origen inexistente"; continue }
    if ((Get-Item -LiteralPath $src -Force).LinkType -eq 'Junction') { "  SKIP: ya es junction"; continue }

    $before = Get-Size $src
    "  origen: {0:N2} GB" -f ($before / 1GB)
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
    if (Test-Path -LiteralPath $src) {
        "  AVISO: no se pudo borrar el original por completo; se deja como esta"
        continue
    }

    cmd /c mklink /J "$src" "$dst" | Out-Null
    $probe = Get-ChildItem -LiteralPath $src -Force -ErrorAction SilentlyContinue | Select-Object -First 1
    $isJ = (Get-Item -LiteralPath $src -Force).LinkType -eq 'Junction'
    "  RESULTADO: movido  junction={0}  visible={1}" -f $isJ, [bool]$probe
}

$freeAfter = (Get-PSDrive C).Free
"FIN libre C: {0:N1} GB  ganado en este paso: {1:N1} GB" -f ($freeAfter / 1GB), (($freeAfter - $freeBefore) / 1GB)
"DONE"
