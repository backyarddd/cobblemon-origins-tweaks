package dev.cobblemonorigins.tweaks.flight

import net.minecraft.client.Minecraft

/**
 * Sprint intent of the local rider, read from the client's key bindings the way Cobblemon's own riding behaviours
 * read sprint/boost input (e.g. `HorseBehaviour.handleSprinting`). Vanilla's own sprint flag is not enough: it only
 * starts a double-tap sprint while the vehicle is on the ground.
 *
 * Client-only: call it only from client-side ticks for the local player. There is one local rider, so the
 * double-tap and exhaustion tracking lives here instead of in the synced riding state.
 */
internal object SprintInput {
    private val tracker = SprintTracker()

    /** Call once per client tick while flying. Returns whether the rider is sprinting this tick. */
    fun update(wasSprinting: Boolean, stamina: Float): Boolean {
        val options = Minecraft.getInstance().options
        return tracker.update(options.keyUp.isDown, options.keySprint.isDown, wasSprinting, stamina)
    }

    /** Call when a new flight starts. */
    fun reset() = tracker.resetTaps()
}

/** Key-state logic behind [SprintInput], kept free of client classes so it can be unit tested. */
internal class SprintTracker {
    private var forwardWasDown = false
    private var tapTicksLeft = 0
    private var exhausted = false

    fun update(forward: Boolean, sprintKey: Boolean, wasSprinting: Boolean, stamina: Float): Boolean {
        val tapped = forward && !forwardWasDown
        val doubleTapped = tapped && tapTicksLeft > 0
        tapTicksLeft = if (tapped && !doubleTapped) DOUBLE_TAP_TICKS else (tapTicksLeft - 1).coerceAtLeast(0)
        forwardWasDown = forward
        // An empty bar locks sprinting until it has refilled completely.
        if (stamina <= 0f) exhausted = true else if (stamina >= 1f) exhausted = false
        // Like vanilla sprinting: starts with sprint key + W or a W double-tap, lasts until W is released.
        if (!forward || exhausted) return false
        return wasSprinting || sprintKey || doubleTapped
    }

    /** Forgets W taps from before a new flight, so they cannot complete a double-tap. */
    fun resetTaps() {
        forwardWasDown = false
        tapTicksLeft = 0
    }

    private companion object {
        const val DOUBLE_TAP_TICKS = 7 // same window as vanilla and Cobblemon's horse sprint
    }
}
