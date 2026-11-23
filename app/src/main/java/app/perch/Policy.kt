package app.perch

/** What the screen shows: the player, or the always-on-display style clock. */
enum class Mode { PLAYER, CLOCK }

/**
 * The display rules as pure functions, so they can be tested without a device.
 *
 * Every time here is a duration in milliseconds, never a wall-clock instant.
 */
object Policy {
    /**
     * PLAYER while a song plays, and for [idleDelayMs] after it stopped (so a pause between
     * songs does not flip to the clock). With no media session at all it is always the clock.
     */
    fun mode(hasSession: Boolean, playing: Boolean, stoppedForMs: Long, idleDelayMs: Long): Mode =
        if (playing || (hasSession && stoppedForMs < idleDelayMs)) Mode.PLAYER else Mode.CLOCK

    /**
     * Whether Perch should hold the screen on.
     *
     * Plugged in, the clock stays up forever. On battery it stays up for [batteryClockMs] after
     * the last touch or the switch to the clock; after that the system's own screen timeout takes
     * over. The player always holds the screen: it is a speaker's face.
     */
    fun keepAwake(mode: Mode, plugged: Boolean, clockForMs: Long, batteryClockMs: Long): Boolean =
        mode == Mode.PLAYER || plugged || clockForMs < batteryClockMs

    /** The clock dims to a low backlight until someone touches it. */
    fun dimmed(mode: Mode, sinceTouchMs: Long, brightForMs: Long = BRIGHT_AFTER_TOUCH_MS): Boolean =
        mode == Mode.CLOCK && sinceTouchMs >= brightForMs

    const val BRIGHT_AFTER_TOUCH_MS = 8_000L

    /** Formats a position as m:ss (or h:mm:ss from an hour up). */
    fun formatTime(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
