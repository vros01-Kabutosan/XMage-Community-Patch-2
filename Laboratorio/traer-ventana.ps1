$ErrorActionPreference = "Stop"
Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Collections.Generic;
public class WinX {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr h);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr h);
  public static List<string> Rows = new List<string>();
  public static bool Scan(IntPtr h, IntPtr l) {
    if (!IsWindowVisible(h)) return true;
    int len = GetWindowTextLength(h);
    if (len == 0) return true;
    StringBuilder sb = new StringBuilder(len + 1);
    GetWindowText(h, sb, sb.Capacity);
    uint pid; GetWindowThreadProcessId(h, out pid);
    Rows.Add(pid + "|" + ((long)h).ToString() + "|" + sb.ToString());
    return true;
  }
}
"@

[WinX]::EnumWindows([WinX+EnumProc]{ param($h,$l) [WinX]::Scan($h,$l) }, [IntPtr]::Zero) | Out-Null

$labPid = 0
$procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'"
foreach ($p in $procs) {
  if ($p.CommandLine -match 'XMageLabUIv2' -and $p.CommandLine -match 'mage-client') { $labPid = $p.ProcessId }
}
Write-Output ("lab client pid: " + $labPid)

$pick = $null
foreach ($r in [WinX]::Rows) {
  $c = $r.Split("|")
  if ($c[0] -eq "$labPid" -and $c[2] -match 'XMage') { $pick = $c }
}
if ($null -eq $pick) {
  foreach ($r in [WinX]::Rows) {
    $c = $r.Split("|")
    if ($c[2] -match 'XMage.*Client') { $pick = $c }
  }
}
if ($null -eq $pick) { Write-Output "SIN VENTANA XMAGE"; exit 1 }

$h = [IntPtr]([long]$pick[1])
[WinX]::ShowWindow($h, 3) | Out-Null      # SW_MAXIMIZE
[WinX]::BringWindowToTop($h) | Out-Null
[WinX]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Seconds 2
Write-Output ("activada hwnd=" + $h + " titulo=" + $pick[2])
