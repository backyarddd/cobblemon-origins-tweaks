package dev.cobblemonorigins.tweaks.mixin.cosmetics;

import com.cobblemon.mod.common.client.battle.ClientBallDisplay;
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cobblemonorigins.tweaks.cosmetics.ClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// The send-out/recall ball: renderTransition builds a fresh ClientBallDisplay(caughtBall, emptySet()) every frame and
// passes it to drawPokeBall, which resolves the ball's model and texture variation from the display's currentAspects.
// ClientBallDisplay never copies its constructor aspects into currentAspects, so the skin aspect goes there directly.
// Cobblemon-only names: renderTransition and drawPokeBall$default are not obfuscated, hence remap = false. The INVOKE
// target leaves out the descriptor because it holds Minecraft types (PoseStack, MultiBufferSource) whose runtime names
// differ from these; drawPokeBall$default is called exactly once in renderTransition.
@Mixin(value = PokemonRenderer.class, remap = false)
public abstract class PokemonRendererMixin {
    @ModifyArg(
        method = "renderTransition",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/client/render/pokemon/PokemonRenderer;drawPokeBall$default"
        ),
        index = 1,
        require = 1
    )
    private ClientBallDisplay origins_tweaks$skinTransitionBall(
        ClientBallDisplay display,
        @Local(argsOnly = true) PokemonEntity entity
    ) {
        display.setCurrentAspects(ClientState.withBallSkin(entity.getId(), display.getCurrentAspects()));
        return display;
    }
}
