package de.pattaku.worktracker

import android.app.Application
import android.content.Context
import de.pattaku.worktracker.di.AppContainer

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Notification-Channel, Alarme und Shortcut werden in den jeweiligen Phasen ergänzt.
    }

    companion object {
        fun container(context: Context): AppContainer =
            (context.applicationContext as App).container
    }
}
