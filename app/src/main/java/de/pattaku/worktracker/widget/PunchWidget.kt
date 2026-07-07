package de.pattaku.worktracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.action.Action
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.pattaku.worktracker.domain.DaySummary
import de.pattaku.worktracker.domain.WorkState
import de.pattaku.worktracker.domain.label
import de.pattaku.worktracker.domain.toHMM
import de.pattaku.worktracker.trigger.Puncher

/**
 * Home-Screen-Widget (Glance). Zeigt Live-Status und stempelt per Button. Bei DONE erscheint ein
 * Reset-Button (force=true). Jedes Update liest frisch aus der DB — kein gecachter State (§8, §13).
 */
class PunchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Frisch aus der DB — nichts cachen.
        val status = Puncher.currentStatus(context)
        provideContent {
            GlanceTheme {
                Content(status)
            }
        }
    }

    @Composable
    private fun Content(status: DaySummary) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = status.state.label(),
                style = TextStyle(fontWeight = FontWeight.Bold),
            )
            Text(text = "Arbeit ${status.worked.toHMM()} · Pause ${status.pause.toHMM()}")
            Spacer(GlanceModifier.height(8.dp))

            if (status.state == WorkState.DONE) {
                WidgetButton(text = "Neu starten", onClick = PunchAction.reset())
            } else {
                WidgetButton(text = "Stempeln", onClick = PunchAction.punch())
            }
        }
    }

    @Composable
    private fun WidgetButton(text: String, onClick: Action) {
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(Color(0xFF3DDC97)))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = TextStyle(color = ColorProvider(Color.Black), fontWeight = FontWeight.Bold),
            )
        }
    }
}
