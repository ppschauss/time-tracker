package de.pattaku.worktracker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.pattaku.worktracker.domain.ZONE
import de.pattaku.worktracker.settings.AppSettings
import de.pattaku.worktracker.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Plant die zwei exakten Alarme (Auto-Close, Reminder) auf ihr jeweils nächstes Vorkommen (§9).
 * Nutzt setExactAndAllowWhileIdle; ohne Exact-Alarm-Recht (12+) fällt es auf best-effort inexakt
 * zurück (die UI zeigt dann die Hinweis-Card zum Nachfordern).
 */
class AlarmScheduler(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val am = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

    /** Beide Alarme neu planen (nach Settings-Change und beim Boot). */
    fun rescheduleAll() {
        scope.launch {
            val cfg = settings.current()
            scheduleAutoClose(cfg)
            scheduleReminder(cfg)
        }
    }

    fun scheduleAutoClose(cfg: AppSettings) {
        val pi = pendingIntent(AutoCloseReceiver::class.java, REQ_AUTO_CLOSE)
        if (!cfg.autoCloseEnabled) {
            am.cancel(pi)
            return
        }
        setExact(nextOccurrence(cfg.autoCloseTime), pi)
    }

    fun scheduleReminder(cfg: AppSettings) {
        val pi = pendingIntent(ReminderReceiver::class.java, REQ_REMINDER)
        if (!cfg.reminderEnabled) {
            am.cancel(pi)
            return
        }
        setExact(nextOccurrence(cfg.reminderTime), pi)
    }

    private fun setExact(at: Instant, pi: PendingIntent) {
        val millis = at.toEpochMilli()
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            // Best effort ohne Exact-Recht — UI fordert die Berechtigung nach.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** Nächstes Vorkommen der lokalen Uhrzeit [time]; heute, falls noch nicht vorbei, sonst morgen. */
    private fun nextOccurrence(time: LocalTime, now: Instant = Instant.now()): Instant {
        val zNow = now.atZone(ZONE)
        var candidate = ZonedDateTime.of(zNow.toLocalDate(), time, ZONE)
        if (!candidate.toInstant().isAfter(now)) {
            candidate = ZonedDateTime.of(zNow.toLocalDate().plusDays(1), time, ZONE)
        }
        return candidate.toInstant()
    }

    private fun pendingIntent(cls: Class<*>, requestCode: Int): PendingIntent {
        val intent = Intent(context, cls)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQ_AUTO_CLOSE = 1001
        const val REQ_REMINDER = 1002
    }
}
