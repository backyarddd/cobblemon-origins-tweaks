package dev.cobblemonorigins.tweaks.mixin.cosmetics;

import com.cobblemon.mod.common.client.render.pokeball.PokeBallRenderer;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cobblemonorigins.tweaks.cosmetics.ClientState;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// A thrown ball: every frame render() copies the entity's synced aspects into its client delegate with
// setCurrentAspects, and the poser, texture and layers are then resolved from the delegate's currentAspects.
// render overrides EntityRenderer.render, so its name and descriptor are remapped (refmap); the INVOKE target is
// Cobblemon-only and is not.
@Mixin(PokeBallRenderer.class)
public abstract class PokeBallRendererMixin {
    @ModifyArg(
        method = "render(Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/client/entity/EmptyPokeBallClientDelegate;setCurrentAspects(Ljava/util/Set;)V",
            remap = false
        ),
        index = 0,
        require = 1
    )
    private Set<String> origins_tweaks$skinThrownBall(Set<String> aspects, @Local(argsOnly = true) EmptyPokeBallEntity entity) {
        return ClientState.withBallSkin(entity.getId(), aspects);
    }
}
