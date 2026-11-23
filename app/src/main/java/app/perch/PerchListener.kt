package app.perch

import android.service.notification.NotificationListenerService

/**
 * Perch reads no notifications. The service exists because Android only lets an app list other
 * apps' media sessions once it holds notification access; the system binds this class whenever
 * access is on, which also starts the process after a reboot.
 */
class PerchListener : NotificationListenerService() {
    override fun onListenerConnected() {
        (application as PerchApp).watcher.start()
    }

    override fun onListenerDisconnected() {
        // Access was withdrawn or the system rebound us; ask to be bound again.
        runCatching { requestRebind(android.content.ComponentName(this, PerchListener::class.java)) }
    }
}
