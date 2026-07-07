package de.pattaku.worktracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.worktracker.App
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.domain.ZONE
import de.pattaku.worktracker.domain.toHMM
import de.pattaku.worktracker.ui.util.formatClock
import de.pattaku.worktracker.ui.util.formatDate
import java.time.LocalTime

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val container = App.container(LocalContext.current)
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Punch?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Diese Woche: ${state.weekWorked.toHMM()} h",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(state.days) { day ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = formatDate(day.events.first().ts),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Arbeit ${day.summary.worked.toHMM()} · Pause ${day.summary.pause.toHMM()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    day.events.forEach { e ->
                        EventRow(e) { editing = e }
                    }
                }
            }
        }
    }

    editing?.let { punch ->
        EditDialog(
            punch = punch,
            onDismiss = { editing = null },
            onSave = { time -> vm.editTime(punch, time); editing = null },
            onDelete = { vm.delete(punch); editing = null },
        )
    }
}

@Composable
private fun EventRow(e: Punch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = formatClock(e.ts), style = MaterialTheme.typography.bodyLarge)
        Text(text = e.kind.name, style = MaterialTheme.typography.bodyLarge)
        if (e.auto) AssistChip(onClick = onClick, label = { Text("auto") })
        TextButton(onClick = onClick) { Text("Bearbeiten") }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditDialog(
    punch: Punch,
    onDismiss: () -> Unit,
    onSave: (LocalTime) -> Unit,
    onDelete: () -> Unit,
) {
    val local = punch.ts.atZone(ZONE).toLocalTime()
    val pickerState = rememberTimePickerState(
        initialHour = local.hour,
        initialMinute = local.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buchung bearbeiten") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onSave(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Löschen") }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}
