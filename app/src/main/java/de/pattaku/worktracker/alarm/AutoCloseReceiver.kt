package de.pattaku.worktracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.pattaku.worktracker.App
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.domain.dayBounds
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Schließt am Abend einen offenen Tag automatisch ab und markiert die Events mit auto=true (§9).
 * DB-Zugriff über goAsync() + Coroutine, damit der Receiver nicht vorzeitig getötet wird (§13).
 */
class AutoCloseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = App.container(context)
        container.appScope.launch {
            try {
                val now = Instant.now()
                val (from, to) = dayBounds(now)
                val events = container.punchRepository.between(from, to)
                val kinds: List<PunchKind> = when (events.lastOrNull()?.kind) {
                    PunchKind.BREAK_START -> listOf(PunchKind.BREAK_END, PunchKind.CLOCK_OUT)
                    PunchKind.CLOCK_IN, PunchKind.BREAK_END -> listOf(PunchKind.CLOCK_OUT)
                    else -> emptyList() // schon zu (CLOCK_OUT) oder leer -> nichts (§14.5)
                }
                for (kind in kinds) {
                    container.punchRepository.insert(Punch(ts = now, kind = kind, auto = true))
                }
            } finally {
                // Self-Reschedule auf das nächste Vorkommen.
                container.alarmScheduler.scheduleAutoClose(container.settingsRepository.current())
                pending.finish()
            }
        }
    }
}
