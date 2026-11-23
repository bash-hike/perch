package app.perch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.perch.Settings

/** What the settings sheet shows about permissions, and what its buttons do. Null where there is no sheet (the screen saver). */
class Setup(
    val deviceName: String,
    val notificationsAllowed: Boolean,
    val fullScreenAllowed: Boolean,
    val onAllowNotifications: () -> Unit,
    val onAllowFullScreen: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    hasAccess: Boolean,
    setup: Setup,
    onChange: (Settings.() -> Settings) -> Unit,
    onAllowAccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 28.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Perch", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Plays through the Spotify app on this tablet. In Spotify on your phone, pick this tablet (${setup.deviceName}) from the device list and Perch shows what is playing.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Section("Clock on battery")
            Text("Plugged in, the clock stays on. On battery it stays on for:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Chips(listOf(1 to "1 min", 5 to "5 min", 15 to "15 min", 30 to "30 min", 60 to "1 hour"), settings.batteryClockMin) { v -> onChange { copy(batteryClockMin = v) } }

            Section("After a pause")
            Text("Switch from the player to the clock after:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Chips(listOf(30 to "30 s", 60 to "1 min", 120 to "2 min", 300 to "5 min"), settings.idleDelaySec) { v -> onChange { copy(idleDelaySec = v) } }

            Section("When music starts")
            SwitchRow(
                "Open Perch automatically",
                "Shows Perch over the lock screen when a song starts on a sleeping or locked tablet, and when you wake a locked tablet while music plays. Never while you are using it.",
                settings.openOnPlay,
            ) { v -> onChange { copy(openOnPlay = v) } }
            if (settings.openOnPlay && !setup.notificationsAllowed) {
                Needs("Notifications are off for Perch, so it cannot open itself yet.", setup.onAllowNotifications)
            } else if (settings.openOnPlay && !setup.fullScreenAllowed) {
                Needs("Full screen notifications are off for Perch, so it cannot open itself yet.", setup.onAllowFullScreen)
            }

            Section("Media access")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hasAccess) "On: Perch can see what is playing." else "Off: Perch cannot see what is playing.",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                )
                if (!hasAccess) Button(onClick = onAllowAccess) { Text("Allow") }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Needs(message: String, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Button(onClick = onAllow) { Text("Allow") }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onPick(value) }, label = { Text(label) })
        }
    }
}
