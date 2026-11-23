package app.perch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Opens the system screen where the user grants Perch notification access (media session access). */
object AccessSettings {
    fun open(context: Context) {
        val me = ComponentName(context, PerchListener::class.java).flattenToString()
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, me)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(detail) }.onFailure {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
