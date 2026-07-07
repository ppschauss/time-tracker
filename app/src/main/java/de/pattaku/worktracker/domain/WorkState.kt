package de.pattaku.worktracker.domain

/** Abgeleiteter Tageszustand — NIE persistiert, immer aus den Events des Tages berechnet (§5). */
enum class WorkState { IDLE, WORKING, ON_BREAK, DONE }
