package dev.cobblemonorigins.tweaks.flight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SprintTrackerTest {
    private data class Tick(val forward: Boolean, val sprintKey: Boolean, val stamina: Float = 1f)

    /** Feeds one tick per entry and returns the sprint state after each tick. */
    private fun run(vararg ticks: Tick): List<Boolean> {
        val tracker = SprintTracker()
        var sprinting = false
        return ticks.map {
            sprinting = tracker.update(it.forward, it.sprintKey, sprinting, it.stamina)
            sprinting
        }
    }

    private val w = Tick(true, false)
    private val wCtrl = Tick(true, true)
    private val idle = Tick(false, false)

    @Test
    fun `W alone does not sprint`() = assertEquals(listOf(false, false, false), run(w, w, w))

    @Test
    fun `sprint key with W sprints and lasts after the key is released`() =
        assertEquals(listOf(true, true, true), run(wCtrl, w, w))

    @Test
    fun `releasing W ends the sprint`() {
        val states = run(wCtrl, idle, idle, idle, idle, idle, idle, idle, idle, w)
        assertTrue(states.first())
        assertFalse(states.drop(1).any { it })
    }

    @Test
    fun `double-tap W within the window sprints`() = assertTrue(run(w, idle, idle, w, w).last())

    @Test
    fun `slow second tap does not sprint`() = assertFalse(run(w, idle, idle, idle, idle, idle, idle, idle, idle, w).last())

    @Test
    fun `a W tap from before a new flight does not complete a double-tap`() {
        val tracker = SprintTracker()
        tracker.update(true, false, false, 1f)  // W tapped while walking...
        tracker.update(false, false, false, 1f)
        tracker.resetTaps()                      // ...then a new flight starts
        assertFalse(tracker.update(true, false, false, 1f))
    }

    @Test
    fun `empty bar stops the sprint until stamina has fully refilled`() {
        val states = run(
            wCtrl.copy(stamina = 0.5f),
            wCtrl.copy(stamina = 0f),   // exhausted: sprint stops even with the key held
            wCtrl.copy(stamina = 0.5f), // refilling: still locked
            wCtrl.copy(stamina = 1f),   // full again: sprint works
        )
        assertEquals(listOf(true, false, false, true), states)
    }
}
