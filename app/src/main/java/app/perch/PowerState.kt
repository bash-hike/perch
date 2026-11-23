package app.perch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PowerInfo(val level: Int = 100, val plugged: Boolean = false)

/** Tracks the battery for as long as the process lives (the receiver is registered once). */
class PowerState(context: Context) {
    private val _state = MutableStateFlow(PowerInfo())
    val state: StateFlow<PowerInfo> = _state

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = publish(i)
        }
        // ACTION_BATTERY_CHANGED is sticky, so registering also returns the current reading.
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let(::publish)
    }

    private fun publish(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        _state.value = PowerInfo(
            level = if (level >= 0) level * 100 / scale else _state.value.level,
            plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0,
        )
    }
}
