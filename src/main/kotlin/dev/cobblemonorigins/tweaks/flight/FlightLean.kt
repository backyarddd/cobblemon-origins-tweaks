package dev.cobblemonorigins.tweaks.flight

import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeState
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Client-only cosmetic bank: a classic-flight mount tilts toward the side it strafes to, and its rider tilts with it.
 * Render only: the entity's rotation, the camera (built from Cobblemon's seat locators, not the render stack), physics
 * and flight direction are untouched. Driven by the mount's position change, which every client has, so other players
 * see the lean too.
 */
object FlightLean {
    private const val MAX_DEGREES = 12f
    // Sideways blocks per tick for a full lean; a mount with base ride stats strafes at about 0.44.
    private const val FULL_LEAN_SPEED = 0.5
    // Fraction of the way to the target lean covered each tick.
    private const val EASE = 0.25f
    // After a long frame gap (lag, chunk reload) catch up at most this many ticks of easing.
    private const val MAX_CATCH_UP_TICKS = 10
    private const val MIN_VISIBLE_DEGREES = 0.01f

    private class Lean {
        var previous = 0f
        var current = 0f
        var lastTick = -1
    }

    // Weak keys: an entry goes away with its entity, no unload hook needed.
    private val leans = WeakHashMap<PokemonEntity, Lean>()

    /**
     * Called by LivingEntityRendererMixin right after the renderer's pushPose, where the stack is only the entity's
     * render translation. One rotation about a world-space point then moves the mount and its rider together.
     */
    @JvmStatic
    fun apply(entity: LivingEntity, partialTick: Float, poseStack: PoseStack) {
        val mount = entity as? PokemonEntity ?: entity.vehicle as? PokemonEntity ?: return
        val flying = isClassicFlying(mount)
        val lean = leans[mount] ?: if (flying) Lean().also { leans[mount] = it } else return
        step(lean, mount, flying)

        // With Cobblemon's own roll active its orientation matrix is already on the stack; never stack a lean on it.
        // OrientationControllable is mixed into entities by Cobblemon, so it is only visible at runtime.
        if (((mount as Any) as? OrientationControllable)?.orientationController?.active == true) return
        val degrees = Mth.lerp(partialTick, lean.previous, lean.current)
        if (abs(degrees) < MIN_VISIBLE_DEGREES) {
            if (!flying) leans.remove(mount)
            return
        }

        val yaw = Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot) * Mth.DEG_TO_RAD
        // Mount's centre relative to the entity being drawn (zero offset when that is the mount itself).
        val pivotX = Mth.lerp(partialTick.toDouble(), mount.xo, mount.x) - Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
        val pivotY = Mth.lerp(partialTick.toDouble(), mount.yo, mount.y) - Mth.lerp(partialTick.toDouble(), entity.yo, entity.y) +
            mount.bbHeight / 2.0
        val pivotZ = Mth.lerp(partialTick.toDouble(), mount.zo, mount.z) - Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
        // Positive rotation about the forward axis (-sin yaw, 0, cos yaw) tips the top toward the mount's right.
        val bank = Quaternionf().rotationAxis(degrees * Mth.DEG_TO_RAD, -sin(yaw), 0f, cos(yaw))
        poseStack.rotateAround(bank, pivotX.toFloat(), pivotY.toFloat(), pivotZ.toFloat())
    }

    private fun isClassicFlying(mount: PokemonEntity): Boolean {
        if (mount.controllingPassenger == null) return false
        val context = mount.ridingController?.context ?: return false
        if (context.behaviour == ClassicFlightBehaviour.KEY) return true
        // A composite ride (e.g. land + air) reports its own key; the child it is running is in its state.
        return (context.state as? CompositeState)?.activeBehaviour?.get() == ClassicFlightBehaviour.KEY
    }

    /** Eases toward the target once per game tick, however many frames render in between. */
    private fun step(lean: Lean, mount: PokemonEntity, flying: Boolean) {
        if (lean.lastTick == mount.tickCount) return
        val ticks = if (lean.lastTick < 0) 1 else (mount.tickCount - lean.lastTick).coerceIn(1, MAX_CATCH_UP_TICKS)
        lean.lastTick = mount.tickCount
        val target = if (flying) targetDegrees(mount.x - mount.xo, mount.z - mount.zo, mount.yBodyRot) else 0f
        repeat(ticks) {
            lean.previous = lean.current
            lean.current += (target - lean.current) * EASE
        }
    }

    /**
     * Lean for one tick of movement [dx], [dz] by a mount facing [yawDegrees]: sideways speed relative to the facing,
     * positive (top toward the right) when moving right.
     */
    internal fun targetDegrees(dx: Double, dz: Double, yawDegrees: Float): Float {
        val facing = yawDegrees * Mth.DEG_TO_RAD
        // Minecraft's local +x (the A key) is (cos yaw, sin yaw).
        val left = dx * cos(facing) + dz * sin(facing)
        return (-(left / FULL_LEAN_SPEED).coerceIn(-1.0, 1.0) * MAX_DEGREES).toFloat()
    }
}
