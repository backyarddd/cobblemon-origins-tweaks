# Keep a test client out of the user's way without taking focus: restore it if minimized (SW_SHOWNOACTIVATE) and
# move it to the bottom of the z-order (SWP_NOACTIVATE). PrintWindow captures still work while it is covered.
# Usage: sendback.ps1 -ProcId <pid> [-WaitSeconds 120]
param([int]$ProcId, [int]$WaitSeconds = 120)
Add-Type @"
using System; using System.Runtime.InteropServices;
public static class W11 {
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@
$end = (Get-Date).AddSeconds($WaitSeconds)
do { $h = (Get-Process -Id $ProcId).MainWindowHandle; if ($h -ne [IntPtr]::Zero) { break }; Start-Sleep -Milliseconds 250 } while ((Get-Date) -lt $end)
if ($h -eq [IntPtr]::Zero) { "no window"; exit 1 }
if ([W11]::IsIconic($h)) { [W11]::ShowWindow($h, 4) | Out-Null }
[W11]::SetWindowPos($h, [IntPtr]1, 0, 0, 0, 0, 0x0013) | Out-Null
"sent back"
