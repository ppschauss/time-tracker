package de.pattaku.worktracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.pattaku.worktracker.data.PunchRepository
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.di.AppContainer
import de.pattaku.worktracker.domain.DaySummary
import de.pattaku.worktracker.domain.ZONE
import de.pattaku.worktracker.domain.summarize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

/** Ein lokaler Tag mit neu berechneter Zusammenfassung und den zugehörigen Events (aufsteigend). */
data class DayGroup(
    val date: LocalDate,
    val summary: DaySummary,
    val events: List<Punch>,
)

data class HistoryUiState(
    val weekWorked: Duration = Duration.ZERO,
    val days: List<DayGroup> = emptyList(),
)

class HistoryViewModel(private val repo: PunchRepository) : ViewModel() {

    val state: StateFlow<HistoryUiState> =
        repo.observeAll().map { all -> buildState(all) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    private fun buildState(all: List<Punch>): HistoryUiState {
        val now = Instant.now()
        val today = now.atZone(ZONE).toLocalDate()

        // Nach lokalem Tag gruppieren.
        val byDay = all.groupBy { it.ts.atZone(ZONE).toLocalDate() }

        val days = byDay.entries
            .sortedByDescending { it.key }
            .map { (date, events) ->
                val ascending = events.sortedWith(compareBy({ it.ts }, { it.id }))
                // Offene Intervalle kappen: heute bis now, sonst bis Tagesende.
                val capAt = if (date == today) now
                else date.plusDays(1).atStartOfDay(ZONE).toInstant()
                DayGroup(date, summarize(ascending, capAt), ascending)
            }

        // Wochensumme (ISO-Woche der aktuellen Zeit).
        val wf = WeekFields.ISO
        val thisWeek = today.get(wf.weekOfWeekBasedYear())
        val thisYear = today.get(wf.weekBasedYear())
        val weekWorked = days
            .filter {
                it.date.get(wf.weekOfWeekBasedYear()) == thisWeek &&
                    it.date.get(wf.weekBasedYear()) == thisYear
            }
            .fold(Duration.ZERO) { acc, d -> acc + d.summary.worked }

        return HistoryUiState(weekWorked, days)
    }

    /** Verschiebt einen Event auf eine neue lokale Uhrzeit am selben Tag. */
    fun editTime(punch: Punch, newTime: LocalTime) = viewModelScope.launch {
        val date = punch.ts.atZone(ZONE).toLocalDate()
        val newTs = date.atTime(newTime).atZone(ZONE).toInstant()
        repo.update(punch.copy(ts = newTs))
    }

    fun delete(punch: Punch) = viewModelScope.launch { repo.delete(punch) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container.punchRepository) }
        }
    }
}
