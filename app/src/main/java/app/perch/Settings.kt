package app.perch

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    /** How long the clock holds the screen on while running on battery. */
    val batteryClockMin: Int = 15,
    /** How long a paused song keeps the player up before the clock takes over. */
    val idleDelaySec: Int = 60,
    /** Open Perch over the lock screen when a song starts while the tablet is asleep or locked. */
    val openOnPlay: Boolean = true,
)

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("perch", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        Settings(
            batteryClockMin = sp.getInt("batteryClockMin", 15),
            idleDelaySec = sp.getInt("idleDelaySec", 60),
            openOnPlay = sp.getBoolean("openOnPlay", true),
        )
    )
    val state: StateFlow<Settings> = _state

    fun update(change: Settings.() -> Settings) {
        val next = _state.value.change()
        _state.value = next
        sp.edit()
            .putInt("batteryClockMin", next.batteryClockMin)
            .putInt("idleDelaySec", next.idleDelaySec)
            .putBoolean("openOnPlay", next.openOnPlay)
            .apply()
    }
}
