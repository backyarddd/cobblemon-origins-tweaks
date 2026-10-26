package dev.cobblemonorigins.tweaks.cosmetics

import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ClientStateTest {
    private val local = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val hat = WearPayload.Part("straw_hat", "head")
    private val wings = WearPayload.Part("wings", "body")
    private val crown = WearPayload.Part("crown", "head")

    @AfterTest
    fun reset() = ClientState.clear()

    @Test
    fun `wear for a player not loaded yet is stored`() {
        ClientState.applyWear(WearPayload(other, listOf(hat)))
        assertEquals(listOf(hat), ClientState.wear(other))
    }

    @Test
    fun `wear replaces the previous parts`() {
        ClientState.applyWear(WearPayload(other, listOf(hat, wings)))
        ClientState.applyWear(WearPayload(other, listOf(wings)))
        assertEquals(listOf(wings), ClientState.wear(other))
    }

    @Test
    fun `wear with no parts removes the entry`() {
        ClientState.applyWear(WearPayload(other, listOf(hat)))
        ClientState.applyWear(WearPayload(other, emptyList()))
        assertNull(ClientState.wear(other))
    }

    @Test
    fun `ball skin adds the aspect and keeps the others`() {
        ClientState.applyBall(BallPayload(7, "galaxy"))
        assertEquals(setOf("shiny", "origins_ball_galaxy"), ClientState.withBallSkin(7, setOf("shiny")))
    }

    @Test
    fun `entity without a ball skin keeps its aspects unchanged`() {
        val aspects = setOf("shiny")
        assertSame(aspects, ClientState.withBallSkin(8, aspects))
    }

    @Test
    fun `empty ball skin clears it`() {
        ClientState.applyBall(BallPayload(7, "galaxy"))
        ClientState.applyBall(BallPayload(7, ""))
        assertEquals(emptySet(), ClientState.withBallSkin(7, emptySet()))
    }

    @Test
    fun `unloading an entity drops its ball skin so a reused id starts clean`() {
        ClientState.applyBall(BallPayload(7, "galaxy"))
        ClientState.forgetEntity(7, null)
        assertEquals(emptySet(), ClientState.withBallSkin(7, emptySet()))
    }

    @Test
    fun `unloading a player drops their wear`() {
        ClientState.applyWear(WearPayload(other, listOf(hat)))
        ClientState.forgetEntity(2, other)
        assertNull(ClientState.wear(other))
    }

    @Test
    fun `level change drops balls and other players but keeps the local player's wear`() {
        ClientState.applyWear(WearPayload(local, listOf(crown)))
        ClientState.applyWear(WearPayload(other, listOf(hat)))
        ClientState.applyBall(BallPayload(7, "galaxy"))
        ClientState.levelChanged(local)
        assertEquals(listOf(crown), ClientState.wear(local))
        assertNull(ClientState.wear(other))
        assertEquals(emptySet(), ClientState.withBallSkin(7, emptySet()))
    }

    @Test
    fun `disconnect drops everything`() {
        ClientState.applyWear(WearPayload(local, listOf(crown)))
        ClientState.applyBall(BallPayload(7, "galaxy"))
        ClientState.clear()
        assertNull(ClientState.wear(local))
        assertEquals(emptySet(), ClientState.withBallSkin(7, emptySet()))
    }
}
