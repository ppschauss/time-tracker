package de.pattaku.worktracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.pattaku.worktracker.App
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.domain.ZONE
import de.pattaku.worktracker.domain.dayBounds
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime

/**
 * Erinnert morgens, falls noch nicht eingestempelt. Genau eine Notification/Tag (feste ID), still
 * wenn heute bereits ein CLOCK_IN existiert (§9, §13 Reminder-Spam). goAsync() + Coroutine.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = App.container(context)
        container.appScope.launch {
            try {
                val now = Instant.now()
                val (from, to) = dayBounds(now)
                val events = container.punchRepository.between(from, to)
                val cfg = container.settingsRepository.current()

                val clockedInToday = events.any { it.kind == PunchKind.CLOCK_IN }
                val nowLocal: LocalTime = now.atZone(ZONE).toLocalTime()
                if (!clockedInToday && !nowLocal.isBefore(cfg.morningEnd)) {
                    Notifications.showReminder(context)
                }
            } finally {
                container.alarmScheduler.scheduleReminder(container.settingsRepository.current())
                pending.finish()
            }
        }
    }
}
