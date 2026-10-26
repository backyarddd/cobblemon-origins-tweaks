package dev.cobblemonorigins.tweaks.flight

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.CobblemonRideSettings
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviour
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.resolveDouble
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Classic flight: WASD moves level and relative to the camera, Space ascends, Shift descends, no roll, normal mouse
 * look. Speeds settle exactly on their targets, so the mount never drifts: no keys means a steady hover.
 *
 * Top speed and stamina use the global bird settings (`data/cobblemon/ride_settings/bird.json`) so former bird/jet
 * mounts keep their speed; acceleration, turn rate and stamina refill use the global hover settings.
 */
object ClassicFlightBehaviour : RidingBehaviour<ClassicFlightSettings, ClassicFlightState> {
    val KEY: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ClassicFlight.MOD_ID, "air/classic")

    override val key: ResourceLocation = KEY

    private const val FLY_POSE_SPEED = 0.1
    private const val MAX_YAW_DIFF = 90f
    private const val TAKE_OFF_TICKS = 10
    private const val SPRINT_FOV = 1.15f // vanilla sprinting widens the FOV by about this much

    // Fallbacks equal to Cobblemon 1.8.1-1.8.2 shipped ride_settings (identical), used only if a datapack blanks a global expression.
    private val DEFAULT_SPEED = "q.get_ride_stats('SPEED', 'AIR', 20.0, 4.0)".asExpression()
    private val DEFAULT_STAMINA = "q.get_ride_stats('STAMINA', 'AIR', 80.0, 0.0)".asExpression()
    private val DEFAULT_INFINITE_STAMINA = "false".asExpression()
    private val DEFAULT_ACCELERATION = "q.get_ride_stats('ACCELERATION', 'AIR', 1.0, 6.0)".asExpression()
    private val DEFAULT_HANDLING = "q.get_ride_stats('SKILL', 'AIR', 270.0, 45.0)".asExpression()
    private val DEFAULT_REPLENISH = "2.0".asExpression()

    private fun PokemonEntity.resolve(expression: Expression?, fallback: Expression): Double =
        runtime.resolveDouble(expression ?: fallback)

    /** Blocks per tick, mapped from the SPEED ride stat exactly as Cobblemon's bird flight maps it. */
    private fun topSpeed(vehicle: PokemonEntity): Double =
        vehicle.resolve(CobblemonRideSettings.bird.speedExpr, DEFAULT_SPEED) / 20.0

    override fun getRidingStyle(settings: ClassicFlightSettings, state: ClassicFlightState): RidingStyle = RidingStyle.AIR

    override fun isActive(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean =
        !vehicle.isUnderWater

    override fun pose(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): PoseType =
        if (state.rideVelocity.get().horizontalDistance() > FLY_POSE_SPEED) PoseType.FLY else PoseType.HOVER

    override fun speed(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player): Float =
        state.rideVelocity.get().length().toFloat()

    override fun tick(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player, input: Vec3) {
        // Flight is client-authoritative in Cobblemon: the sprint flag and stamina reach the server with the rest of the
        // riding state. The server's infiniteRideStamina option is applied by Cobblemon after this tick (refills to 1).
        if (!vehicle.level().isClientSide) return
        // Only the rider's own client reads its keyboard; the synced flag carries it everywhere else.
        if (driver.isLocalPlayer) {
            // A new flight must not inherit a W tap from before it (e.g. walking up to a take-off).
            if (!state.sprintInputStarted.get()) {
                SprintInput.reset()
                state.sprintInputStarted.set(true, false)
            }
            state.sprinting.set(SprintInput.update(state.sprinting.get(), state.stamina.get()), false)
        }
        tickStamina(state, vehicle)
    }

    /** Stamina only matters for sprinting: it drains while sprinting and refills otherwise, at hover's refill rate. */
    private fun tickStamina(state: ClassicFlightState, vehicle: PokemonEntity) {
        val bird = CobblemonRideSettings.bird
        if (vehicle.runtime.resolveBoolean(bird.infiniteStamina ?: DEFAULT_INFINITE_STAMINA)) {
            // Keep the bar full, so an empty bar saved from an earlier ride cannot lock sprinting.
            if (state.stamina.get() != 1f) state.stamina.set(1f, false)
            return
        }
        val topSpeed = topSpeed(vehicle)
        val speedRatio = if (topSpeed > 1.0e-6) state.controls.get().z / topSpeed else 0.0
        val next = FlightMath.stamina(
            state.stamina.get(),
            state.sprinting.get(),
            speedRatio,
            staminaSeconds = vehicle.resolve(bird.staminaExpr, DEFAULT_STAMINA),
            replenishSeconds = vehicle.resolve(CobblemonRideSettings.hover.stamReplenishTimeSeconds, DEFAULT_REPLENISH),
        )
        state.stamina.set(next, false)
    }

    override fun rotation(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: LivingEntity): Vec2 =
        Vec2(driver.xRot, vehicle.yRot + turnTowardsRider(vehicle, driver))

    /** Port of `HoverBehaviour.calcRotAmount`: turns faster the further the rider looks away, a bit slower at speed. */
    private fun turnTowardsRider(vehicle: PokemonEntity, driver: LivingEntity): Float {
        val handling = vehicle.resolve(CobblemonRideSettings.hover.handlingExpr, DEFAULT_HANDLING) / 20.0
        val diff = Mth.wrapDegrees(driver.yRot - vehicle.yRot).coerceIn(-MAX_YAW_DIFF, MAX_YAW_DIFF)
        var rate = handling.toFloat() * sqrt(abs(diff / MAX_YAW_DIFF)) * sign(diff)
        val topSpeed = topSpeed(vehicle)
        val speedFraction = if (topSpeed < 0.01) 0.0 else (vehicle.deltaMovement.length() / topSpeed).coerceIn(0.0, 1.0)
        rate -= rate * 0.5f * (speedFraction * speedFraction).toFloat()
        return rate.coerceIn(-abs(diff), abs(diff))
    }

    override fun velocity(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player, input: Vec3): Vec3 {
        // Cobblemon asks for ride velocity more than once per tick; step the controls once per tick.
        if (state.lastStepTick.get() != vehicle.tickCount) {
            stepControls(state, vehicle, driver)
            state.lastStepTick.set(vehicle.tickCount, false)
        }
        // Controls are relative to the rider's yaw; Cobblemon applies ride velocity relative to the mount's yaw.
        val rideSpace = FlightMath.rotateFrame(state.controls.get(), driver.yRot, vehicle.yRot)
        state.rideVelocity.set(rideSpace, false)
        return rideSpace
    }

    private fun stepControls(state: ClassicFlightState, vehicle: PokemonEntity, driver: Player) {
        // First step after Cobblemon switched from walking to flying (it marks take-off with an upward ride velocity):
        // climb briefly on our own so the mount clears the ground, like Cobblemon's bird take-off hop.
        if (state.lastStepTick.get() == ClassicFlightState.NEVER && state.rideVelocity.get().y > 0.0) {
            state.takeOffTicks.set(TAKE_OFF_TICKS, false)
        }
        val takingOff = state.takeOffTicks.get() > 0
        if (takingOff) state.takeOffTicks.set(state.takeOffTicks.get() - 1, false)
        val vertical = when {
            driver.isShiftKeyDown -> -1
            driver.jumping || takingOff -> 1
            else -> 0
        }
        val topSpeed = topSpeed(vehicle)
        // Hover's acceleration time, halved: Cobblemon steps hover's velocity twice per tick.
        val accel = topSpeed / (vehicle.resolve(CobblemonRideSettings.hover.accelerationExpr, DEFAULT_ACCELERATION) * 10.0)
        val next = FlightMath.step(
            state.controls.get(),
            strafe = sign(driver.xxa).toInt(),
            vertical = vertical,
            forward = sign(driver.zza).toInt(),
            topSpeed = topSpeed,
            accel = accel,
            sprinting = state.sprinting.get(),
        )
        state.controls.set(next, false)
    }

    override fun angRollVel(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player, deltaTime: Double): Vec3 =
        Vec3.ZERO

    override fun rotationOnMouseXY(
        settings: ClassicFlightSettings,
        state: ClassicFlightState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double,
    ): Vec3 = Vec3.ZERO

    override fun canJump(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player): Boolean = false

    override fun setRideBar(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player): Float =
        state.stamina.get()

    override fun jumpForce(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player, jumpStrength: Int): Vec3 =
        Vec3.ZERO

    override fun gravity(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, regularGravity: Double): Double = 0.0

    /** Client-only. Cobblemon scales this by the FOV Effects slider and the game eases FOV changes over a few ticks. */
    override fun rideFovMultiplier(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity, driver: Player): Float =
        if (state.sprinting.get()) SPRINT_FOV else 1.0f

    override fun useAngVelSmoothing(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = false

    override fun inertia(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Double = 1.0

    override fun shouldRoll(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = false

    override fun turnOffOnGround(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = false

    override fun dismountOnShift(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = false

    override fun shouldRotatePokemonHead(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = false

    override fun shouldRotateRiderHead(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): Boolean = true

    override fun getRideSounds(settings: ClassicFlightSettings, state: ClassicFlightState, vehicle: PokemonEntity): RideSoundSettingsList =
        settings.rideSounds

    override fun createDefaultState(settings: ClassicFlightSettings): ClassicFlightState = ClassicFlightState()
}
