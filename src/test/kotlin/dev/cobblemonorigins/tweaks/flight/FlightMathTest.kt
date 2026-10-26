package dev.cobblemonorigins.tweaks.flight

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlightMathTest {
    // Charizard at base AIR ride stats: SPEED 30 -> 8.8 blocks/s, ACCELERATION 45 -> 3.75 s hover time (halved per
    // tick), STAMINA 45 -> 36 s; hover refills stamina in 2 s.
    private val top = 0.44
    private val accel = top / (3.75 * 10.0)
    private val staminaSeconds = 36.0
    private val replenishSeconds = 2.0

    private data class Keys(val strafe: Int = 0, val vertical: Int = 0, val forward: Int = 0, val sprint: Boolean = false)

    private val w = Keys(forward = 1)

    /** Runs [ticks] ticks of [keys]; returns the final controls and the distance flown (x = strafe, y = up, z = forward). */
    private fun fly(keys: Keys, ticks: Int, start: Vec3 = Vec3.ZERO): Pair<Vec3, Vec3> {
        var controls = start
        var moved = Vec3.ZERO
        repeat(ticks) {
            controls = FlightMath.step(controls, keys.strafe, keys.vertical, keys.forward, top, accel, keys.sprint)
            moved = moved.add(controls)
        }
        return controls to moved
    }

    @Test
    fun `W reaches top speed level and straight ahead`() {
        val (speed, moved) = fly(w, 100)
        assertEquals(Vec3(0.0, 0.0, top), speed)
        assertEquals(0.0, moved.y)
    }

    @Test
    fun `W never changes altitude while turning frames`() {
        // Whatever the camera pitch, W input only ever produces level (y = 0) controls.
        val (speed, _) = fly(Keys(strafe = 1, forward = 1, sprint = true), 100)
        assertEquals(0.0, speed.y)
    }

    @Test
    fun `space and shift climb and descend at half top speed`() {
        assertEquals(top * FlightMath.VERTICAL_SPEED_FACTOR, fly(Keys(vertical = 1), 10).first.y)
        assertEquals(-top * FlightMath.VERTICAL_SPEED_FACTOR, fly(Keys(vertical = -1), 10).first.y)
    }

    @Test
    fun `vertical speed settles to zero within three ticks and altitude then holds exactly`() {
        val climbing = fly(Keys(vertical = 1), 10).first
        val (afterThree, _) = fly(Keys(), 3, start = climbing)
        assertEquals(0.0, afterThree.y)
        val (_, drift) = fly(Keys(), 100, start = afterThree) // 5 s idle in the air
        assertEquals(Vec3.ZERO, drift)
    }

    @Test
    fun `releasing every key brakes to a hover within a third of a second`() {
        val (cruising, _) = fly(w, 100)
        assertEquals(Vec3.ZERO, fly(Keys(), 6, start = cruising).first)
        val (sprinting, _) = fly(w.copy(sprint = true), 100)
        assertEquals(Vec3.ZERO, fly(Keys(), 9, start = sprinting).first)
    }

    @Test
    fun `A strafes left and D strafes right`() {
        assertEquals(top, fly(Keys(strafe = 1), 100).first.x)
        assertEquals(-top, fly(Keys(strafe = -1), 100).first.x)
    }

    @Test
    fun `diagonal flight keeps straight-line top speed`() {
        assertEquals(top, fly(Keys(strafe = 1, forward = 1), 100).first.length(), 1e-9)
    }

    @Test
    fun `sprint raises forward top speed by half but not vertical speed`() {
        assertEquals(top * FlightMath.SPRINT_SPEED_FACTOR, fly(w.copy(sprint = true), 100).first.z, 1e-12)
        assertEquals(top * FlightMath.VERTICAL_SPEED_FACTOR, fly(Keys(vertical = 1, sprint = true), 10).first.y)
    }

    @Test
    fun `three seconds of sprinting covers half again the distance`() {
        val ratio = fly(w.copy(sprint = true), 60).second.z / fly(w, 60).second.z
        assertEquals(FlightMath.SPRINT_SPEED_FACTOR, ratio, 1e-9)
    }

    @Test
    fun `normal flight never costs stamina`() {
        var stamina = 1f
        repeat(200) { stamina = FlightMath.stamina(stamina, false, 1.0, staminaSeconds, replenishSeconds) } // 10 s
        assertEquals(1f, stamina)
    }

    /** Ticks of full-speed sprint until the bar is empty. */
    private fun sprintTicks(staminaSeconds: Double): Int {
        var stamina = 1f
        var ticks = 0
        while (stamina > 0f && ticks < 10_000) {
            stamina = FlightMath.stamina(stamina, true, FlightMath.SPRINT_SPEED_FACTOR, staminaSeconds, replenishSeconds)
            ticks++
        }
        return ticks
    }

    @Test
    fun `a full bar lasts about seven seconds of full sprint and refills in two`() {
        assertTrue(sprintTicks(staminaSeconds) in 138..142, "Charizard sprint lasted ${sprintTicks(staminaSeconds)} ticks")
        var stamina = 0f
        repeat(41) { stamina = FlightMath.stamina(stamina, false, 0.0, staminaSeconds, replenishSeconds) } // ~2 s (float rounding)
        assertEquals(1f, stamina)
    }

    @Test
    fun `every flyer sprints between six and eight seconds`() {
        // Mantine's AIR STAMINA maps to Cobblemon's 1e-6 s floor; the best stat maps to 80 s.
        assertTrue(sprintTicks(1.0e-6) in 119..121, "lowest stamina sprint lasted ${sprintTicks(1.0e-6)} ticks")
        assertTrue(sprintTicks(80.0) in 159..161, "highest stamina sprint lasted ${sprintTicks(80.0)} ticks")
    }

    @Test
    fun `sprinting below normal top speed costs no stamina`() {
        var stamina = 1f
        for (ratio in listOf(0.0, 0.5, 1.0)) {
            repeat(100) { stamina = FlightMath.stamina(stamina, true, ratio, staminaSeconds, replenishSeconds) }
        }
        assertEquals(1f, stamina)
    }

    @Test
    fun `unusable stamina seconds leave stamina unchanged`() {
        for (seconds in listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(0.5f, FlightMath.stamina(0.5f, true, 1.5, seconds, replenishSeconds), "staminaSeconds $seconds")
        }
    }

    @Test
    fun `unusable replenish seconds leave stamina unchanged`() {
        for (seconds in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(0.5f, FlightMath.stamina(0.5f, false, 0.0, staminaSeconds, seconds), "replenishSeconds $seconds")
        }
    }

    @Test
    fun `negative or NaN speed ratio never drains`() {
        assertEquals(0.5f, FlightMath.stamina(0.5f, true, -3.0, staminaSeconds, replenishSeconds))
        assertEquals(0.5f, FlightMath.stamina(0.5f, true, Double.NaN, staminaSeconds, replenishSeconds))
    }

    @Test
    fun `a NaN result becomes an empty bar`() {
        assertEquals(0f, FlightMath.stamina(Float.NaN, false, 0.0, staminaSeconds, replenishSeconds))
        assertEquals(0f, FlightMath.stamina(Float.NaN, true, 1.5, Double.NaN, replenishSeconds))
    }

    @Test
    fun `W follows the camera while the mount still faces elsewhere`() {
        // Rider looks west (yaw 90), mount still faces south (yaw 0).
        val rideSpace = FlightMath.rotateFrame(fly(w, 100).first, fromYaw = 90f, toYaw = 0f)
        val world = toWorld(rideSpace, mountYaw = 0f)
        assertEquals(-top, world.x, 1e-6)
        assertTrue(abs(world.z) < 1e-6)
    }

    @Test
    fun `frame rotation round trips`() {
        val v = Vec3(0.1, -0.2, 0.3)
        val back = FlightMath.rotateFrame(FlightMath.rotateFrame(v, 17f, 250f), 250f, 17f)
        assertTrue(back.distanceTo(v) < 1e-6)
    }

    /** Ride space to world, exactly as Cobblemon 1.8.1 `PokemonEntity.travel` applies it. */
    private fun toWorld(rideSpace: Vec3, mountYaw: Float): Vec3 {
        val sin = Mth.sin(Math.toRadians(mountYaw.toDouble()).toFloat()).toDouble()
        val cos = Mth.cos(Math.toRadians(mountYaw.toDouble()).toFloat()).toDouble()
        return Vec3(rideSpace.x * cos - rideSpace.z * sin, rideSpace.y, rideSpace.z * cos + rideSpace.x * sin)
    }
}
