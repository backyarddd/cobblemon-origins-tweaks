package dev.cobblemonorigins.tweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cobblemonorigins.tweaks.flight.FlightLean;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Right after render's only pushPose the stack holds just the entity's render translation: before the renderer's scale,
// before setupRotations (where Cobblemon moves a rider onto its seat) and before the model. A lean applied here moves a
// classic-flight mount and its rider as one body, and the nametag (drawn after popPose) stays upright.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void origins_tweaks$flightLean(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        FlightLean.apply(entity, partialTick, poseStack);
    }
}
