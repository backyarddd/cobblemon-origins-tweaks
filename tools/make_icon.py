"""Generate the 400x400 project icon (original art): a stylised feather over a navy disc with a gold rim.
Usage: python -I tools/make_icon.py <out.png>"""
import math
import sys

from PIL import Image, ImageDraw

SIZE, SS = 400, 4  # final size, supersampling factor
NAVY, GOLD = (19, 33, 74, 255), (226, 176, 74, 255)
VANE, VANE_SHADE = (246, 242, 230, 255), (204, 213, 236, 255)


def main(out):
    s = SIZE * SS
    img = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 6 * SS
    d.ellipse([m, m, s - m, s - m], fill=GOLD)
    rim = 20 * SS
    d.ellipse([m + rim, m + rim, s - m - rim, s - m - rim], fill=NAVY)

    # Feather axis from the quill end (lower left) to the tip (upper right), slightly curved.
    p0, p1 = (s * 0.25, s * 0.78), (s * 0.77, s * 0.21)
    ax, ay = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(ax, ay)
    ux, uy = ax / length, ay / length
    nx, ny = -uy, ux  # left-hand normal on screen

    def at(t, offset):
        bend = math.sin(math.pi * t) * length * 0.05  # gentle curve of the rachis
        return (p0[0] + ux * length * t + nx * (offset + bend), p0[1] + uy * length * t + ny * (offset + bend))

    def half_width(t, side):
        u = (t - 0.16) / 0.84
        if u <= 0:
            return 0.0
        w = math.sin(math.pi * min(u, 1.0) ** 0.75) * (0.95 if u < 0.85 else 1.0)
        return length * (0.20 if side > 0 else 0.16) * w

    ts = [0.16 + 0.84 * i / 60 for i in range(61)]
    left = [at(t, half_width(t, +1)) for t in ts]
    right = [at(t, -half_width(t, -1)) for t in ts]
    spine = [at(t, 0) for t in ts]
    d.polygon(left + spine[::-1], fill=VANE)
    d.polygon(spine + right[::-1], fill=VANE_SHADE)

    # Slanted splits in the vane, following the barbs from the shaft towards the tip.
    for t, side, depth in [(0.42, +1, 0.70), (0.60, -1, 0.65), (0.30, -1, 0.55)]:
        hw = half_width(t, side)
        inner = at(t, side * hw * (1 - depth))
        e0 = at(t + 0.050, side * half_width(t + 0.050, side) * 1.02)
        e1 = at(t + 0.085, side * half_width(t + 0.085, side) * 1.02)
        out1 = at(t + 0.085, side * hw * 1.8)
        out0 = at(t + 0.050, side * hw * 1.8)
        d.polygon([inner, e0, out0, out1, e1], fill=NAVY)

    # Rachis (quill shaft) in gold, extending past the vane at the root.
    shaft = [at(t, 0) for t in [i / 80 for i in range(81)]]
    d.line(shaft, fill=GOLD, width=9 * SS, joint='curve')
    r = 5 * SS
    d.ellipse([shaft[0][0] - r, shaft[0][1] - r, shaft[0][0] + r, shaft[0][1] + r], fill=GOLD)

    # Motion streaks trailing behind the feather, parallel to its flight.
    for off, t0, t1 in [(-0.24, 0.02, 0.30), (-0.31, 0.10, 0.34), (-0.38, 0.20, 0.40)]:
        a = (p0[0] + ux * length * t0 + nx * length * off, p0[1] + uy * length * t0 + ny * length * off)
        b = (p0[0] + ux * length * t1 + nx * length * off, p0[1] + uy * length * t1 + ny * length * off)
        d.line([a, b], fill=GOLD, width=12 * SS)
        for p in (a, b):
            d.ellipse([p[0] - 6 * SS, p[1] - 6 * SS, p[0] + 6 * SS, p[1] + 6 * SS], fill=GOLD)

    img.resize((SIZE, SIZE), Image.LANCZOS).save(out)


if __name__ == '__main__':
    main(sys.argv[1])
