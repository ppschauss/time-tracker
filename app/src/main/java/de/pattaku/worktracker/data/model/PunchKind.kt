package de.pattaku.worktracker.data.model

/** Die vier möglichen Stempel-Ereignisse. Reihenfolge = fachliche Reihenfolge eines Tages. */
enum class PunchKind {
    CLOCK_IN,
    BREAK_START,
    BREAK_END,
    CLOCK_OUT,
}
