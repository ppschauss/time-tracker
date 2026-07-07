package de.pattaku.worktracker.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persistiert [AppSettings] via DataStore. Zeiten werden als Minuten-seit-Mitternacht (Int)
 * gespeichert (§7). Nach Änderung alarm-relevanter Felder wird [rescheduleAll] getriggert.
 */
class SettingsRepository(private val context: Context) {

    /** Von AppContainer nach Konstruktion gesetzt (vermeidet Zyklus mit AlarmScheduler). */
    var rescheduleAll: () -> Unit = {}

    private object Keys {
        val morningStart = intPreferencesKey("morningStart")
        val morningEnd = intPreferencesKey("morningEnd")
        val eveningStart = intPreferencesKey("eveningStart")
        val eveningEnd = intPreferencesKey("eveningEnd")
        val autoCloseTime = intPreferencesKey("autoCloseTime")
        val reminderTime = intPreferencesKey("reminderTime")
        val autoCloseEnabled = booleanPreferencesKey("autoCloseEnabled")
        val reminderEnabled = booleanPreferencesKey("reminderEnabled")
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            morningStart = min(this[Keys.morningStart]) ?: d.morningStart,
            morningEnd = min(this[Keys.morningEnd]) ?: d.morningEnd,
            eveningStart = min(this[Keys.eveningStart]) ?: d.eveningStart,
            eveningEnd = min(this[Keys.eveningEnd]) ?: d.eveningEnd,
            autoCloseTime = min(this[Keys.autoCloseTime]) ?: d.autoCloseTime,
            reminderTime = min(this[Keys.reminderTime]) ?: d.reminderTime,
            autoCloseEnabled = this[Keys.autoCloseEnabled] ?: d.autoCloseEnabled,
            reminderEnabled = this[Keys.reminderEnabled] ?: d.reminderEnabled,
        )
    }

    fun flow(): Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow().first()

    // --- Setter je Feld ---

    suspend fun setMorningStart(t: LocalTime) = putTime(Keys.morningStart, t, reschedule = false)
    suspend fun setMorningEnd(t: LocalTime) = putTime(Keys.morningEnd, t, reschedule = false)
    suspend fun setEveningStart(t: LocalTime) = putTime(Keys.eveningStart, t, reschedule = false)
    suspend fun setEveningEnd(t: LocalTime) = putTime(Keys.eveningEnd, t, reschedule = false)
    suspend fun setAutoCloseTime(t: LocalTime) = putTime(Keys.autoCloseTime, t, reschedule = true)
    suspend fun setReminderTime(t: LocalTime) = putTime(Keys.reminderTime, t, reschedule = true)

    suspend fun setAutoCloseEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.autoCloseEnabled] = enabled }
        rescheduleAll()
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.reminderEnabled] = enabled }
        rescheduleAll()
    }

    private suspend fun putTime(key: Preferences.Key<Int>, t: LocalTime, reschedule: Boolean) {
        context.dataStore.edit { it[key] = t.toMinutes() }
        if (reschedule) rescheduleAll()
    }

    private companion object {
        fun LocalTime.toMinutes(): Int = hour * 60 + minute
        fun min(value: Int?): LocalTime? = value?.let { LocalTime.of(it / 60, it % 60) }
    }
}
