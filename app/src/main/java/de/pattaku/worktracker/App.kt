package de.pattaku.worktracker

import android.app.Application
import android.content.Context
import de.pattaku.worktracker.di.AppContainer
import de.pattaku.worktracker.trigger.ShortcutSetup

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ShortcutSetup.ensure(this)
        // Notification-Channel und Alarme werden in Phase 6 ergänzt.
    }

    companion object {
        fun container(context: Context): AppContainer =
            (context.applicationContext as App).container
    }
}
