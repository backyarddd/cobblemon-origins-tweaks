# Changelog

## 1.0.3+1.21.1

- Removed the **Mewtwo DNA** item (`cobblemon_origins_tweaks:mewtwo_dna`), its icon and its lang lines. The Cobblemon Origins modpack now revives Mewtwo from LumyMon's Ancient DNA and Cloning Catalyst instead. Mewtwo DNA still in a world is lost when it loads (unknown item), and datapack files that name the item fail to load.
- Install the same version on the server and on every client, as before.
- Still requires exactly Cobblemon 1.8.2. Classic flight, the lean, the Battle Extras fix and cosmetics support are unchanged.

## 1.0.2+1.21.1

- New item: **Mewtwo DNA** (`cobblemon_origins_tweaks:mewtwo_dna`). Epic rarity, stacks to 16, animated 32x32 icon (a stasis vial lying diagonally: the DNA helix turns and the liquid glows), listed in Cobblemon's Archaeology creative tab after the fossils. The mod only adds the item: the Cobblemon Origins modpack's datapack gives it for beating the last Gym Leader of each Radical Cobblemon Trainers series and makes it, together with a Nether Star, a Restoration Tank fossil that revives Mewtwo.
- Server and clients must update together: a client on 1.0.1 cannot join a server on 1.0.2 (the server has an item the client does not know).
- Still requires exactly Cobblemon 1.8.2. Classic flight, the lean, the Battle Extras fix and cosmetics support are unchanged.

## 1.0.1+1.21.1

- Updated for Cobblemon 1.8.2. This version requires exactly Cobblemon 1.8.2; stay on 1.0.0 with Cobblemon 1.8.1.
- No gameplay changes: classic flight, the lean, the Battle Extras fix and cosmetics support work as in 1.0.0.

## 1.0.0+1.21.1 (initial release)

Initial release of Cobblemon Origins Tweaks, which replaces Cobblemon Classic Flight 1.0.1. The mod id is `cobblemon_origins_tweaks`. Install the same version on the server and on every client: its classic flight riding key (`cobblemon_origins_tweaks:air/classic`) does not match Classic Flight's, so remove Cobblemon Classic Flight when you install this.

- Classic flight (from Cobblemon Classic Flight 1.0.1): bird and jet flyers fly with WASD relative to the camera, Space ascends, Shift descends, no roll, with sprint and stamina.
- New (client): flying Pokémon lean slightly into sideways movement (A/D), up to 12°, and the rider leans with them. Purely visual: the camera, controls and flight path are unchanged, and other players see the lean too.
- New (client): fixes `Unknown MovesetBuilder id: cobblemon:wild` on multiplayer servers. Cobblemon Battle Extras move tooltips (type effectiveness, damage range) no longer disappear against some opponents, and the log no longer fills with that stack trace while you hover a move.
- New (client): support for Cobblemon Origins server cosmetics (worn items such as hats, and Poké Ball skins). It does nothing unless the server sends them.

## Cobblemon Classic Flight (previous project)

### 1.0.1+1.21.1

- Players with the mod can now join servers that do not have it, and get Cobblemon's normal flight there.
- Sprint now lasts 6 to 8 seconds on a full bar for every flyer (it could be a single tick or about 3 seconds for some Pokémon).
- Stamina now only drains from the extra sprint speed; speeding up to normal speed is free.
- Fixed stamina breaking on datapacks with zero or invalid stamina values, and an empty saved bar locking sprint when a datapack makes stamina infinite.
- Double-tapping W before taking off or after landing no longer starts a sprint on your next W press in the air.

### 1.0.0+1.21.1 (initial release)

- Classic flight replaces Cobblemon's roll-based `bird` and `jet` flight for every species, including Mega Showdown flyers.
- WASD flies level relative to the camera, Space ascends, Shift descends, no roll, normal mouse look; the Pokémon turns to face where you look.
- No drift: altitude holds exactly without Space/Shift, and releasing the keys brakes to a hover quickly.
- Top speed follows the AIR SPEED ride stat with Cobblemon's bird mapping; vertical speed is half of top speed; no altitude ceiling.
- Sprint (sprint key + W, or double-tap W): 50% faster forward flight with a sprint FOV effect.
- Stamina is only used by sprinting; normal flight is free. An empty bar locks sprint until it refills (about 2 s). Respects `infiniteRideStamina`.
- Take-off (double-tap Space) and landing work as in Cobblemon.
- Hover pose when idle in the air, flying pose when moving.
- Summary screen shows a "Classic" ride style with its icon.
