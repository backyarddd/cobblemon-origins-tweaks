# Cobblemon Origins Tweaks

Cobblemon tweaks from the Cobblemon Origins modpack: classic flight for your flying Pokémon and a fix that keeps client mods such as Cobblemon Battle Extras working on multiplayer servers.

Replaces **Cobblemon Classic Flight** (remove that mod when you install this one). Made for the [Cobblemon Origins - Road to Champion](https://www.curseforge.com/minecraft/modpacks/cobblemon-origins-road-to-champion) modpack, and works in any Cobblemon 1.8.2 setup.

## Classic flight

Cobblemon 1.8 flies bird and jet mounts with roll-based controls: the mouse rolls and pitches your Pokémon like a plane. This mod replaces that with classic flight for every player: your Pokémon goes where you look, Space takes you up, Shift takes you down, and the camera never rolls.

### Controls

| Input | Action |
| --- | --- |
| W / A / S / D | Fly forward, left, back, right, level and relative to where you are looking |
| Space | Ascend |
| Shift (sneak) | Descend |
| Sprint key (Left Ctrl) + W, or double-tap W | Sprint: 50% faster forward flight, wider FOV, uses stamina |
| Mouse | Normal mouse look. Your Pokémon turns to face where you look |
| Double-tap Space on the ground | Take off: your Pokémon lifts off the ground, hold Space to keep climbing |
| Fly down onto the ground | Land and switch back to walking (same as Cobblemon) |

- Only Space and Shift change altitude. Looking up or down while pressing W keeps you level.
- No drift: with no keys held your Pokémon hovers in place and holds its altitude exactly. Letting go of the keys brakes to a hover within a fraction of a second.
- No roll and no pitch: the horizon stays flat. When you strafe with A or D your Pokémon leans slightly into the movement (up to 12°) and you lean with it. This is only visual: the camera and the flight path never tilt.
- Climbing and descending run at half of the Pokémon's top speed. No altitude ceiling.
- Idle in the air plays the hover animation, moving plays the flying animation.

### Stamina and sprint

- Normal flight is free: it never uses stamina and never forces you down.
- Sprinting only pays for the extra speed above normal flight: speeding up to normal speed is free, and a full bar lasts 6 to 8 seconds of full sprint depending on the Pokémon's AIR STAMINA stat (about 7 seconds for a Charizard with base ride stats).
- When the bar runs empty the sprint stops and you keep flying at normal speed. Sprinting unlocks again once the bar has refilled, which takes about 2 seconds of not sprinting.
- The sprint FOV effect follows Minecraft's "FOV Effects" accessibility slider (0% turns it off).
- The server option `infiniteRideStamina` gives unlimited sprint.

### What changes

- Every species that uses Cobblemon's `bird` or `jet` flight style now uses classic flight. This covers Cobblemon's own flyers (Charizard, Pidgeot, Dragonite, Togekiss, the legendary birds and more), Mega Showdown's flyers, and any other datapack or addon that uses those two styles. No data is edited.
- Top speed comes from each Pokémon's AIR SPEED ride stat, mapped exactly like Cobblemon's bird flight (4 to 20 blocks per second), so flyers stay about as fast as before. A Charizard with base ride stats flies at 8.8 blocks per second (13.2 sprinting), up to 14.4 (21.6) when its SPEED is fully boosted.
- Acceleration and turning use Cobblemon's hover settings.
- All other riding styles (hover, rocket, glider, land and water mounts) are untouched.

## Battle Extras tooltip fix (client)

On a multiplayer server, Cobblemon never sends its moveset builder data to clients. Any client mod that builds a Pokémon on the client then fails with `Unknown MovesetBuilder id: cobblemon:wild`. Cobblemon Battle Extras does this for its move tooltips, so against many opponents the tooltip (type effectiveness, damage range) silently disappears and the log fills with stack traces every frame. This mod gives those client-built Pokémon a placeholder move set instead, the same way Cobblemon already treats Pokémon it receives from the server. Nothing changes in single player or on the server.

## Requirements

- Minecraft 1.21.1, Fabric Loader 0.16 or newer
- Cobblemon 1.8.2 (later patches need a matching update: the client render hooks target 1.8.2 exactly; version 1.0.0 of this mod is for Cobblemon 1.8.1)
- Fabric Language Kotlin (Cobblemon already needs it)
- Fabric API 0.116.17 or newer

**Install it on the server and on every client that plays there.** The server turns bird and jet flight into classic flight and sends that to its players, so a client without the mod cannot join a server that has it, and the server and its clients must run the same version of this mod. A client with the mod can still join servers without it and gets Cobblemon's normal flight there. For single player, installing it on the client is enough.

## Compatibility

- Mega Showdown: its bird and jet flyers (Mega Charizard X and Y, Rayquaza, Reshiram, Zekrom, Yveltal, Lunala, Koraidon, Miraidon, Kyurem forms, the Forces of Nature and others) fly with classic controls too.
- Works with any species data that uses `cobblemon:air/bird` or `cobblemon:air/jet`.

## Known limitations

- Jet species use the bird speed and stamina ranges, so their maximum top speed (before sprinting) is 20 blocks per second instead of the jet style's 24.
- Bird gliding, dive speed boosts and wall-collision damage are gone with the roll controls.
- It is all or nothing: every bird and jet flyer uses classic flight for every player.
- Cobblemon's on-screen ride hints (when enabled in Cobblemon's config) show the generic layout for classic flight: mouse, WASD, Space and Shift.

## Planned

- A per-player toggle to choose between classic flight and Cobblemon's roll flight.

## License

MIT. Made by Cobblemon Origins. Not affiliated with the Cobblemon team, Nintendo, Game Freak or The Pokémon Company.
