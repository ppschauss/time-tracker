package de.pattaku.worktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.pattaku.worktracker.data.PunchRepository
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.di.AppContainer
import de.pattaku.worktracker.domain.DaySummary
import de.pattaku.worktracker.domain.PunchUseCase
import de.pattaku.worktracker.domain.WorkState
import de.pattaku.worktracker.domain.buttonLabel
import de.pattaku.worktracker.domain.inWindow
import de.pattaku.worktracker.domain.summarize
import de.pattaku.worktracker.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class HomeUiState(
    val summary: DaySummary = DaySummary(WorkState.IDLE, Duration.ZERO, Duration.ZERO, null, null),
    val evening: Boolean = false,
    val buttonLabel: String = "Einstempeln",
)

class HomeViewModel(
    private val repo: PunchRepository,
    private val settings: SettingsRepository,
    private val punchUseCase: PunchUseCase,
) : ViewModel() {

    // Sekündlicher Tick, damit offene Intervalle live weiterlaufen (§10).
    private val tick = flow {
        while (true) {
            emit(Instant.now())
            delay(1000)
        }
    }

    val state: StateFlow<HomeUiState> =
        combine(repo.observeToday(Instant.now()), settings.flow(), tick) { events, cfg, now ->
            val summary = summarize(events, now)
            val evening = inWindow(now, cfg.eveningStart, cfg.eveningEnd)
            HomeUiState(summary, evening, buttonLabel(summary.state, evening))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun punch() = viewModelScope.launch { punchUseCase.punch(force = false) }

    fun reset() = viewModelScope.launch { punchUseCase.punch(force = true) }

    /** Fallback "Manuell buchen": fügt genau das gewählte Kind mit now ein. */
    fun manual(kind: PunchKind) = viewModelScope.launch {
        repo.insert(Punch(ts = Instant.now(), kind = kind, auto = false))
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.punchRepository,
                    container.settingsRepository,
                    container.punchUseCase,
                )
            }
        }
    }
}
