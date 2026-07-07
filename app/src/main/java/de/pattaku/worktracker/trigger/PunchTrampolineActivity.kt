package de.pattaku.worktracker.trigger

import android.app.Activity
import android.os.Bundle

/**
 * Unsichtbare Activity (translucent, kein Layout) für den App-Shortcut. Löst genau einen
 * Stempel-Vorgang aus und beendet sich sofort — der Nutzer sieht die App nie (§8).
 */
class PunchTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Puncher.fire(this) { finish() }
    }
}
