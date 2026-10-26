package dev.cobblemonorigins.tweaks.cosmetics

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.inventory.InventoryMenu
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Draws every worn part (hat, back, front, ...) of every rendered player on the part's bone. */
class WearLayer(
    parent: RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
) : RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(parent) {
    override fun render(
        poseStack: PoseStack,
        buffers: MultiBufferSource,
        packedLight: Int,
        player: AbstractClientPlayer,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        if (player.isInvisible) return
        val parts = ClientState.wear(player.uuid) ?: return
        for (part in parts) {
            val bone = when (part.bone) {
                "head" -> parentModel.head
                "body" -> parentModel.body
                else -> {
                    if (reportedBones.add(part.bone)) {
                        LOGGER.warn("Cosmetics: unknown bone '{}' for model {}, not drawing it", part.bone, part.model)
                    }
                    continue
                }
            }
            draw(part.model, bone, poseStack, buffers, packedLight)
        }
    }

    // Slots need no code of their own: a front item sits at z <= 6 and a back item at z >= 10 of the body's model space.
    private fun draw(id: String, bone: ModelPart, poseStack: PoseStack, buffers: MultiBufferSource, packedLight: Int) {
        // A hidden bone (the body of a spectator) hides what is attached to it.
        if (!bone.visible) return
        val model = CosmeticModels.get(id) ?: return
        poseStack.pushPose()
        // Now in the bone's space: origin at its pivot, 1 unit = 1 block, +x = the player's left, +y = down,
        // +z = toward the player's back (ModelPart space; the head cube is -4..4, -8..0, -4..4 pixels).
        bone.translateAndRotate(poseStack)
        // Contract model space, in pixels p = 16 * v for a baked vertex v: (8, 0, 8) is the pivot, +x = the wearer's
        // right, +y = up, +z = back (Blockbench with the front facing north). So bone = (0.5 - v.x, -v.y, v.z - 0.5):
        // translate first, then turn 180 degrees about z, which flips x and y together. A rotation, not a mirror, so
        // winding and normals stay correct. That maps the head cube x 4..12, y 0..8, z 4..12 onto -4..4, -8..0, -4..4
        // and the torso (x 4..12, y -12..0, z 6..10) onto -4..4, 0..12, -2..2.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        poseStack.translate(-0.5f, 0f, -0.5f)
        // entityTranslucentCull culls back faces, draws cutout and translucent texels alike, and baked JSON models use
        // the block atlas.
        Minecraft.getInstance().blockRenderer.modelRenderer.renderModel(
            poseStack.last(),
            buffers.getBuffer(RENDER_TYPE),
            null,
            model,
            1f,
            1f,
            1f,
            packedLight,
            OverlayTexture.NO_OVERLAY,
        )
        poseStack.popPose()
    }

    private companion object {
        val RENDER_TYPE: RenderType = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)

        val LOGGER: Logger = LoggerFactory.getLogger("Cobblemon Origins Tweaks")

        /** Bone values already reported as unknown; touched only on the render thread. */
        val reportedBones = HashSet<String>()
    }
}
