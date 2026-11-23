package app.perch

import android.annotation.SuppressLint
import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/** Process-wide state: the media watcher, the battery and the settings outlive the Activity. */
class PerchApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var settings: SettingsStore
    lateinit var watcher: MediaWatcher
    lateinit var power: PowerState

    /** How many Perch screens (the app, the screen saver) are on display right now. */
    private val shown = AtomicInteger()
    fun uiShown(on: Boolean) { if (on) shown.incrementAndGet() else shown.updateAndGet { (it - 1).coerceAtLeast(0) } }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        power = PowerState(this)
        watcher = MediaWatcher(this, scope)
        if (hasAccess()) watcher.start()

        // When a song starts while the tablet sleeps or sits on the lock screen, open Perch over it.
        scope.launch {
            watcher.state.map { it.isPlaying }.distinctUntilChanged().drop(1).collect { playing ->
                if (playing) onPlaybackStarted()
            }
        }
        // When someone wakes a locked tablet while music plays, show Perch instead of the bare lock screen.
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = onScreenWoken()
        }, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    /** A song started: if the tablet is asleep or on the lock screen, open Perch over it. */
    private fun onPlaybackStarted() {
        if (!settings.state.value.openOnPlay) return
        val inUse = getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (inUse) return  // someone is using the tablet (in Instagram, say): leave it alone
        openOverLockScreen("song started while asleep or locked")
    }

    /**
     * The screen just came on (power button, a tap, a notification). If music is playing and the tablet is locked,
     * show Perch rather than the plain lock screen. The screen is never woken by this: it only reacts to a wake
     * that already happened. An unlocked tablet is left alone.
     */
    private fun onScreenWoken() {
        if (!settings.state.value.openOnPlay || !watcher.state.value.isPlaying) return
        if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        openOverLockScreen("screen woken while playing")
    }

    /**
     * Ask Android to show Perch with a full-screen-intent notification: the platform starts the activity over
     * the lock screen (and wakes the screen if it is off), which an app cannot do from the background by itself.
     */
    // The manifest holds POST_NOTIFICATIONS, and areNotificationsEnabled() is checked first.
    @SuppressLint("NotificationPermission")
    private fun openOverLockScreen(reason: String) {
        if (shown.get() > 0) return  // Perch is already on the screen
        val now = SystemClock.elapsedRealtime()
        if (now - lastOpenAt < DEBOUNCE_MS) return  // one trigger often follows another (a wake after a post)
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled() || !nm.canUseFullScreenIntent()) {
            Log.i("Perch", "opening needs notifications and full screen notifications allowed")
            return
        }
        lastOpenAt = now
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_show_on_play), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_perch)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_now_playing))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setFullScreenIntent(open, true)
                .setAutoCancel(true)
                .setTimeoutAfter(TIMEOUT_MS)
                .build()
        )
        Log.i("Perch", "$reason: full-screen intent posted")
    }

    private var lastOpenAt = 0L

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL = "show_on_play"
        private const val TIMEOUT_MS = 15_000L
        private const val DEBOUNCE_MS = 4_000L
    }
}
