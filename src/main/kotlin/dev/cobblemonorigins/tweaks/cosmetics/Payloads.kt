package dev.cobblemonorigins.tweaks.cosmetics

import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.UUID

// Server -> client payloads of the origins_cosmetics server mod. Ids and codecs must stay byte-identical to the
// server's copy (specs/origins-cosmetics/client-contract.md); PayloadsTest pins the wire format.
private const val NAMESPACE = "origins_cosmetics"

/** Everything one wearer shows (full state, at most [MAX_PARTS]); an empty list means nothing worn. */
data class WearPayload(val player: UUID, val parts: List<Part>) : CustomPacketPayload {
    /** Model `origins:cosmetic/<model>` drawn on [bone], "head" or "body"; other bone values are ignored. */
    data class Part(val model: String, val bone: String) {
        companion object {
            val CODEC: StreamCodec<ByteBuf, Part> = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Part::model,
                ByteBufCodecs.STRING_UTF8, Part::bone,
                ::Part,
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<WearPayload> = TYPE

    companion object {
        const val MAX_PARTS = 8

        val TYPE: CustomPacketPayload.Type<WearPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "wear"))

        val CODEC: StreamCodec<ByteBuf, WearPayload> = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, WearPayload::player,
            Part.CODEC.apply(ByteBufCodecs.list(MAX_PARTS)), WearPayload::parts,
            ::WearPayload,
        )
    }
}

/** Ball skin for the PokemonEntity or EmptyPokeBallEntity with network id [entity]; an empty [skin] clears it. */
data class BallPayload(val entity: Int, val skin: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<BallPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<BallPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "ball"))

        val CODEC: StreamCodec<ByteBuf, BallPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BallPayload::entity,
            ByteBufCodecs.STRING_UTF8, BallPayload::skin,
            ::BallPayload,
        )
    }
}
