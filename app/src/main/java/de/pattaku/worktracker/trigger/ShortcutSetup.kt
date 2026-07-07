package de.pattaku.worktracker.trigger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import de.pattaku.worktracker.R

/** Registriert den dynamischen "Stempeln"-Shortcut, der die Trampoline-Activity startet (§8). */
object ShortcutSetup {

    private const val ID = "punch"

    fun ensure(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return

        val intent = Intent(context, PunchTrampolineActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            component = ComponentName(context, PunchTrampolineActivity::class.java)
        }

        val shortcut = ShortcutInfo.Builder(context, ID)
            .setShortLabel(context.getString(R.string.shortcut_punch_short))
            .setLongLabel(context.getString(R.string.shortcut_punch_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setIntent(intent)
            .build()

        // setDynamicShortcuts ist idempotent — ersetzt den bestehenden Eintrag mit gleicher ID.
        manager.dynamicShortcuts = listOf(shortcut)
    }
}
