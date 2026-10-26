# Copy of packdev/menu/test/grab.ps1: PrintWindow capture without focus or cursor changes. If the window is minimized it is
# restored without activation behind all windows, captured, and minimized again. Usage: grab2.ps1 -ProcId <pid> -Out <png>
param([int]$ProcId, [string]$Out)
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System; using System.Runtime.InteropServices;
public struct RECT4 { public int Left, Top, Right, Bottom; }
public static class W4 {
  [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT4 r);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@
[W4]::SetProcessDPIAware() | Out-Null
$h = (Get-Process -Id $ProcId).MainWindowHandle
$wasMin = [W4]::IsIconic($h)
if ($wasMin) { [W4]::ShowWindow($h, 4) | Out-Null; [W4]::SetWindowPos($h, [IntPtr]1, 0, 0, 0, 0, 0x0013) | Out-Null; Start-Sleep -Milliseconds 1500 }
$r = New-Object RECT4; [W4]::GetClientRect($h, [ref]$r) | Out-Null
$bmp = New-Object System.Drawing.Bitmap $r.Right, $r.Bottom
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
$ok = [W4]::PrintWindow($h, $hdc, 3)  # PW_CLIENTONLY | PW_RENDERFULLCONTENT
$g.ReleaseHdc($hdc)
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
if ($wasMin) { [W4]::ShowWindow($h, 7) | Out-Null }  # SW_SHOWMINNOACTIVE
"$($r.Right)x$($r.Bottom) printwindow=$ok"
