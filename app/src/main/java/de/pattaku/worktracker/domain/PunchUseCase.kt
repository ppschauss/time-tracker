package de.pattaku.worktracker.domain

import de.pattaku.worktracker.data.PunchRepository
import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.data.model.PunchKind
import de.pattaku.worktracker.settings.AppSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant

/** Ergebnis eines Tap: entweder nichts (NoOp) oder eine oder mehrere gebuchte Ereignisse. */
sealed interface PunchResult {
    data object NoOp : PunchResult
    data class Recorded(val kinds: List<PunchKind>, val summary: DaySummary) : PunchResult
}

/**
 * Die EINE Wahrheit hinter Tile/Widget/Shortcut/UI. Entscheidet kontextabhängig, was ein Tap
 * bedeutet (§5). Der [Mutex] serialisiert konkurrierende Trigger (Doppel-Tap, §13).
 */
class PunchUseCase(
    private val repo: PunchRepository,
    private val settingsProvider: suspend () -> AppSettings,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val mutex = Mutex()

    suspend fun punch(force: Boolean = false): PunchResult = mutex.withLock {
        val now = Instant.now(clock)
        val (from, to) = dayBounds(now)
        val events = repo.between(from, to)
        val state = stateFrom(events)
        val cfg = settingsProvider()
        val evening = inWindow(now, cfg.eveningStart, cfg.eveningEnd)

        val kinds: List<PunchKind> = when (state) {
            WorkState.IDLE -> listOf(PunchKind.CLOCK_IN)
            WorkState.WORKING ->
                listOf(if (evening) PunchKind.CLOCK_OUT else PunchKind.BREAK_START)
            WorkState.ON_BREAK ->
                if (evening) listOf(PunchKind.BREAK_END, PunchKind.CLOCK_OUT)
                else listOf(PunchKind.BREAK_END)
            WorkState.DONE ->
                if (force) listOf(PunchKind.CLOCK_IN) else emptyList()
        }

        if (kinds.isEmpty()) return@withLock PunchResult.NoOp

        // Sequenzielle Inserts mit identischem ts=now — Reihenfolge wird durch id-Tie-Break gewahrt.
        for (kind in kinds) {
            repo.insert(Punch(ts = now, kind = kind, auto = false))
        }

        val after = repo.between(from, to)
        PunchResult.Recorded(kinds, summarize(after, now))
    }
}
