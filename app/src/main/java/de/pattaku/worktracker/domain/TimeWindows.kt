package de.pattaku.worktracker.domain

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
