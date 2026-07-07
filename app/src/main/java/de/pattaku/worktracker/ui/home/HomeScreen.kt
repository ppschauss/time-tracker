package de.pattaku.worktracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pattaku.worktracker.App
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.domain.WorkState
import de.pattaku.worktracker.domain.label
import de.pattaku.worktracker.domain.toHMM
import de.pattaku.worktracker.ui.util.formatClock

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val container = App.container(androidx.compose.ui.platform.LocalContext.current)
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    var confirmReset by remember { mutableStateOf(false) }
    var manualOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.summary.state.label(),
            style = MaterialTheme.typography.headlineMedium,
        )

        StatCard(title = "Gearbeitet", value = state.summary.worked.toHMM())
        StatCard(title = "Pause", value = state.summary.pause.toHMM())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                title = "Erste Buchung",
                value = state.summary.firstIn?.let { formatClock(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "Letzte Buchung",
                value = state.summary.lastOut?.let { formatClock(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = {
                if (state.summary.state == WorkState.DONE) confirmReset = true else vm.punch()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(text = state.buttonLabel, style = MaterialTheme.typography.titleLarge)
        }

        TextButton(onClick = { manualOpen = true }) {
            Text("Manuell buchen")
        }
        DropdownMenu(expanded = manualOpen, onDismissRequest = { manualOpen = false }) {
            PunchKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.name) },
                    onClick = {
                        vm.manual(kind)
                        manualOpen = false
                    },
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Tag zurücksetzen?") },
            text = { Text("Es wird ein neues Einstempeln für heute angelegt.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    confirmReset = false
                }) { Text("Neu starten") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Start,
            )
        }
    }
}
