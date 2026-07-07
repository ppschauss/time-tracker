package de.pattaku.worktracker.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Clock, deren Zeit im Test frei gesetzt werden kann. */
class MutableClock(var current: Instant, private val zone: ZoneId = ZONE) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(z: ZoneId): Clock = MutableClock(current, z)
    override fun instant(): Instant = current
}

/** Hilfsfunktion: lokale Berlin-Uhrzeit an einem festen Datum -> Instant. */
fun berlin(year: Int, month: Int, day: Int, time: LocalTime): Instant =
    ZonedDateTime.of(year, month, day, time.hour, time.minute, 0, 0, ZONE).toInstant()
