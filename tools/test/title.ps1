# Keep an obvious title on a test client window (WM_SETTEXT only, no focus change) while the process lives.
param([int]$ProcId, [string]$Title = 'AUTOMATED TEST CLIENT - please do not use (Origins Tweaks tests)')
Add-Type @"
using System; using System.Runtime.InteropServices;
public static class W14 { [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern bool SetWindowText(IntPtr h, string t); }
"@
while ($p = Get-Process -Id $ProcId -ErrorAction SilentlyContinue) {
  if ($p.MainWindowHandle -ne [IntPtr]::Zero -and $p.MainWindowTitle -ne $Title) { [W14]::SetWindowText($p.MainWindowHandle, $Title) | Out-Null }
  Start-Sleep -Seconds 3
}
