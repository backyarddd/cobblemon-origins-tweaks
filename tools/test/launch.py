"""Launch the CurseForge-installed Fabric 1.21.1 client offline against a test game dir.
Copy of packdev/menu/test/launch.py with extra Minecraft arguments passed through (e.g. --quickPlayMultiplayer
localhost:25570) and the heap size taken from MC_XMX (default 6G).
Usage: launch.py <game_dir> <width> <height> [extra minecraft args...]"""
import json, os, subprocess, sys

INSTALL = os.environ.get('CURSEFORGE_INSTALL', os.path.expanduser(r'~\curseforge\minecraft\Install'))
JAVA = os.path.join(INSTALL, 'java', 'Jre_21', 'bin', 'javaw.exe')
game_dir, width, height, extra = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]

def ver(name):
    return json.load(open(os.path.join(INSTALL, 'versions', name, name + '.json'), encoding='utf-8'))

def allowed(lib):
    ok = True
    for r in lib.get('rules', []):
        os_ok = 'os' not in r or r['os'].get('name') == 'windows'
        if os_ok:
            ok = r['action'] == 'allow'
        elif r['action'] == 'allow':
            ok = False
    return ok

def maven_path(name):
    g, a, v = name.split(':')[:3]
    return os.path.join(g.replace('.', os.sep), a, v, f'{a}-{v}.jar')

fabric, vanilla = ver('fabric-loader-0.19.5-1.21.1'), ver('1.21.1')
cp, seen = [], set()
for lib in fabric['libraries']:
    ga = ':'.join(lib['name'].split(':')[:2])
    seen.add(ga)
    cp.append(os.path.join(INSTALL, 'libraries', maven_path(lib['name'])))
for lib in vanilla['libraries']:
    if not allowed(lib):
        continue
    ga = ':'.join(lib['name'].split(':')[:2])
    if ga in seen and 'natives' not in lib['name']:
        continue  # fabric ships a newer asm etc.
    cp.append(os.path.join(INSTALL, 'libraries', lib['downloads']['artifact']['path'].replace('/', os.sep)))
cp.append(os.path.join(INSTALL, 'versions', '1.21.1', '1.21.1.jar'))
missing = [p for p in cp if not os.path.exists(p)]
if missing:
    sys.exit(f'missing libraries: {missing[:5]}')

cmd = [JAVA, '-Xmx' + os.environ.get('MC_XMX', '6G'), '-DFabricMcEmu= net.minecraft.client.main.Main ', '-cp', os.pathsep.join(cp), fabric['mainClass'],
       '--username', 'Trainer', '--version', 'fabric-loader-0.19.5-1.21.1', '--gameDir', game_dir,
       '--assetsDir', os.path.join(INSTALL, 'assets'), '--assetIndex', vanilla['assetIndex']['id'],
       '--uuid', '00000000000000000000000000000000', '--accessToken', '0', '--userType', 'legacy', '--versionType', 'release',
       '--width', width, '--height', height] + extra
p = subprocess.Popen(cmd, cwd=game_dir, stdout=open(os.path.join(game_dir, 'stdout.txt'), 'w'), stderr=subprocess.STDOUT)
print(p.pid)
