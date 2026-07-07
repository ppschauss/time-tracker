package de.pattaku.worktracker.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import de.pattaku.worktracker.R
import de.pattaku.worktracker.trigger.PunchTrampolineActivity

/** Notification-Channel + Reminder-Notification mit fester ID (§9, kein Spam). */
object Notifications {

    const val CHANNEL_REMINDER = "reminder"
    const val ID_REMINDER = 42

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        mgr.createNotificationChannel(channel)
    }

    fun showReminder(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        // Tippen auf die Notification stempelt direkt ein (Trampoline).
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, PunchTrampolineActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        // Feste ID -> höchstens eine Reminder-Notification gleichzeitig.
        mgr.notify(ID_REMINDER, notification)
    }
}
