"""Summarise flight test segments from the cftest logger. Usage: analyze.py <latest.log> <markers.txt>
Logger lines (every 2 ticks): CFPOS <gametime>L [rider pos] ROT [yaw,pitch] MOUNT [yaw,pitch] MPOS [mount pos]."""
import math, re, sys

LINE = re.compile(r'^\[(\d\d:\d\d:\d\d)\].*CFPOS (\d+)L \[([^\]]*)\] ROT \[([^\]]*)\] MOUNT \[([^\]]*)\] MPOS \[([^\]]*)\]')
vals = lambda s: [float(v.rstrip('dfL')) for v in s.split(',')]
secs = lambda t: sum(int(v) * m for v, m in zip(t.split(':'), (3600, 60, 1)))

samples = []
for line in open(sys.argv[1], encoding='utf-8', errors='replace'):
    m = LINE.search(line)
    if m and m[6]:
        samples.append(dict(t=secs(m[1]), gt=int(m[2]), rot=vals(m[4]), myaw=vals(m[5])[0], pos=vals(m[6])))
marks = {}
for line in open(sys.argv[2]):
    t, label = line.split()
    hh, ms = t.split('.')
    marks[label] = secs(hh) + int(ms) / 1000

def window(t0, t1):  # samples logged within the wall-clock window (log timestamps have 1 s resolution)
    return [s for s in samples if math.floor(t0) <= s['t'] <= math.floor(t1)]

def hdist(a, b):
    return math.hypot(b['pos'][0] - a['pos'][0], b['pos'][2] - a['pos'][2])

def at_gt(gt):
    return min(samples, key=lambda s: abs(s['gt'] - gt))

def moving_onset(win):
    for a, b in zip(win, win[1:]):
        if hdist(a, b) > 0.02:
            return a
    return None

for label, t0 in sorted(marks.items(), key=lambda kv: kv[1]):
    if label.endswith('-end'):
        continue
    win = window(t0, marks[label + '-end'] + 0.5)
    if len(win) < 2:
        print(f'{label}: no samples'); continue
    a, b = win[0], win[-1]
    ys = [s['pos'][1] for s in win]
    secs_span = (b['gt'] - a['gt']) / 20
    line = (f'{label:13s} {secs_span:4.1f}s  mount y {a["pos"][1]:.3f} -> {b["pos"][1]:.3f} (range {min(ys):.3f}..{max(ys):.3f})'
            f'  horizontal {hdist(a, b):6.2f} blocks  rider yaw/pitch {a["rot"][0]:.1f}/{a["rot"][1]:.1f}  mount yaw {a["myaw"]:.1f}->{b["myaw"]:.1f}')
    print(line)
    if label in ('W', 'CTRLW', 'DTW', 'UPW', 'DOWNW'):
        s0 = moving_onset(win)
        if s0:
            s1 = at_gt(s0['gt'] + 60)
            th = math.radians(s0['rot'][0])
            look = math.degrees(math.atan2(math.cos(th), -math.sin(th)))
            head = math.degrees(math.atan2(s1['pos'][2] - s0['pos'][2], s1['pos'][0] - s0['pos'][0]))
            print(f'{"":13s} 3 s from onset: {hdist(s0, s1):.2f} blocks, altitude change {s1["pos"][1] - s0["pos"][1]:+.3f}, '
                  f'heading error vs look {abs((head - look + 180) % 360 - 180):.1f} deg')
    if label in ('EXHAUST', 'NORMAL10'):
        s1 = win[-1]; s0 = at_gt(s1['gt'] - 60)
        print(f'{"":13s} last 3 s: {hdist(s0, s1) / 3:.2f} blocks/s, altitude change {s1["pos"][1] - s0["pos"][1]:+.3f}')
    if label in ('BRAKE', 'SETTLE', 'SETTLE2'):  # from the last full-speed sample before release to standstill
        prev = {'BRAKE': 'W', 'SETTLE': 'SPACE', 'SETTLE2': 'SHIFT'}[label]
        both = window(marks[prev], marks[label + '-end'])
        step = lambda x, y: math.dist(x['pos'], y['pos']) / max(1, y['gt'] - x['gt'])
        pairs = list(zip(both, both[1:]))
        top = max((step(x, y) for x, y in pairs), default=0.0)
        if top <= 0.0:
            print(f'{"":13s} no motion in this window'); continue
        last_full = max(i for i, (x, y) in enumerate(pairs) if step(x, y) > 0.98 * top)
        stop = next((i for i in range(last_full, len(pairs)) if step(*pairs[i]) < 1e-6), None)
        took = (pairs[stop][0]['gt'] - pairs[last_full][1]['gt']) / 20 if stop is not None else float('nan')
        print(f'{"":13s} after release: standstill within {took:.2f} s (+-0.1 s sampling), then holds')
