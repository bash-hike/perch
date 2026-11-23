package app.perch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyTest {
    private val idle = 60_000L
    private val battery = 15 * 60_000L

    @Test fun playingShowsPlayerNoMatterHowLongAgoItStarted() =
        assertEquals(Mode.PLAYER, Policy.mode(hasSession = true, playing = true, stoppedForMs = 9_999_999, idleDelayMs = idle))

    @Test fun briefPauseKeepsThePlayer() =
        assertEquals(Mode.PLAYER, Policy.mode(true, false, 30_000, idle))

    @Test fun longPauseFallsBackToTheClock() =
        assertEquals(Mode.CLOCK, Policy.mode(true, false, 60_000, idle))

    @Test fun noSessionIsAlwaysTheClock() =
        assertEquals(Mode.CLOCK, Policy.mode(false, false, 0, idle))

    @Test fun pluggedClockStaysOnForever() =
        assertTrue(Policy.keepAwake(Mode.CLOCK, plugged = true, clockForMs = 99L * 3_600_000, batteryClockMs = battery))

    @Test fun batteryClockStaysOnForTheSetTimeOnly() {
        assertTrue(Policy.keepAwake(Mode.CLOCK, false, battery - 1, battery))
        assertFalse(Policy.keepAwake(Mode.CLOCK, false, battery, battery))
    }

    @Test fun playerHoldsTheScreenOnBattery() =
        assertTrue(Policy.keepAwake(Mode.PLAYER, false, 99L * 3_600_000, battery))

    @Test fun clockDimsOnlyAfterTheTouchWindow() {
        assertFalse(Policy.dimmed(Mode.CLOCK, 7_999))
        assertTrue(Policy.dimmed(Mode.CLOCK, 8_000))
        assertFalse(Policy.dimmed(Mode.PLAYER, 999_999))
    }

    @Test fun timeFormatting() {
        assertEquals("0:00", Policy.formatTime(-5))
        assertEquals("3:07", Policy.formatTime(187_999))
        assertEquals("1:01:01", Policy.formatTime(3_661_000))
    }
}
