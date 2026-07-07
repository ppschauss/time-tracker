package de.pattaku.worktracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.pattaku.worktracker.App

/** Plant nach einem Reboot beide Alarme neu (§9, §14.9). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                App.container(context).alarmScheduler.rescheduleAll()
            }
        }
    }
}
