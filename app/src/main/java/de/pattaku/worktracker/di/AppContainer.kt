package de.pattaku.worktracker.di

import android.content.Context
import de.pattaku.worktracker.alarm.AlarmScheduler
import de.pattaku.worktracker.data.PunchRepository
import de.pattaku.worktracker.data.db.AppDatabase
import de.pattaku.worktracker.domain.PunchUseCase
import de.pattaku.worktracker.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Manuelle DI (§2, kein Hilt). Ein Container pro Prozess, gehalten von der Application.
 * Alle Singletons sind lazy, damit Prozess-Start (z.B. Receiver-Weckung) billig bleibt.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Langlebiger Scope für Trigger/Receiver-Coroutinen (kein an UI gebundener Lifecycle). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val punchRepository: PunchRepository by lazy { PunchRepository(database.punchDao()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val punchUseCase: PunchUseCase by lazy {
        PunchUseCase(punchRepository, settingsRepository::current, Clock.systemUTC())
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(appContext, settingsRepository, appScope)
    }

    /** Verdrahtet den Reschedule-Hook, sobald beide Seiten existieren (vermeidet Init-Zyklus). */
    fun wire() {
        settingsRepository.rescheduleAll = { alarmScheduler.rescheduleAll() }
    }
}
