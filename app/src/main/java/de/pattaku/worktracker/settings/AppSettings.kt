package de.pattaku.worktracker.settings

import java.time.LocalTime

/**
 * Nutzer-Einstellungen (§7). Zeiten als [LocalTime]; persistiert werden sie als
 * Minuten-seit-Mitternacht (siehe SettingsRepository).
 */
data class AppSettings(
    val morningStart: LocalTime = LocalTime.of(7, 0),
    val morningEnd: LocalTime = LocalTime.of(9, 0),
    val eveningStart: LocalTime = LocalTime.of(16, 30),
    val eveningEnd: LocalTime = LocalTime.of(20, 0),
    val autoCloseTime: LocalTime = LocalTime.of(20, 15),
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val autoCloseEnabled: Boolean = true,
    val reminderEnabled: Boolean = true,
)
