package dev.cobblemonorigins.tweaks.cosmetics

import java.util.UUID

/**
 * What the server told this client about cosmetics. Touched only on the render thread (payload handlers, entity and
 * connection events, rendering), so plain maps suffice. Kept free of Minecraft types so it can be unit tested.
 */
object ClientState {
    /** Prefix of the aspect Cobblemon ball variations in the server pack are keyed on. */
    const val BALL_ASPECT_PREFIX = "origins_ball_"

    private val wear = HashMap<UUID, List<WearPayload.Part>>()
    private val balls = HashMap<Int, String>()

    /**
     * Replaces the wearer's parts; an empty list removes the entry. Stored even for a wearer not loaded yet: the
     * server sends it before or right after the spawn packet.
     */
    fun applyWear(payload: WearPayload) {
        if (payload.parts.isEmpty()) wear.remove(payload.player) else wear[payload.player] = payload.parts
    }

    /** Stored by network id until that entity unloads; the server sends it before the entity's spawn packet. */
    fun applyBall(payload: BallPayload) {
        if (payload.skin.isEmpty()) balls.remove(payload.entity) else balls[payload.entity] = payload.skin
    }

    /** The parts [player] wears, never empty, or null if nothing. */
    fun wear(player: UUID): List<WearPayload.Part>? = wear[player]

    /** [aspects] plus `origins_ball_<skin>` if the entity has a ball skin, else [aspects] itself. */
    @JvmStatic
    fun withBallSkin(entityId: Int, aspects: Set<String>): Set<String> {
        val skin = balls[entityId] ?: return aspects
        return aspects + (BALL_ASPECT_PREFIX + skin)
    }

    /** An entity left the client level; its network id may be reused for a different entity later. */
    fun forgetEntity(entityId: Int, player: UUID?) {
        balls.remove(entityId)
        if (player != null) wear.remove(player)
    }

    /**
     * The client level was replaced. Every entity id is stale. Other players' wear is resent when this client starts
     * tracking them in the new level, but the server sends the local player's own wear only on join, so it is kept.
     */
    fun levelChanged(localPlayer: UUID?) {
        balls.clear()
        wear.keys.retainAll { it == localPlayer }
    }

    fun clear() {
        balls.clear()
        wear.clear()
    }
}
