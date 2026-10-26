"""Ride (stamina) bar fill fraction from a 1600x900 GUI-scale-2 screenshot. Usage: barfill.py <png>..."""
import sys
from PIL import Image

for path in sys.argv[1:]:
    im = Image.open(path).convert('RGB')
    row = [im.getpixel((x, 846)) for x in range(612, 990)]
    filled = sum(1 for r, g, b in row if r > 150 and g > 100)          # gold fill
    empty = sum(1 for r, g, b in row if b > r and b > 40 and r < 60)     # dark blue track
    print(f'{path}: stamina bar {filled / max(1, filled + empty):.2f} ({filled} filled px, {empty} empty px)')
