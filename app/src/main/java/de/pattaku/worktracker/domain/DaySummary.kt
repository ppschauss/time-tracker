package de.pattaku.worktracker.domain

import java.time.Duration
import java.time.Instant

/**
 * Ergebnis von [summarize] für einen lokalen Tag. Offene Intervalle sind bis `now` gekappt,
 * daher sind [worked]/[pause] live-fähig und nie negativ.
 */
data class DaySummary(
    val state: WorkState,
    val worked: Duration,
    val pause: Duration,
    val firstIn: Instant?,
    val lastOut: Instant?,
)
