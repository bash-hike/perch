package app.perch.ui

import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.perch.DemoHooks
import app.perch.Mode
import app.perch.PerchApp
import app.perch.Policy
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Decides between the player and the clock, and keeps the window's wake and brightness state in
 * line with [Policy]. [touchedAt] is an elapsedRealtime value the Activity bumps on every touch.
 */
@Composable
fun PerchRoot(
    app: PerchApp,
    window: Window,
    touchedAt: State<Long>,
    hasAccess: Boolean,
    onAllowAccess: () -> Unit,
    /** Null where the settings sheet cannot be shown (the screen saver). */
    setup: Setup? = null,
    /** Non-null in the screen saver: shows a close button that ends it. */
    onExit: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val now by app.watcher.state.collectAsState()
    val settings by app.settings.state.collectAsState()
    val power by app.power.state.collectAsState()
    val deviceName = remember {
        DemoHooks.deviceName ?: android.provider.Settings.Global.getString(context.contentResolver, android.provider.Settings.Global.DEVICE_NAME)
            ?: android.os.Build.MODEL
    }

    // One-second heartbeat: drives the clock face and every timeout below.
    val tick by produceState(SystemClock.elapsedRealtime()) {
        while (true) { value = SystemClock.elapsedRealtime(); delay(1000) }
    }

    val stoppedFor = tick - now.lastPlayingAt
    val mode = Policy.mode(now.hasSession, now.isPlaying, stoppedFor, settings.idleDelaySec * 1000L)

    // When the clock began: the later of switching to it and the last touch.
    var clockSince by remember { mutableLongStateOf(tick) }
    var lastMode by remember { mutableStateOf(mode) }
    if (mode != lastMode) { lastMode = mode; if (mode == Mode.CLOCK) clockSince = tick }
    val clockFor = tick - maxOf(clockSince, touchedAt.value)

    val keepAwake = Policy.keepAwake(mode, power.plugged, clockFor, settings.batteryClockMin * 60_000L)
    val dimmed = Policy.dimmed(mode, tick - touchedAt.value)

    LaunchedEffect(keepAwake) {
        if (keepAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    LaunchedEffect(dimmed) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (dimmed) DIM_BRIGHTNESS else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    val transport = remember(app) {
        Transport(app.watcher::play, app.watcher::pause, app.watcher::next, app.watcher::previous, app.watcher::seekTo)
    }
    var sheet by remember { mutableStateOf(false) }
    val awake = !dimmed

    Box(Modifier.fillMaxSize()) {
        Crossfade(mode, animationSpec = tween(700), label = "mode") { m ->
            when (m) {
                Mode.PLAYER -> PlayerScreen(now, transport, deviceName)
                Mode.CLOCK -> ClockScreen(
                    time = LocalDateTime.now(),
                    is24h = android.text.format.DateFormat.is24HourFormat(context),
                    power = power,
                    now = now,
                    awake = awake,
                    hasAccess = hasAccess,
                    onAllowAccess = onAllowAccess,
                )
            }
        }

        // Corner buttons: always on the player, only after a touch on the clock. The screen saver
        // gets a close button instead of settings (a dialog cannot open from a screen saver).
        AnimatedVisibility(
            visible = mode == Mode.PLAYER || awake,
            modifier = Modifier.align(if (onExit != null) Alignment.TopStart else Alignment.TopEnd).safeDrawingPadding().padding(16.dp),
            enter = fadeIn(), exit = fadeOut(),
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))
                    .clickable { if (onExit != null) onExit() else sheet = true },
                contentAlignment = Alignment.Center,
            ) {
                if (onExit != null) CloseGlyph(Color.White.copy(alpha = 0.85f)) else TuneGlyph(Color.White.copy(alpha = 0.85f))
            }
        }
    }

    if (sheet && setup != null) {
        SettingsSheet(
            settings = settings,
            hasAccess = hasAccess,
            setup = setup,
            onChange = app.settings::update,
            onAllowAccess = onAllowAccess,
            onDismiss = { sheet = false },
        )
    }
}

private const val DIM_BRIGHTNESS = 0.04f
