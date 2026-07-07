package de.pattaku.worktracker.trigger

import android.content.Context
import de.pattaku.worktracker.App
import de.pattaku.worktracker.domain.DaySummary
import de.pattaku.worktracker.domain.PunchResult
import de.pattaku.worktracker.domain.dayBounds
import de.pattaku.worktracker.domain.summarize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Gemeinsamer Einstiegspunkt für Tile/Widget/Shortcut. Führt punch() auf dem langlebigen
 * appScope aus (überlebt kurzlebige Trigger-Kontexte) und ruft [onDone] auf dem Main-Thread.
 */
object Puncher {

    /** Löst einen Stempel-Vorgang aus. [onDone] erhält das Ergebnis (für UI-Refresh/Subtitle). */
    fun fire(context: Context, force: Boolean = false, onDone: (PunchResult) -> Unit = {}) {
        val container = App.container(context)
        container.appScope.launch {
            val result = container.punchUseCase.punch(force)
            withContext(Dispatchers.Main) { onDone(result) }
        }
    }

    /** Aktueller Tages-Status ohne zu stempeln — für Tile/Widget-Anzeige. Immer frisch aus DB. */
    suspend fun currentStatus(context: Context): DaySummary {
        val container = App.container(context)
        val now = Instant.now()
        val (from, to) = dayBounds(now)
        val events = container.punchRepository.between(from, to)
        return summarize(events, now)
    }
}
