# Cosmetics (client half of origins_cosmetics)

Client-only code that receives the origins_cosmetics server mod's payloads and draws hats, back items and Poke Ball
skins. Contract (ids, codecs, model space, aspect names):
`packdev/server/specs/origins-cosmetics/client-contract.md`.

Reachable only from the `client` entrypoint `CosmeticsClient` and the client-only mixin config
`cobblemon_origins_tweaks.cosmetics.mixins.json`. The jar also runs on the dedicated server, where the separate
origins_cosmetics jar registers the payload types; registering them here outside the client initializer would
register them twice and crash.

| file | role |
|---|---|
| `Payloads.kt` | `origins_cosmetics:wear` v2 (UUID player, then `Part.CODEC.apply(ByteBufCodecs.list(8))`: VarInt count <= 8, each part String model + String bone) and `origins_cosmetics:ball` (VarInt entity, String skin) |
| `ClientState.kt` | worn parts by player UUID (an empty list removes the entry), ball skin by entity network id; cleared on entity unload, level change, disconnect |
| `CosmeticModels.kt` | extra models `origins:cosmetic/<id>` for every `assets/origins/models/cosmetic/<id>.json`, rescanned each reload |
| `WearLayer.kt` | player render layer: every part on its bone (`head` or `body`; other values skipped, logged once each), in the contract's model space |
| `CosmeticsClient.kt` | registration of all of the above |

Clearing: the local player's own wear survives a level change and its own respawn, because the server sends it only on
join; everything else is resent by the server when this client starts tracking the entity again.

## Resource pack requirements

- Models: `assets/origins/models/cosmetic/<id>.json`, vanilla block/item model JSON; `display` is ignored.
- Model textures are baked from the block atlas, which only stitches the directories listed in
  `assets/minecraft/atlases/blocks.json`. Textures under `assets/origins/textures/cosmetic/` need the pack to add
  `{"sources": [{"type": "directory", "source": "cosmetic", "prefix": "cosmetic/"}]}` to that file (atlas sources from
  all packs are merged), and models reference them as `origins:cosmetic/<texture>`. Without it they render as the
  missing texture.
- Model space (pixels): (8, 0, 8) = the bone pivot (the neck for both bones; front items sit at z <= 6 and back items at z >= 10 on the body), +x = toward the wearer's right, +y = up,
  +z = toward the back, i.e. Blockbench with the wearer's front facing north. The model appears in game as it does in
  Blockbench, textures included; back faces are culled as usual.

## Ball skin call sites (Cobblemon 1.8.1, verified with javap on both the named and the production jar)

Ball model, texture and layers are resolved by `VaryingModelRepository.getPoser/getTexture/getLayers(name, state)`,
which match `ModelAssetVariation.aspects` against `PosableState.getCurrentAspects()` only. Both mixins therefore add
`origins_ball_<skin>` to the state's current aspects.

1. Send-out / recall ball, `PokemonRendererMixin` (`remap = false`):
   - owner `com/cobblemon/mod/common/client/render/pokemon/PokemonRenderer`
   - method `renderTransition(Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableModel;ILcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/cobblemon/mod/common/client/entity/PokemonClientDelegate;)V`
   - call `INVOKESTATIC PokemonRenderer.drawPokeBall$default(Lcom/cobblemon/mod/common/client/render/pokemon/PokemonRenderer;Lcom/cobblemon/mod/common/client/battle/ClientBallDisplay;Lcom/mojang/blaze3d/vertex/PoseStack;FFZLnet/minecraft/client/renderer/MultiBufferSource;ILcom/cobblemon/mod/common/pokeball/PokeBall;IILjava/lang/Object;)V`
     (offset 994, the only call), preceded at offset 806 by `new ClientBallDisplay(entity.getPokemon().getCaughtBall(), emptySet())`
   - injector `@ModifyArg(index = 1)` on that call, targeted by owner + name only (its descriptor holds Minecraft
     types that differ between dev and production); sets `currentAspects` of the per-frame `ClientBallDisplay`, because
     `ClientBallDisplay` never copies its constructor aspects into `currentAspects`
2. Thrown ball, `PokeBallRendererMixin` (method remapped through the refmap, call not):
   - owner `com/cobblemon/mod/common/client/render/pokeball/PokeBallRenderer`
   - method `render(Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V`
     (production: same name, descriptor `(...EmptyPokeBallEntity;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V`)
   - call `INVOKEVIRTUAL com/cobblemon/mod/common/client/entity/EmptyPokeBallClientDelegate.setCurrentAspects(Ljava/util/Set;)V`
     (offset 79, the only call; its argument is `entity.getAspects()`)
   - injector `@ModifyArg(index = 0)` on that call

Both injectors use `require = 1` and MixinExtras `@Local(argsOnly = true)` for the entity, whose network id keys the
ball map.
