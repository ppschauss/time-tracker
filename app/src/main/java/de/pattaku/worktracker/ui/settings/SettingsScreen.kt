package de.pattaku.worktracker.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.worktracker.App
import java.time.LocalTime

@Composable
fun SettingsScreen(
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = App.container(context)
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val s by vm.state.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Pair<TimeField, LocalTime>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!vm.canScheduleExact()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Exakte Alarme sind nicht erlaubt",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text("Auto-Close und Reminder können sich verspäten.")
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                        )
                    }) { Text("Berechtigung erteilen") }
                }
            }
        }

        TimeRow("Morgen-Start", s.morningStart) { editing = TimeField.MORNING_START to s.morningStart }
        TimeRow("Morgen-Ende", s.morningEnd) { editing = TimeField.MORNING_END to s.morningEnd }
        TimeRow("Abend-Start", s.eveningStart) { editing = TimeField.EVENING_START to s.eveningStart }
        TimeRow("Abend-Ende", s.eveningEnd) { editing = TimeField.EVENING_END to s.eveningEnd }
        TimeRow("Auto-Close", s.autoCloseTime) { editing = TimeField.AUTO_CLOSE to s.autoCloseTime }
        TimeRow("Reminder", s.reminderTime) { editing = TimeField.REMINDER to s.reminderTime }

        SwitchRow("Auto-Close aktiv", s.autoCloseEnabled) { vm.setAutoCloseEnabled(it) }
        SwitchRow("Reminder aktiv", s.reminderEnabled) { vm.setReminderEnabled(it) }

        Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
            Text("CSV exportieren")
        }
    }

    editing?.let { (field, time) ->
        TimePickerDialog(
            initial = time,
            onDismiss = { editing = null },
            onConfirm = { vm.setTime(field, it); editing = null },
        )
    }
}

@Composable
private fun TimeRow(label: String, value: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("%02d:%02d".format(value.hour, value.minute), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeit wählen") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("OK")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
