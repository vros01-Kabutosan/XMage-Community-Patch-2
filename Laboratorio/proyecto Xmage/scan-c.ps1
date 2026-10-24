$ErrorActionPreference = 'SilentlyContinue'
$ProgressPreference = 'SilentlyContinue'

function Get-DirSize {
    param([string]$Path)
    $sum = 0L
    $files = 0
    Get-ChildItem -LiteralPath $Path -Force -Recurse -File -ErrorAction SilentlyContinue |
        ForEach-Object { $sum += $_.Length; $files++ }
    [pscustomobject]@{
        Path  = $Path
        GB    = [math]::Round($sum / 1GB, 2)
        Files = $files
    }
}

function Report {
    param([string]$Title, [string[]]$Paths)
    $rows = foreach ($p in $Paths) { Get-DirSize -Path $p }
    $rows | Sort-Object GB -Descending | Format-Table -AutoSize | Out-String -Width 200
}

"=== C: root folders ==="
$rootDirs = Get-ChildItem 'C:\' -Directory -Force | Where-Object { $_.Name -notin '$Recycle.Bin', 'System Volume Information' } | Select-Object -ExpandProperty FullName
Report 'root' $rootDirs

"=== C:\Users ==="
$userDirs = Get-ChildItem 'C:\Users' -Directory -Force | Select-Object -ExpandProperty FullName
Report 'users' $userDirs

"=== C:\Users\vros0 ==="
$profileDirs = Get-ChildItem 'C:\Users\vros0' -Directory -Force | Select-Object -ExpandProperty FullName
Report 'profile' $profileDirs

"=== C:\Users\vros0\AppData (2 levels) ==="
foreach ($sub in 'Local', 'Roaming', 'LocalLow') {
    $base = "C:\Users\vros0\AppData\$sub"
    if (Test-Path $base) {
        "--- $sub ---"
        $dirs = Get-ChildItem $base -Directory -Force | Select-Object -ExpandProperty FullName
        Report $sub $dirs
    }
}

"=== ProgramData ==="
Report 'programdata' (Get-ChildItem 'C:\ProgramData' -Directory -Force | Select-Object -ExpandProperty FullName)

"=== Program Files ==="
Report 'pf' (Get-ChildItem 'C:\Program Files' -Directory -Force | Select-Object -ExpandProperty FullName)

"=== Program Files (x86) ==="
Report 'pf86' (Get-ChildItem 'C:\Program Files (x86)' -Directory -Force | Select-Object -ExpandProperty FullName)

"=== DONE ==="
