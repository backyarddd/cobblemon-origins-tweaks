package dev.cobblemonorigins.tweaks.cosmetics

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PayloadsTest {
    private val player = UUID.fromString("0f3c6a1e-9b2d-4c55-8e71-2a4b6c8d0e1f")

    private fun <T : Any> roundTrip(codec: StreamCodec<ByteBuf, T>, value: T): T {
        val buffer = Unpooled.buffer()
        codec.encode(buffer, value)
        val decoded = codec.decode(buffer)
        assertEquals(0, buffer.readableBytes(), "decode must consume exactly what encode wrote")
        return decoded
    }

    private fun <T : Any> encode(codec: StreamCodec<ByteBuf, T>, value: T): String =
        ByteBufUtil.hexDump(Unpooled.buffer().also { codec.encode(it, value) })

    /** The contract's wire format written with vanilla's own primitives, independent of the codecs under test. */
    private fun expected(write: FriendlyByteBuf.() -> Unit): String =
        ByteBufUtil.hexDump(FriendlyByteBuf(Unpooled.buffer()).apply(write))

    @Test
    fun `payload ids match the contract`() {
        assertEquals("origins_cosmetics:wear", WearPayload.TYPE.id().toString())
        assertEquals("origins_cosmetics:ball", BallPayload.TYPE.id().toString())
    }

    private fun parts(count: Int): List<WearPayload.Part> =
        List(count) { WearPayload.Part("part_$it", if (it % 2 == 0) "head" else "body") }

    @Test
    fun `wear with no parts round-trips`() {
        val payload = WearPayload(player, emptyList())
        assertEquals(payload, roundTrip(WearPayload.CODEC, payload))
    }

    @Test
    fun `wear with one part round-trips`() {
        val payload = WearPayload(player, listOf(WearPayload.Part("straw_hat", "head")))
        assertEquals(payload, roundTrip(WearPayload.CODEC, payload))
    }

    @Test
    fun `wear with the maximum of 8 parts round-trips`() {
        val payload = WearPayload(player, parts(8))
        assertEquals(payload, roundTrip(WearPayload.CODEC, payload))
    }

    @Test
    fun `wear is UUID, VarInt count, then model and bone of each part`() = assertEquals(
        expected { writeUUID(player); writeVarInt(1); writeUtf("straw_hat"); writeUtf("head") },
        encode(WearPayload.CODEC, WearPayload(player, listOf(WearPayload.Part("straw_hat", "head")))),
    )

    @Test
    fun `wear with 9 parts fails to encode`() {
        assertFailsWith<EncoderException> { encode(WearPayload.CODEC, WearPayload(player, parts(9))) }
    }

    @Test
    fun `wear with 9 parts fails to decode`() {
        // Hand-written, since the codec refuses to write it.
        val buffer = FriendlyByteBuf(Unpooled.buffer()).apply {
            writeUUID(player)
            writeVarInt(9)
            parts(9).forEach { writeUtf(it.model); writeUtf(it.bone) }
        }
        assertFailsWith<DecoderException> { WearPayload.CODEC.decode(buffer) }
    }

    @Test
    fun `ball round-trips`() {
        val payload = BallPayload(123_456, "galaxy")
        assertEquals(payload, roundTrip(BallPayload.CODEC, payload))
    }

    @Test
    fun `ball is VarInt then UTF-8 string`() = assertEquals(
        expected { writeVarInt(300); writeUtf("galaxy") },
        encode(BallPayload.CODEC, BallPayload(300, "galaxy")),
    )
}
