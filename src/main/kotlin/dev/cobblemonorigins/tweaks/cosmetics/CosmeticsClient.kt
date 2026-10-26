package dev.cobblemonorigins.tweaks.cosmetics

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.world.entity.player.Player

/**
 * Client half of the origins_cosmetics server mod: hats, back items and Poke Ball skins. Client-only; this jar also
 * runs on the dedicated server, where the origins_cosmetics jar registers the same payload types instead.
 */
object CosmeticsClient : ClientModInitializer {
    override fun onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(WearPayload.TYPE, WearPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BallPayload.TYPE, BallPayload.CODEC)
        // Play payload handlers already run on the render thread, in packet order. Applying in place (rather than via
        // client.execute) keeps a payload ordered against the spawn and remove packets around it.
        ClientPlayNetworking.registerGlobalReceiver(WearPayload.TYPE) { payload, _ -> ClientState.applyWear(payload) }
        ClientPlayNetworking.registerGlobalReceiver(BallPayload.TYPE) { payload, _ -> ClientState.applyBall(payload) }

        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            // The local player is only unloaded to be replaced by a respawned one and keeps its wear (see levelChanged).
            val wearer = if (entity is Player && entity !is LocalPlayer) entity.uuid else null
            ClientState.forgetEntity(entity.id, wearer)
        }
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { client, _ -> ClientState.levelChanged(client.player?.uuid) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute(ClientState::clear) }

        CosmeticModels.register()
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register { _, renderer, helper, _ ->
            if (renderer is PlayerRenderer) helper.register(WearLayer(renderer))
        }
    }
}
