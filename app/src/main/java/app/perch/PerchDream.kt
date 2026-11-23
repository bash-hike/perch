package app.perch

import android.service.dreams.DreamService
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.perch.ui.PerchRoot
import app.perch.ui.PerchTheme

/**
 * Perch as the system screen saver (Settings > Display > Screen saver).
 *
 * This is how the display comes up on its own, with no app to launch: Android starts the screen
 * saver when the tablet is idle on the charger (or dock), including over the lock screen. It shows
 * the same player and clock as the app, and stays up for as long as the tablet is charging.
 */
class PerchDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val app get() = application as PerchApp
    private val touchedAt = mutableLongStateOf(SystemClock.elapsedRealtime())
    private var hasAccess by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true   // touches reach the controls instead of ending the dream
        isFullscreen = true
        isScreenBright = true

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            PerchTheme {
                PerchRoot(
                    app = app,
                    window = window,
                    touchedAt = touchedAt,
                    hasAccess = hasAccess,
                    onAllowAccess = { AccessSettings.open(this); finish() },
                    onExit = ::finish,
                )
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        app.uiShown(true)
        hasAccess = app.hasAccess()
        if (hasAccess) app.watcher.start()
        registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        app.uiShown(false)
        registry.currentState = Lifecycle.State.STARTED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }

    /** Any touch brightens the clock and restarts its timer, as in the app. */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) touchedAt.longValue = SystemClock.elapsedRealtime()
        return super.dispatchTouchEvent(ev)
    }
}
