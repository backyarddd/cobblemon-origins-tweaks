package dev.cobblemonorigins.tweaks.flight

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlightLeanTest {
    private fun assertNear(expected: Float, actual: Float) =
        assertTrue(abs(expected - actual) < 1e-4f, "expected $expected, got $actual")

    @Test
    fun strafingRightLeansRightAndLeftLeansLeft() {
        // Facing yaw 0 looks along +z; the A key (left) is +x, so moving -x is moving right.
        assertNear(12f, FlightLean.targetDegrees(-0.5, 0.0, 0f))
        assertNear(-12f, FlightLean.targetDegrees(0.5, 0.0, 0f))
    }

    @Test
    fun forwardAndBackwardFlightDoNotLean() {
        assertNear(0f, FlightLean.targetDegrees(0.0, 0.44, 0f))
        assertNear(0f, FlightLean.targetDegrees(0.0, -0.44, 0f))
    }

    @Test
    fun leanFollowsTheMountsFacing() {
        // Facing yaw 90 looks along -x; left is +z, so moving -z is moving right.
        assertNear(12f, FlightLean.targetDegrees(0.0, -0.5, 90f))
        assertNear(-12f, FlightLean.targetDegrees(0.0, 0.5, 90f))
    }

    @Test
    fun leanScalesWithSidewaysSpeedAndCapsAtTheMaximum() {
        assertNear(6f, FlightLean.targetDegrees(-0.25, 0.0, 0f))
        assertEquals(12f, FlightLean.targetDegrees(-5.0, 0.0, 0f))
    }
}
