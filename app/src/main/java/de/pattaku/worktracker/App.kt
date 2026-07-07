package de.pattaku.worktracker

import android.app.Application
import android.content.Context
import de.pattaku.worktracker.alarm.Notifications
import de.pattaku.worktracker.di.AppContainer
import de.pattaku.worktracker.trigger.ShortcutSetup

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.wire()
        Notifications.ensureChannel(this)
        ShortcutSetup.ensure(this)
        // Alarme initial planen (idempotent, self-reschedule via Receiver).
        container.alarmScheduler.rescheduleAll()
    }

    companion object {
        fun container(context: Context): AppContainer =
            (context.applicationContext as App).container
    }
}
