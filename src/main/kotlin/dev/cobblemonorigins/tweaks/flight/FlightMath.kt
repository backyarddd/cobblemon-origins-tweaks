package dev.cobblemonorigins.tweaks.flight

import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

/** Flight physics, kept free of Cobblemon and client types so it can be unit tested. All steps are per tick. */
internal object FlightMath {
    const val VERTICAL_SPEED_FACTOR = 0.5
    const val SPRINT_SPEED_FACTOR = 1.5

    /** Ticks to brake from top speed to a hover once the keys are released. */
    private const val BRAKE_TICKS = 6.0

    /** Ticks for the vertical speed to reach its target, so climbs start and stop without drifting. */
    private const val VERTICAL_RESPONSE_TICKS = 2.0

    /**
     * Sprint stamina scale: a full bar lasts (bird STAMINA seconds) x this / sprint speed ratio of full sprint,
     * clamped to [MIN_SPRINT_SECONDS]..[MAX_SPRINT_SECONDS]. Charizard's base AIR STAMINA (36 s) gives
     * 36 x 0.29 / 1.5 = 7.0 s.
     */
    private const val SPRINT_STAMINA_FRACTION = 0.29
    private const val MIN_SPRINT_SECONDS = 6.0
    private const val MAX_SPRINT_SECONDS = 8.0

    /**
     * One tick of the flight controls. [controls] and the result are (strafe, vertical, forward) in blocks per tick,
     * relative to the rider's yaw (x = left, y = up, z = forward, always level). Inputs are -1, 0 or 1.
     * Speeds reach their targets exactly, so a released key leaves no residual drift.
     */
    fun step(
        controls: Vec3,
        strafe: Int,
        vertical: Int,
        forward: Int,
        topSpeed: Double,
        accel: Double,
        sprinting: Boolean,
    ): Vec3 {
        val boost = if (sprinting && forward > 0) SPRINT_SPEED_FACTOR else 1.0
        val diagonal = if (strafe != 0 && forward != 0) 1.0 / sqrt(2.0) else 1.0 // diagonals keep top speed
        val brake = topSpeed / BRAKE_TICKS
        val maxVertical = topSpeed * VERTICAL_SPEED_FACTOR
        val verticalStep = maxVertical / VERTICAL_RESPONSE_TICKS
        return Vec3(
            approach(controls.x, strafe * topSpeed * diagonal, accel, brake),
            approach(controls.y, vertical * maxVertical, verticalStep, verticalStep),
            approach(controls.z, forward * topSpeed * boost * diagonal, accel * boost, brake),
        )
    }

    /** Moves [value] toward [target] by up to [speedUp] while gaining speed in the same direction, else [slowDown]. */
    private fun approach(value: Double, target: Double, speedUp: Double, slowDown: Double): Double {
        val gaining = abs(target) > abs(value) && (value == 0.0 || sign(value) == sign(target))
        val step = if (gaining) speedUp else slowDown
        return if (abs(target - value) <= step) target else value + sign(target - value) * step
    }

    /** Seconds a full bar lasts at full sprint speed, from the bird STAMINA mapping in seconds. */
    fun sprintSeconds(staminaSeconds: Double): Double =
        (staminaSeconds * SPRINT_STAMINA_FRACTION / SPRINT_SPEED_FACTOR).coerceIn(MIN_SPRINT_SECONDS, MAX_SPRINT_SECONDS)

    /**
     * One tick of stamina, which only sprinting uses. [speedRatio] is forward speed / normal top speed. Sprinting only
     * pays for the speed above normal top speed, so full sprint empties the bar in [sprintSeconds]; otherwise stamina
     * refills over [replenishSeconds]. Unusable datapack values leave stamina unchanged.
     */
    fun stamina(stamina: Float, sprinting: Boolean, speedRatio: Double, staminaSeconds: Double, replenishSeconds: Double): Float {
        val usable = staminaSeconds.isFinite() && staminaSeconds > 0.0 && replenishSeconds.isFinite() && replenishSeconds > 0.0
        val next = if (!usable) {
            stamina
        } else {
            val change = if (sprinting) {
                val ratio = if (speedRatio > 0.0) speedRatio else 0.0 // also maps NaN to 0
                val gain = max(0.0, ratio - 1.0)
                -gain / ((SPRINT_SPEED_FACTOR - 1.0) * sprintSeconds(staminaSeconds))
            } else {
                1.0 / replenishSeconds
            }
            (stamina + change / 20.0).toFloat().coerceIn(0f, 1f)
        }
        return if (next.isNaN()) 0f else next
    }

    /** Re-expresses [velocity] from the frame facing [fromYaw] to the frame facing [toYaw] (degrees, Minecraft yaw). */
    fun rotateFrame(velocity: Vec3, fromYaw: Float, toYaw: Float): Vec3 =
        velocity.yRot(Math.toRadians((toYaw - fromYaw).toDouble()).toFloat())
}
