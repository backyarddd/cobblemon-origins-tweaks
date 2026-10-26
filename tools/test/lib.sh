# Helpers for the in-game flight tests. Source with T (game dir), SHOTS (screenshot dir) and pid set.
# Input goes through PostMessage (post2.ps1) and captures through PrintWindow (grab.ps1): no focus or cursor changes.
TOOLS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRAB="$(cygpath -w "$TOOLS/grab2.ps1")"
P() { MSYS_NO_PATHCONV=1 powershell -NoProfile -File "$(cygpath -w "$TOOLS/post2.ps1")" -ProcId "$pid" "$@" >/dev/null; }
G() { powershell -NoProfile -File "$GRAB" -ProcId "$pid" -Out "$(cygpath -w "$SHOTS/$1.png")" >/dev/null; }
alive() { tasklist //FI "PID eq $pid" | grep -q "$pid"; }
wait_log() {  # wait_log <regex> [timeout_s]
  local end=$((SECONDS + ${2:-600}))
  while ((SECONDS < end)); do
    grep -qaE "$1|has crashed" "$T/logs/latest.log" 2>/dev/null && return 0
    alive || { echo "game exited early"; tail -30 "$T/logs/latest.log"; return 1; }
    sleep 3
  done
  echo "timed out waiting for: $1"; return 1
}
cmd() {  # chat command; T opens chat, backspace removes a stray 't'
  P -Seq "d:0x54,w:60,u:0x54"; sleep 1
  P -Seq "d:0x08,w:40,u:0x08"; P -Text "$1"; sleep 0.3
  P -Seq "d:0x0D,w:40,u:0x0D"; sleep 0.8
}
data() {  # data <entity selector> <path>: prints the value from the chat feedback in latest.log
  local before; before=$(grep -ac "has the following entity data" "$T/logs/latest.log")
  cmd "/data get entity $1 $2"
  for _ in 1 2 3 4 5; do
    (( $(grep -ac "has the following entity data" "$T/logs/latest.log") > before )) && break; sleep 0.5
  done
  grep -a "has the following entity data" "$T/logs/latest.log" | tail -1 | sed 's/.*entity data: //'
}
