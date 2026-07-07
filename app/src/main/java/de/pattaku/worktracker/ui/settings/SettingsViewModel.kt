package de.pattaku.worktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.pattaku.worktracker.alarm.AlarmScheduler
import de.pattaku.worktracker.di.AppContainer
import de.pattaku.worktracker.settings.AppSettings
import de.pattaku.worktracker.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

enum class TimeField { MORNING_START, MORNING_END, EVENING_START, EVENING_END, AUTO_CLOSE, REMINDER }

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val alarms: AlarmScheduler,
) : ViewModel() {

    val state: StateFlow<AppSettings> =
        settings.flow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun canScheduleExact(): Boolean = alarms.canScheduleExact()

    fun setTime(field: TimeField, t: LocalTime) = viewModelScope.launch {
        when (field) {
            TimeField.MORNING_START -> settings.setMorningStart(t)
            TimeField.MORNING_END -> settings.setMorningEnd(t)
            TimeField.EVENING_START -> settings.setEveningStart(t)
            TimeField.EVENING_END -> settings.setEveningEnd(t)
            TimeField.AUTO_CLOSE -> settings.setAutoCloseTime(t)
            TimeField.REMINDER -> settings.setReminderTime(t)
        }
    }

    fun setAutoCloseEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setAutoCloseEnabled(enabled)
    }

    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setReminderEnabled(enabled)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.alarmScheduler) }
        }
    }
}
