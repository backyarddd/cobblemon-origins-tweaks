package dev.cobblemonorigins.tweaks.flight

import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.behaviour.Side
import com.cobblemon.mod.common.api.riding.behaviour.ridingState
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3

/**
 * Riding state for classic flight. Only [sprinting] is synced beyond Cobblemon's base state (ride velocity, stamina):
 * flight is client-authoritative, the rest is the rider client's own bookkeeping.
 */
class ClassicFlightState : RidingBehaviourState() {
    /** Rider is sprint-flying; synced so the server and other clients see it. */
    val sprinting = ridingState(false, Side.BOTH)

    /** (strafe, vertical, forward) speeds in blocks per tick, relative to the rider's yaw (x = left, y = up). */
    val controls = ridingState(Vec3.ZERO, Side.CLIENT)

    /** Entity tick the controls were last stepped on; Cobblemon asks for ride velocity more than once per tick. */
    val lastStepTick = ridingState(NEVER, Side.CLIENT)

    /** Ticks of automatic climb left after a take-off from the ground. */
    val takeOffTicks = ridingState(0, Side.CLIENT)

    /** The local rider's sprint input tracking has been reset for this flight. */
    val sprintInputStarted = ridingState(false, Side.CLIENT)

    override fun encode(buffer: FriendlyByteBuf) {
        super.encode(buffer)
        buffer.writeBoolean(sprinting.get())
    }

    override fun decode(buffer: FriendlyByteBuf) {
        super.decode(buffer)
        sprinting.set(buffer.readBoolean(), true)
    }

    override fun reset() {
        super.reset()
        sprinting.set(false, true)
        controls.set(Vec3.ZERO, true)
        lastStepTick.set(NEVER, true)
        takeOffTicks.set(0, true)
        sprintInputStarted.set(false, true)
    }

    override fun copy(): ClassicFlightState = ClassicFlightState().also {
        it.rideVelocity.set(rideVelocity.get(), true)
        it.stamina.set(stamina.get(), true)
        it.sprinting.set(sprinting.get(), true)
        it.controls.set(controls.get(), true)
        it.lastStepTick.set(lastStepTick.get(), true)
        it.takeOffTicks.set(takeOffTicks.get(), true)
        it.sprintInputStarted.set(sprintInputStarted.get(), true)
    }

    override fun shouldSync(previous: RidingBehaviourState): Boolean {
        if (previous !is ClassicFlightState) return false
        return previous.sprinting.get() != sprinting.get() || super.shouldSync(previous)
    }

    companion object {
        const val NEVER = Int.MIN_VALUE
    }
}
