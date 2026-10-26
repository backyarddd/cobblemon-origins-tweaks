package dev.cobblemonorigins.tweaks.mixin;

import com.cobblemon.mod.common.CobblemonMovesetBuilders;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Moveset builders are server data that Cobblemon never syncs, so on a client connected to a dedicated server the
// registry is empty and initializeMovesetFromDefault throws "Unknown MovesetBuilder id: cobblemon:wild" for every
// Pokemon a client mod builds itself (PokemonProperties.create()). Cobblemon Battle Extras does that for its move
// tooltips, which then silently disappear. With no builders loaded, do what Cobblemon already does for client-side
// Pokemon: a one-move placeholder set. In singleplayer the integrated server fills the registry, so nothing changes.
@Mixin(value = Pokemon.class, remap = false)
public abstract class PokemonMixin {
    @Inject(method = "initializeMovesetFromDefault", at = @At("HEAD"), cancellable = true)
    private void origins_tweaks$placeholderWithoutBuilders(CallbackInfo ci) {
        if (!CobblemonMovesetBuilders.INSTANCE.getMovesetBuilders().isEmpty()) return;
        MoveSet placeholder = new MoveSet();
        placeholder.add(Moves.getExceptional().create());
        ((Pokemon) (Object) this).getMoveSet().copyFrom(placeholder);
        ci.cancel();
    }
}
