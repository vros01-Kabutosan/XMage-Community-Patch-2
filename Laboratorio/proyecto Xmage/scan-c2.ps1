$ErrorActionPreference = 'SilentlyContinue'
$ProgressPreference = 'SilentlyContinue'

function Size {
    param([string]$Path)
    $sum = 0L; $n = 0
    Get-ChildItem -LiteralPath $Path -Force -Recurse -File -ErrorAction SilentlyContinue |
        ForEach-Object { $sum += $_.Length; $n++ }
    [pscustomobject]@{ Path = $Path; GB = [math]::Round($sum / 1GB, 2); Files = $n }
}

function Table {
    param([string]$Title, [string]$Base)
    "=== $Title ==="
    $dirs = Get-ChildItem -LiteralPath $Base -Directory -Force -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
    $dirs | ForEach-Object { Size $_ } | Sort-Object GB -Descending |
        Where-Object { $_.GB -ge 0.1 } | Format-Table -AutoSize | Out-String -Width 200
}

Table 'AppData\Local\Programs' 'C:\Users\vros0\AppData\Local\Programs'
Table 'Claude-3p' 'C:\Users\vros0\AppData\Local\Claude-3p'
Table '.codex' 'C:\Users\vros0\.codex'
Table 'Google (Local)' 'C:\Users\vros0\AppData\Local\Google'
Table 'Packages' 'C:\Users\vros0\AppData\Local\Packages'
Table 'NVIDIA (Local)' 'C:\Users\vros0\AppData\Local\NVIDIA'
Table 'rattler' 'C:\Users\vros0\AppData\Local\rattler'
Table 'hermes' 'C:\Users\vros0\AppData\Local\hermes'
Table 'runt-nightly' 'C:\Users\vros0\AppData\Local\runt-nightly'
Table '.bun' 'C:\Users\vros0\.bun'
Table 'Documents' 'C:\Users\vros0\Documents'

"=== Downloads biggest files ==="
Get-ChildItem 'C:\Users\vros0\Downloads' -File -Force -Recurse -ErrorAction SilentlyContinue |
    Sort-Object Length -Descending | Select-Object -First 15 |
    ForEach-Object { "{0,8:N2} GB  {1}" -f ($_.Length / 1GB), $_.FullName }

"=== Recycle Bin (C:) ==="
Size 'C:\$Recycle.Bin' | Format-Table -AutoSize | Out-String -Width 200

"=== Temp folders ==="
Size 'C:\Users\vros0\AppData\Local\Temp' | Format-Table -AutoSize | Out-String -Width 200
"=== DONE ==="
