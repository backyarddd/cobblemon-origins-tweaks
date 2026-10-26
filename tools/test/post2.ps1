# Copy of packdev/menu/test/post.ps1 extended for flight tests: key holds/sequences and right clicks.
# PostMessage only: never changes focus or the real cursor.
# Usage: post2.ps1 -ProcId <pid> [-X x -Y y [-Click | -RightClick]] [-Seq "<actions>"] [-Text "<chars>"]
#   -X/-Y alone posts WM_MOUSEMOVE. Seq actions, comma separated:
#     d:<vk>        key down          u:<vk>   key up          w:<ms>   wait
#     h:<vk>:<ms>   hold: key down, auto-repeat every 100 ms for <ms>, then key up
param([int]$ProcId, [int]$X = -1, [int]$Y = -1, [switch]$Click, [switch]$RightClick, [string]$Seq = '', [string]$Text = '')
Add-Type @"
using System; using System.Runtime.InteropServices;
public static class W6 {
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
}
"@
$h = (Get-Process -Id $ProcId).MainWindowHandle
function Down([int]$vk, [bool]$repeat) {
  $lp = if ($repeat) { 0x40000001 } else { 1 }
  [W6]::PostMessage($h, 0x0100, [IntPtr]$vk, [IntPtr]$lp) | Out-Null   # WM_KEYDOWN
}
function Up([int]$vk) { [W6]::PostMessage($h, 0x0101, [IntPtr]$vk, [IntPtr]0xC0000001) | Out-Null }  # WM_KEYUP

if ($X -ge 0) {
  $lp = [IntPtr](($Y -shl 16) -bor ($X -band 0xFFFF))
  [W6]::PostMessage($h, 0x0200, [IntPtr]::Zero, $lp) | Out-Null   # WM_MOUSEMOVE
  if ($Click -or $RightClick) {
    Start-Sleep -Milliseconds 300
    $down, $up, $mk = if ($RightClick) { 0x0204, 0x0205, 2 } else { 0x0201, 0x0202, 1 }
    [W6]::PostMessage($h, $down, [IntPtr]$mk, $lp) | Out-Null
    Start-Sleep -Milliseconds 80
    [W6]::PostMessage($h, $up, [IntPtr]::Zero, $lp) | Out-Null
  }
}
foreach ($action in ($Seq -split ',' | Where-Object { $_ })) {
  $p = $action.Trim().Split(':')
  switch ($p[0]) {
    'd' { Down ([Convert]::ToInt32($p[1], 16)) $false }
    'u' { Up ([Convert]::ToInt32($p[1], 16)) }
    'w' { Start-Sleep -Milliseconds ([int]$p[1]) }
    'h' {
      $vk = [Convert]::ToInt32($p[1], 16); $end = (Get-Date).AddMilliseconds([int]$p[2])
      Down $vk $false
      while ((Get-Date) -lt $end) { Start-Sleep -Milliseconds 100; Down $vk $true }
      Up $vk
    }
    default { throw "unknown action: $action" }
  }
}
foreach ($ch in $Text.ToCharArray()) {
  [W6]::PostMessage($h, 0x0102, [IntPtr][int]$ch, [IntPtr]1) | Out-Null       # WM_CHAR
  Start-Sleep -Milliseconds 15
}
"ok"
