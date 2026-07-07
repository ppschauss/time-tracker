package de.pattaku.worktracker.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** BroadcastReceiver, der das [PunchWidget] mit dem AppWidget-Host verbindet. */
class PunchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PunchWidget()
}
