package de.pattaku.worktracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import de.pattaku.worktracker.App

/**
 * Widget-Button-Callback. Stempelt (optional force=true als Reset) und aktualisiert danach das
 * Widget frisch aus der DB (§8). Läuft in Glances eigenem Coroutine-Kontext.
 */
class PunchAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val force = parameters[FORCE] ?: false
        // Direkt awaiten (statt Fire-and-forget), damit das anschließende update() den neuen Stand sieht.
        App.container(context).punchUseCase.punch(force)
        PunchWidget().update(context, glanceId)
    }

    companion object {
        private val FORCE = ActionParameters.Key<Boolean>("force")

        fun punch(): Action = actionRunCallback<PunchAction>(actionParametersOf(FORCE to false))
        fun reset(): Action = actionRunCallback<PunchAction>(actionParametersOf(FORCE to true))
    }
}
