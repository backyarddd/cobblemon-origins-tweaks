# In-flight test sequence for a mounted flyer (rider already mounted on the ground, cftest logger running).
# Usage: flight_seq.sh <env.sh> <prefix>. Writes wall-clock action markers to $R/logs/<prefix>_markers.txt.
source "$1"; PFX=$2; M=$R/logs/${PFX}_markers.txt; : > "$M"
mark() { echo "$(date +%T.%3N) $1" >> "$M"; }
seg() {  # seg <label> <post2 seq> [screenshot delay s]: one marked input segment
  mark "$1"
  if [ -n "$3" ]; then P -Seq "$2" & sleep "$3"; G "${PFX}_$1"; wait; else P -Seq "$2"; fi
  mark "$1-end"
}
idle() { mark "$1"; sleep "$2"; G "${PFX}_$1"; mark "$1-end"; }

seg TAKEOFF "d:0x20,w:80,u:0x20,w:150,h:0x20:1500"
idle IDLE 6
seg W "h:0x57:3000" 1.5
idle BRAKE 3
seg SPACE "h:0x20:2000"
idle SETTLE 3
seg SHIFT "h:${SNEAK_VK:-0x10}:2000"
idle SETTLE2 3
seg A "h:0x41:2000"
idle REST1 2
seg D "h:0x44:2000"
idle REST2 3
seg CTRLW "d:0x11,h:0x57:3000,u:0x11" 1.5
idle REFILL1 4
seg DTW "d:0x57,w:80,u:0x57,w:120,h:0x57:3000" 1.5
idle REFILL2 4
seg NORMAL10 "h:0x57:10000" 9.5
idle REST3 3
seg EXHAUST "d:0x11,h:0x57:12000,u:0x11" 10.5
idle AFTEREXHAUST 1
seg LAND "h:${SNEAK_VK:-0x10}:7000"
idle LANDED 3
echo done
