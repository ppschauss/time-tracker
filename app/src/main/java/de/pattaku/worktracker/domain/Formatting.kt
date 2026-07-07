package de.pattaku.worktracker.domain

import java.time.Duration

/** Dauer als H:MM (z.B. 4:05). Negatives wird zu 0:00 geklemmt. */
fun Duration.toHMM(): String {
    val total = if (isNegative) 0 else toMinutes()
    val h = total / 60
    val m = total % 60
    return "%d:%02d".format(h, m)
}

/** Deutscher Zustands-Text für Status-Anzeige. */
fun WorkState.label(): String = when (this) {
    WorkState.IDLE -> "Bereit"
    WorkState.WORKING -> "Arbeitet"
    WorkState.ON_BREAK -> "Pause"
    WorkState.DONE -> "Fertig"
}

/**
 * Label des Haupt-Buttons (§10): Einstempeln / Pause / Weiter / Ausstempeln / Fertig.
 * Im Abendfenster wird aus "Pause" bzw. "Weiter" ein "Ausstempeln", weil punch() dort ausstempelt.
 */
fun buttonLabel(state: WorkState, evening: Boolean): String = when (state) {
    WorkState.IDLE -> "Einstempeln"
    WorkState.WORKING -> if (evening) "Ausstempeln" else "Pause"
    WorkState.ON_BREAK -> if (evening) "Ausstempeln" else "Weiter"
    WorkState.DONE -> "Fertig"
}
