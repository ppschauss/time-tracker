package de.pattaku.worktracker.domain

import de.pattaku.worktracker.data.model.Punch
import de.pattaku.worktracker.data.model.PunchKind
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Alle Zeit-Auswertung geschieht in dieser Zone. Persistiert wird ausschließlich [Instant] (UTC),
 * die Zone dient nur der Ableitung von Tagesgrenzen und Fenstern (§5, §13 DST-safe).
 */
val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

/**
 * Lokaler Tagesanfang (inklusiv) und -ende (exklusiv, = Anfang des Folgetages) als [Instant].
 * DST-safe, weil über [ZoneId] statt fixen 24h-Offset gerechnet wird.
 */
fun dayBounds(now: Instant, zone: ZoneId = ZONE): Pair<Instant, Instant> {
    val today = now.atZone(zone).toLocalDate()
    val start = today.atStartOfDay(zone).toInstant()
    val end = today.plusDays(1).atStartOfDay(zone).toInstant()
    return start to end
}

/**
 * True, wenn die lokale Uhrzeit von [now] innerhalb [[from], [to]) liegt.
 * Über-Mitternacht-Fenster (from > to) werden unterstützt.
 */
fun inWindow(now: Instant, from: LocalTime, to: LocalTime, zone: ZoneId = ZONE): Boolean {
    val t = now.atZone(zone).toLocalTime()
    return if (from <= to) {
        !t.isBefore(from) && t.isBefore(to)
    } else {
        !t.isBefore(from) || t.isBefore(to)
    }
}

/**
 * Zustand allein aus dem letzten Event des Tages: kein Event -> IDLE; CLOCK_IN/BREAK_END -> WORKING;
 * BREAK_START -> ON_BREAK; CLOCK_OUT -> DONE (§5). [dayEvents] muss aufsteigend (ts,id) sortiert sein.
 */
fun stateFrom(dayEvents: List<Punch>): WorkState =
    when (dayEvents.lastOrNull()?.kind) {
        null -> WorkState.IDLE
        PunchKind.CLOCK_IN, PunchKind.BREAK_END -> WorkState.WORKING
        PunchKind.BREAK_START -> WorkState.ON_BREAK
        PunchKind.CLOCK_OUT -> WorkState.DONE
    }

/**
 * Faltet die Events zu Arbeits-/Pausendauer. Offene Intervalle (laufende Arbeit/Pause) werden bis
 * [now] gekappt — dadurch live-fähig und nie negativ. [events] muss (ts,id)-aufsteigend sein.
 *
 * - CLOCK_IN öffnet ein Arbeitsintervall (setzt firstIn, falls noch nicht gesetzt).
 * - BREAK_START schließt Arbeit, öffnet Pause.
 * - BREAK_END schließt Pause, öffnet Arbeit.
 * - CLOCK_OUT schließt Arbeit (setzt lastOut).
 */
fun summarize(events: List<Punch>, now: Instant): DaySummary {
    var worked = Duration.ZERO
    var pause = Duration.ZERO
    var openWork: Instant? = null
    var openBreak: Instant? = null
    var firstIn: Instant? = null
    var lastOut: Instant? = null

    for (e in events) {
        when (e.kind) {
            PunchKind.CLOCK_IN -> {
                if (firstIn == null) firstIn = e.ts
                // Robust gegen doppelte CLOCK_IN: nur öffnen, wenn nicht schon offen.
                if (openWork == null) openWork = e.ts
            }
            PunchKind.BREAK_START -> {
                openWork?.let { worked += Duration.between(it, e.ts) }
                openWork = null
                if (openBreak == null) openBreak = e.ts
            }
            PunchKind.BREAK_END -> {
                openBreak?.let { pause += Duration.between(it, e.ts) }
                openBreak = null
                if (openWork == null) openWork = e.ts
            }
            PunchKind.CLOCK_OUT -> {
                openWork?.let { worked += Duration.between(it, e.ts) }
                openWork = null
                // Falls Pause noch offen war (z.B. defensive), schließen.
                openBreak?.let { pause += Duration.between(it, e.ts) }
                openBreak = null
                lastOut = e.ts
            }
        }
    }

    // Offene Intervalle bis now kappen (nie negativ).
    openWork?.let { if (now.isAfter(it)) worked += Duration.between(it, now) }
    openBreak?.let { if (now.isAfter(it)) pause += Duration.between(it, now) }

    return DaySummary(
        state = stateFrom(events),
        worked = worked,
        pause = pause,
        firstIn = firstIn,
        lastOut = lastOut,
    )
}
