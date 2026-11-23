package app.perch

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.perch.ui.PerchRoot
import app.perch.ui.PerchTheme
import app.perch.ui.Setup

class MainActivity : ComponentActivity() {
    private val app get() = application as PerchApp
    private val nm get() = getSystemService(NotificationManager::class.java)
    private val prefs get() = getSharedPreferences("perch", MODE_PRIVATE)

    private val touchedAt = mutableLongStateOf(SystemClock.elapsedRealtime())
    private var hasAccess by mutableStateOf(false)
    private var notificationsAllowed by mutableStateOf(true)
    private var fullScreenAllowed by mutableStateOf(true)

    // "Open Perch automatically" needs two things: the notification permission, then the user-granted
    // "full screen notifications" switch. Both are asked once, right after media access is on.
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        if (granted) maybeAskFullScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PerchTheme {
                PerchRoot(
                    app = app,
                    window = window,
                    touchedAt = touchedAt,
                    hasAccess = hasAccess,
                    onAllowAccess = { AccessSettings.open(this) },
                    setup = Setup(
                        deviceName = Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL,
                        notificationsAllowed = notificationsAllowed,
                        fullScreenAllowed = fullScreenAllowed,
                        onAllowNotifications = ::allowNotifications,
                        onAllowFullScreen = ::openFullScreenSettings,
                    ),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.uiShown(true)
        // The full-screen notification has done its job once Perch is on the screen.
        nm.cancel(PerchApp.NOTIFICATION_ID)
    }

    override fun onResume() {
        super.onResume()
        // Access and permissions are granted in system screens; re-check every time we come back from one.
        hasAccess = app.hasAccess()
        notificationsAllowed = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        fullScreenAllowed = nm.canUseFullScreenIntent()
        if (hasAccess) {
            app.watcher.start()
            maybeSetUpOpenOnPlay()
        }
        hideBars()
    }

    override fun onStop() {
        app.uiShown(false)
        super.onStop()
    }

    /** Right after media access is on, ask once for what "open automatically" needs. Later the sheet shows what is missing. */
    private fun maybeSetUpOpenOnPlay() {
        if (!app.settings.state.value.openOnPlay) return
        if (!notificationsAllowed) {
            if (!prefs.getBoolean(ASKED_NOTIFICATIONS, false)) {
                prefs.edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        maybeAskFullScreen()
    }

    private fun maybeAskFullScreen() {
        if (nm.canUseFullScreenIntent() || prefs.getBoolean(ASKED_FULL_SCREEN, false)) return
        prefs.edit().putBoolean(ASKED_FULL_SCREEN, true).apply()
        openFullScreenSettings()
    }

    /** The sheet's button: asks if Android still will, otherwise opens Perch's notification settings. */
    private fun allowNotifications() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) || !prefs.getBoolean(ASKED_NOTIFICATIONS, false)) {
            prefs.edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    private fun openFullScreenSettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    /** Any touch wakes the clock from its dim state and restarts the on-battery timer. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) touchedAt.longValue = SystemClock.elapsedRealtime()
        return super.dispatchTouchEvent(ev)
    }

    private fun hideBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        const val ASKED_NOTIFICATIONS = "askedNotifications"
        const val ASKED_FULL_SCREEN = "askedFullScreen"
    }
}
