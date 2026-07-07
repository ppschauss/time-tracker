package de.pattaku.worktracker.ui.util

import de.pattaku.worktracker.domain.ZONE
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clockFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
private val dateFmt = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMANY)

/** Uhrzeit in Europe/Berlin, z.B. "08:00". */
fun formatClock(ts: Instant): String = ts.atZone(ZONE).format(clockFmt)

/** Datum in Europe/Berlin, z.B. "Mo, 07.07.2026". */
fun formatDate(ts: Instant): String = ts.atZone(ZONE).format(dateFmt)
